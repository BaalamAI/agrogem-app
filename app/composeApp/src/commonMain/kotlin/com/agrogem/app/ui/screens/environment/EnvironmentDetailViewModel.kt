package com.agrogem.app.ui.screens.environment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.climate.domain.ClimateHistory
import com.agrogem.app.data.climate.domain.ClimateQuery
import com.agrogem.app.data.climate.domain.ClimateRepository
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.soil.domain.SoilProfile
import com.agrogem.app.data.soil.domain.SoilRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EnvironmentDetailEffect {
    data object NavigateBack : EnvironmentDetailEffect
}

class EnvironmentDetailViewModel(
    private val soilRepository: SoilRepository,
    private val climateRepository: ClimateRepository,
    private val location: ResolvedLocation,
    private val query: ClimateQuery,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EnvironmentDetailUiState>(EnvironmentDetailUiState.Loading)
    val uiState: StateFlow<EnvironmentDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EnvironmentDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<EnvironmentDetailEffect> = _effects.asSharedFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun onBack() {
        _effects.tryEmit(EnvironmentDetailEffect.NavigateBack)
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = EnvironmentDetailUiState.Loading

            val soilDeferred = async { fetchSoil() }
            val climateDeferred = async { fetchClimate() }

            val soilResult = soilDeferred.await()
            val climateResult = climateDeferred.await()

            val soil = soilResult.getOrNull()
            val climate = climateResult.getOrNull()
            val soilError = soilResult.exceptionOrNull()
            val climateError = climateResult.exceptionOrNull()

            if (soil == null && climate == null) {
                _uiState.value = EnvironmentDetailUiState.Error(
                    message = buildAggregateMessage(soilError, climateError),
                    canRetry = true,
                )
            } else {
                _uiState.value = EnvironmentDetailUiState.Success(
                    soil = soil,
                    climate = climate,
                    interpretation = soil?.interpretation ?: "",
                    locationName = location.display.primary,
                    elevationMeters = location.elevationMeters,
                )
            }
        }
    }

    private suspend fun fetchSoil(): Result<SoilProfile> =
        try {
            soilRepository.getSoil(location.coordinates)
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun fetchClimate(): Result<ClimateHistory> =
        try {
            climateRepository.getClimateHistory(
                latLng = location.coordinates,
                start = query.start,
                end = query.end,
                granularity = query.granularity,
            )
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun buildAggregateMessage(soilError: Throwable?, climateError: Throwable?): String {
        val parts = mutableListOf<String>()
        soilError?.message?.let { parts.add("Suelo: $it") }
        climateError?.message?.let { parts.add("Clima: $it") }
        return if (parts.isEmpty()) "Error desconocido" else parts.joinToString("\n")
    }
}
