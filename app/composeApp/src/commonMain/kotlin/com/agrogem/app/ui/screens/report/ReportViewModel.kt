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
    crop = "Roya del Cafeto",
    lot = "Lote Norte",
    healthScore = 45,
    statusLabel = "CRÍTICA",
    diagnosis = "Se ha detectado una infección avanzada por Hemileia vastatrix. El 45% del follaje muestra pústulas activas. Se requiere intervención inmediata para evitar la pérdida total de la cosecha. Si tienes otra pregunta puedes con gusto hacerla y yo te puedo ayudar.",
    recommendations = listOf(
        "Ajustar riego por goteo en las próximas 24h y volver a medir humedad del suelo.",
        "Aplicar monitoreo visual diario durante 3 días para detectar avance de lesiones.",
        "Priorizar revisión del sector noreste del lote donde se concentró mayor riesgo.",
    ),
)
