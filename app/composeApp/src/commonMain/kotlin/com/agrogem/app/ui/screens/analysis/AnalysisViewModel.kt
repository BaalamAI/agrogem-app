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
    subtitle = "Procesando imagen para identificar señales de estrés y enfermedades.",
    progress = 0.72f,
    status = "Aplicando modelo fitosanitario sobre el cultivo escaneado...",
    steps = listOf(
        AnalysisStep(id = "s1", label = "Preprocesamiento de imagen", done = true),
        AnalysisStep(id = "s2", label = "Detección de lesiones", done = true),
        AnalysisStep(id = "s3", label = "Clasificación de severidad", done = false),
        AnalysisStep(id = "s4", label = "Generación de recomendaciones", done = false),
    ),
)
