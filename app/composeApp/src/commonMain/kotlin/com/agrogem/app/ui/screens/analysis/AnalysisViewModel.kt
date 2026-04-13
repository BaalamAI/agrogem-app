package com.agrogem.app.ui.screens.analysis

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AnalysisViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(defaultAnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun onEvent(event: AnalysisEvent) {
        when (event) {
            AnalysisEvent.OnFinishRequested -> _uiState.update {
                it.copy(
                    progress = 1f,
                    status = "Análisis completado. Reporte listo para revisión.",
                    steps = it.steps.map { step -> step.copy(done = true) },
                )
            }

            AnalysisEvent.OnCancelRequested -> Unit
        }
    }
}

sealed interface AnalysisEvent {
    data object OnFinishRequested : AnalysisEvent

    data object OnCancelRequested : AnalysisEvent
}

internal fun defaultAnalysisUiState(): AnalysisUiState = AnalysisUiState(
    title = "Análisis en progreso",
    subtitle = "Analizando irregularidades celulares y severidad de afectación.",
    progress = 0.74f,
    status = "IA activa sobre cultivo escaneado.",
    steps = listOf(
        AnalysisStep(id = "s1", label = "Identificando patrones de hojas...", done = true),
        AnalysisStep(id = "s2", label = "Consultando base de datos de plagas...", done = false),
        AnalysisStep(id = "s3", label = "Calculando severidad...", done = false),
    ),
)
