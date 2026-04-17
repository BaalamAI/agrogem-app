package com.agrogem.app.ui.screens.report

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(defaultReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun onEvent(event: ReportEvent) {
        // Lifecycle-safe no-op: scan again is not yet implemented
        when (event) {
            is ReportEvent.OnScanAgain -> Unit
        }
    }
}

sealed interface ReportEvent {
    data object OnScanAgain : ReportEvent
}

@Immutable
data class ReportUiState(
    val diagnosis: String,
    val recommendations: List<String>,
)

private fun defaultReportUiState(): ReportUiState = ReportUiState(
    diagnosis = "Infección por Hemileia vastatrix confirmada en 45% del follaje.",
    recommendations = listOf(
        "Aplicar fungicida sistémico en las próximas 12 horas.",
        "Revisar sistema de riego para evitar mayor humedad.",
        "Programar inspección presencial.",
    ),
)
