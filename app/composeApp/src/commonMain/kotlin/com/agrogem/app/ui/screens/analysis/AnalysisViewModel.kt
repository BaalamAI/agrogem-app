package com.agrogem.app.ui.screens.analysis

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AnalysisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(defaultAnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _finishCallbacks = mutableListOf<() -> Unit>()

    val onFinishAndViewReport: (callback: () -> Unit) -> Unit = { callback ->
        _finishCallbacks.add(callback)
        callback()
        _uiState.update {
            it.copy(
                progress = 1f,
                steps = it.steps.map { step -> step.copy(done = true) },
                status = "completado",
            )
        }
    }

    fun onEvent(event: AnalysisEvent) {
        when (event) {
            is AnalysisEvent.OnFinishRequested -> {
                _uiState.update {
                    it.copy(
                        progress = 1f,
                        steps = it.steps.map { step -> step.copy(done = true) },
                        status = "completado",
                    )
                }
            }
        }
    }
}

sealed interface AnalysisEvent {
    data object OnFinishRequested : AnalysisEvent
}

@Immutable
data class AnalysisUiState(
    val progress: Float,
    val steps: List<AnalysisStep>,
    val status: String,
)

@Immutable
data class AnalysisStep(
    val title: String,
    val subtitle: String,
    val done: Boolean,
)

private fun defaultAnalysisUiState(): AnalysisUiState = AnalysisUiState(
    progress = 0f,
    steps = listOf(
        AnalysisStep(title = "Identificando...", subtitle = "Procesando", done = false),
        AnalysisStep(title = "Consultando...", subtitle = "Sincronizando", done = false),
        AnalysisStep(title = "Calculando...", subtitle = "Estimando", done = false),
        AnalysisStep(title = "Generando...", subtitle = "Finalizando", done = false),
    ),
    status = "En progreso",
)
