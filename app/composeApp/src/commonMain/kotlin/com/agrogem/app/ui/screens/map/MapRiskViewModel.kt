package com.agrogem.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.risk.domain.DiseaseRisk
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.risk.domain.RiskSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface MapRiskViewModelState {
    data object Loading : MapRiskViewModelState
    data class Error(val message: String) : MapRiskViewModelState
    data class Success(val data: MapRiskUiState) : MapRiskViewModelState
}

class MapRiskViewModel(
    private val geolocationRepository: GeolocationRepository,
    private val riskRepository: RiskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapRiskViewModelState>(MapRiskViewModelState.Loading)
    val uiState: StateFlow<MapRiskViewModelState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: MapRiskEvent) {
        when (event) {
            MapRiskEvent.OnRefreshRequested -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = MapRiskViewModelState.Loading
            val location = geolocationRepository.observeResolvedLocation().first()
            if (location == null) {
                _uiState.value = MapRiskViewModelState.Error("No se encontró ubicación. Guarda una ubicación primero.")
                return@launch
            }
            val result = riskRepository.getDiseaseRisks(location.coordinates)
            result.fold(
                onSuccess = { diseases ->
                    _uiState.value = MapRiskViewModelState.Success(
                        buildMapRiskUiState(diseases, location.display.primary)
                    )
                },
                onFailure = { error ->
                    _uiState.value = MapRiskViewModelState.Error(
                        error.message ?: "Error de conexión"
                    )
                },
            )
        }
    }
}

sealed interface MapRiskEvent {
    data object OnRefreshRequested : MapRiskEvent
}

internal fun buildMapRiskUiState(diseases: List<DiseaseRisk>, locationName: String): MapRiskUiState {
    val criticalCount = diseases.count { it.severity == RiskSeverity.Critica }
    val attentionCount = diseases.count { it.severity == RiskSeverity.Atencion }

    return MapRiskUiState(
        title = "Mapa de riesgo\nregional",
        subtitle = if (locationName.isNotBlank()) "Ubicación: $locationName" else "Vista regional de alertas de plagas sobre capa estática.",
        riskSummary = listOf(
            RiskSummaryItem(label = "Lotes monitoreados", value = diseases.size.toString()),
            RiskSummaryItem(label = "Alertas críticas", value = criticalCount.toString()),
            RiskSummaryItem(label = "Atención", value = attentionCount.toString()),
        ),
        markers = diseases.mapIndexed { index, disease ->
            RiskMarker(
                id = disease.diseaseName,
                lot = disease.displayName,
                riskLabel = disease.displayName,
                xFraction = 0.22f + index * 0.28f,
                yFraction = 0.3f + index * 0.2f,
                severity = disease.severity,
            )
        },
        alerts = diseases.filter { it.severity != RiskSeverity.Optimo }.map { disease ->
            RiskAlert(
                id = disease.diseaseName,
                lot = disease.displayName,
                detail = disease.interpretation,
                recommendation = when (disease.severity) {
                    RiskSeverity.Critica -> "Priorizar inspección presencial en las próximas 12h."
                    RiskSeverity.Atencion -> "Ajustar riego y re-evaluar mañana por la mañana."
                    RiskSeverity.Optimo -> "Monitorear condiciones regulares."
                },
                severity = disease.severity,
            )
        },
        disclaimer = "Riesgo estimado para cultivos comunes de la región.",
    )
}
