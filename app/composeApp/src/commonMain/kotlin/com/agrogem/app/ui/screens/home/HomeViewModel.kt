package com.agrogem.app.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.analysis.domain.AnalysisRepository
import com.agrogem.app.data.analysis.domain.StoredAnalysis
import com.agrogem.app.data.location.DeviceLocationProvider
import com.agrogem.app.data.location.createDeviceLocationProvider
import com.agrogem.app.data.session.SessionSnapshot
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.soil.domain.SoilSummary
import com.agrogem.app.data.weather.domain.CurrentWeather
import com.agrogem.app.data.weather.domain.WeatherRepository
import com.agrogem.app.ui.components.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val LOCATION_RESOLUTION_TIMEOUT_MS = 10_000L

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object LocationMissing : HomeUiState
    data class Error(val message: String, val retryable: Boolean) : HomeUiState
    data class Data(
        val locationInfo: ResolvedLocation,
        val weather: CurrentWeather,
        val metrics: WeatherMetrics,
        val soilSummary: SoilSummary?,
        val profileGreeting: String?,
        val cropContext: String?,
        val recentAnalyses: List<HomeRecentAnalysis>,
    ) : HomeUiState
}

@Immutable
data class HomeRecentAnalysis(
    val name: String,
    val subtitle: String,
    val health: String,
    val severity: Severity,
)

@Immutable
data class WeatherMetrics(
    val humidity: String,
    val windSpeed: String,
    val precipitation: String,
    val maxMin: String,
    val uvIndex: String,
)

class HomeViewModel(
    private val geolocationRepository: GeolocationRepository,
    private val weatherRepository: WeatherRepository,
    private val soilRepository: SoilRepository,
    private val sessionLocalStore: SessionLocalStore,
    private val deviceLocationProvider: DeviceLocationProvider? = null,
    private val analysisRepository: AnalysisRepository = EmptyAnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _isResolvingLocation = MutableStateFlow(false)
    val isResolvingLocation: StateFlow<Boolean> = _isResolvingLocation.asStateFlow()
    private var autoResolveAttempted = false

    init {
        observeLocationAndLoad()
    }

    fun refresh() {
        viewModelScope.launch {
            val location = geolocationRepository.observeResolvedLocation().first()
            if (location == null) {
                _uiState.value = HomeUiState.LocationMissing
                return@launch
            }
            loadFor(location)
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.value = HomeUiState.LocationMissing
            return
        }
        viewModelScope.launch {
            _isResolvingLocation.value = true
            _uiState.value = HomeUiState.Loading
            val locationProvider = deviceLocationProvider ?: createDeviceLocationProvider()
            runCatching {
                withTimeout(LOCATION_RESOLUTION_TIMEOUT_MS) {
                    locationProvider.getCurrentLatLng()
                }
            }.fold(
                onSuccess = { locationResult ->
                    locationResult.fold(
                        onSuccess = { latLng ->
                            geolocationRepository.reverseGeocode(latLng).onFailure { error ->
                                _uiState.value = HomeUiState.Error(
                                    message = error.message ?: "No se pudo resolver tu ubicación",
                                    retryable = true,
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.value = HomeUiState.Error(
                                message = error.message ?: "No se pudo obtener tu ubicación",
                                retryable = true,
                            )
                        },
                    )
                },
                onFailure = {
                    _uiState.value = HomeUiState.Error(
                        message = "La ubicación tardó demasiado. Reintentá con el GPS activo.",
                        retryable = true,
                    )
                },
            )
            _isResolvingLocation.value = false
        }
    }

    private fun observeLocationAndLoad() {
        viewModelScope.launch {
            geolocationRepository.observeResolvedLocation().collect { location ->
                if (location == null) {
                    tryResolveLocationSilently()
                    _uiState.value = HomeUiState.LocationMissing
                    return@collect
                }
                loadFor(location)
            }
        }
    }

    private suspend fun loadFor(location: ResolvedLocation) {
        val cachedWeather = weatherRepository.getCachedWeather(location.coordinates)
        if (cachedWeather != null) {
            pushDataState(location = location, weather = cachedWeather, soilSummary = null)
            loadSoilInBackground(location, cachedWeather)
        } else {
            _uiState.value = HomeUiState.Loading
        }

        val shouldRefresh = weatherRepository.shouldRefresh(location.coordinates)
        if (!shouldRefresh && cachedWeather != null) {
            return
        }

        val weatherResult = weatherRepository.getCurrentWeather(location.coordinates)
        weatherResult.fold(
            onSuccess = { weather ->
                pushDataState(location = location, weather = weather, soilSummary = null)
                loadSoilInBackground(location, weather)
            },
            onFailure = { error ->
                if (cachedWeather == null) {
                    _uiState.value = HomeUiState.Error(
                        message = error.message ?: "Error desconocido",
                        retryable = true,
                    )
                }
            },
        )
    }

    private suspend fun tryResolveLocationSilently() {
        if (autoResolveAttempted) return
        autoResolveAttempted = true
        val locationProvider = deviceLocationProvider ?: runCatching { createDeviceLocationProvider() }.getOrNull() ?: return
        runCatching {
            withTimeout(LOCATION_RESOLUTION_TIMEOUT_MS) {
                locationProvider.getCurrentLatLng()
            }
        }.getOrNull()?.onSuccess { latLng ->
            geolocationRepository.reverseGeocode(latLng)
        }
    }

    private fun loadSoilInBackground(location: ResolvedLocation, weather: CurrentWeather) {
        viewModelScope.launch {
            val soilSummary = soilRepository.getSoil(location.coordinates).getOrNull()?.summary
            val current = _uiState.value
            if (current is HomeUiState.Data && current.locationInfo == location && current.weather == weather) {
                pushDataState(location, weather, soilSummary)
            }
        }
    }

    private suspend fun pushDataState(location: ResolvedLocation, weather: CurrentWeather, soilSummary: SoilSummary?) {
        val profile = sessionLocalStore.read()
        val recentAnalyses = analysisRepository.listRecent(limit = 3).map(::toRecentAnalysis)
        _uiState.value = HomeUiState.Data(
            locationInfo = location,
            weather = weather,
            metrics = WeatherMetrics(
                humidity = weather.humidity,
                windSpeed = weather.windSpeed,
                precipitation = weather.precipitation,
                maxMin = weather.maxMin,
                uvIndex = weather.uvIndex,
            ),
            soilSummary = soilSummary,
            profileGreeting = buildProfileGreeting(profile),
            cropContext = buildCropContext(profile),
            recentAnalyses = recentAnalyses,
        )
    }

    private fun toRecentAnalysis(analysis: StoredAnalysis): HomeRecentAnalysis {
        val diagnosis = analysis.diagnosis
        return HomeRecentAnalysis(
            name = diagnosis.pestName.uppercase(),
            subtitle = diagnosis.diagnosisText,
            health = "Salud: ${(diagnosis.confidence * 100).toInt()}%",
            severity = diagnosis.severity.toSeverity(),
        )
    }

    private fun buildProfileGreeting(snapshot: SessionSnapshot): String? {
        val crops = snapshot.crops?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val name = snapshot.name?.trim().takeUnless { it.isNullOrEmpty() }
        val area = snapshot.area?.trim().takeUnless { it.isNullOrEmpty() }
        val stage = snapshot.stage?.trim().takeUnless { it.isNullOrEmpty() }
        return buildString {
            append("Hola")
            if (name != null) append(", $name")
            append(". Hoy te acompaño con $crops")
            if (stage != null) append(" en etapa $stage")
            if (area != null) append(" en $area")
            append('.')
        }
    }

    private fun buildCropContext(snapshot: SessionSnapshot): String? {
        val crops = snapshot.crops?.trim().takeUnless { it.isNullOrEmpty() }
        val stage = snapshot.stage?.trim().takeUnless { it.isNullOrEmpty() }
        if (crops == null && stage == null) return null
        return listOfNotNull(crops, stage).joinToString(" · ")
    }
}

private object EmptyAnalysisRepository : AnalysisRepository {
    override suspend fun save(analysis: StoredAnalysis) = Unit
    override suspend fun getById(analysisId: String): StoredAnalysis? = null
    override suspend fun listRecent(limit: Long): List<StoredAnalysis> = emptyList()
}

private fun String.toSeverity(): Severity {
    val value = lowercase()
    return when {
        value.contains("cr") || value.contains("alta") -> Severity.Critica
        value.contains("aten") || value.contains("media") || value.contains("moder") -> Severity.Atencion
        else -> Severity.Optimo
    }
}
