package com.agrogem.app.ui.screens.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapRiskViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(defaultMapRiskUiState())
    val uiState: StateFlow<MapRiskUiState> = _uiState.asStateFlow()
}

internal fun defaultMapRiskUiState(): MapRiskUiState = MapRiskUiState(
    title = "Mapa de riesgo",
    subtitle = "Vista estática del lote central con alertas fitosanitarias simuladas.",
    riskSummary = listOf(
        RiskSummaryItem(label = "Lotes monitoreados", value = "12"),
        RiskSummaryItem(label = "Alertas críticas", value = "2"),
        RiskSummaryItem(label = "Atención", value = "4"),
    ),
    markers = listOf(
        RiskMarker(
            id = "m1",
            lot = "Lote Norte",
            riskLabel = "Riesgo alto",
            xFraction = 0.22f,
            yFraction = 0.36f,
            severity = RiskSeverity.Critica,
        ),
        RiskMarker(
            id = "m2",
            lot = "Lote Este",
            riskLabel = "Estrés hídrico",
            xFraction = 0.58f,
            yFraction = 0.28f,
            severity = RiskSeverity.Atencion,
        ),
        RiskMarker(
            id = "m3",
            lot = "Lote Sur",
            riskLabel = "Óptimo",
            xFraction = 0.7f,
            yFraction = 0.7f,
            severity = RiskSeverity.Optimo,
        ),
    ),
    alerts = listOf(
        RiskAlert(
            id = "a1",
            lot = "Lote Norte",
            detail = "Concentración de manchas foliares por encima del umbral interno.",
            recommendation = "Priorizar inspección presencial en las próximas 12h.",
            severity = RiskSeverity.Critica,
        ),
        RiskAlert(
            id = "a2",
            lot = "Lote Este",
            detail = "Descenso sostenido de humedad en la última ventana simulada.",
            recommendation = "Ajustar riego y re-evaluar mañana por la mañana.",
            severity = RiskSeverity.Atencion,
        ),
    ),
    disclaimer = "Maqueta estática: sin SDK de mapas ni geolocalización real.",
)
