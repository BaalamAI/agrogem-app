package com.agrogem.app.ui.screens.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(defaultCameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onEvent(event: CameraEvent) {
        when (event) {
            CameraEvent.OnToggleGuide -> Unit
            CameraEvent.OnStartAnalysis -> Unit
        }
    }
}

sealed interface CameraEvent {
    data object OnToggleGuide : CameraEvent

    data object OnStartAnalysis : CameraEvent
}

internal fun defaultCameraUiState(): CameraUiState = CameraUiState(
    title = "Escaneo IA",
    subtitle = "Alineá la hoja dentro del marco para iniciar un diagnóstico simulado.",
    guideLines = listOf(
        "Buena iluminación natural",
        "Evitá sombras sobre la hoja",
        "Mantené la cámara estable",
    ),
    primaryActionLabel = "Iniciar análisis",
    hint = "Mock visual — sin integración real de cámara en esta fase.",
)
