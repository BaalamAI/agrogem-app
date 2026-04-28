package com.agrogem.app.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.session.SessionSnapshot
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.soil.domain.SoilSummary
import com.agrogem.app.data.weather.domain.CurrentWeather
import com.agrogem.app.data.weather.domain.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    ) : HomeUiState
}

@Immutable
data class WeatherMetrics(
    val humidity: String,
    val cloudCover: String,
    val uvIndex: String,
)

class HomeViewModel(
    private val geolocationRepository: GeolocationRepository,
    private val weatherRepository: WeatherRepository,
    private val soilRepository: SoilRepository,
    private val sessionLocalStore: SessionLocalStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val location = geolocationRepository.observeResolvedLocation().first()
            if (location == null) {
                _uiState.value = HomeUiState.LocationMissing
                return@launch
            }
            val weatherResult = weatherRepository.getCurrentWeather(location.coordinates)
            weatherResult.fold(
                onSuccess = { weather ->
                    val profile = sessionLocalStore.read()
                    val profileGreeting = buildProfileGreeting(profile)
                    val cropContext = buildCropContext(profile)
                    val soilResult = soilRepository.getSoil(location.coordinates)
                    val soilSummary = soilResult.getOrNull()?.summary
                    _uiState.value = HomeUiState.Data(
                        locationInfo = location,
                        weather = weather,
                        metrics = WeatherMetrics(
                            humidity = weather.humidity,
                            cloudCover = weather.cloudCover,
                            uvIndex = weather.uvIndex,
                        ),
                        soilSummary = soilSummary,
                        profileGreeting = profileGreeting,
                        cropContext = cropContext,
                    )
                },
                onFailure = { error ->
                    _uiState.value = HomeUiState.Error(
                        message = error.message ?: "Error desconocido",
                        retryable = true,
                    )
                },
            )
        }
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
