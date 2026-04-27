package com.agrogem.app.ui.screens.environment

import com.agrogem.app.data.climate.domain.ClimateHistory
import com.agrogem.app.data.soil.domain.SoilProfile

sealed interface EnvironmentDetailUiState {
    data object Loading : EnvironmentDetailUiState
    data class Success(
        val soil: SoilProfile?,
        val climate: ClimateHistory?,
        val interpretation: String,
        val locationName: String,
        val elevationMeters: Double?,
    ) : EnvironmentDetailUiState
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
    ) : EnvironmentDetailUiState
}
