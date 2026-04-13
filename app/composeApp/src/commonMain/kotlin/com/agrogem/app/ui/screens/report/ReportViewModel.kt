package com.agrogem.app.ui.screens.report

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(defaultReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
}

internal fun defaultReportUiState(): ReportUiState = ReportUiState(
    title = "Reporte de análisis",
    crop = "Tomate",
    lot = "Lote Norte",
    healthScore = 68,
    statusLabel = "ATENCIÓN",
    diagnosis = "Se detectan signos tempranos compatibles con estrés hídrico y manchas foliares leves en el tercio medio.",
    recommendations = listOf(
        "Ajustar riego por goteo en las próximas 24h y volver a medir humedad del suelo.",
        "Aplicar monitoreo visual diario durante 3 días para detectar avance de lesiones.",
        "Priorizar revisión del sector noreste del lote donde se concentró mayor riesgo.",
    ),
)
