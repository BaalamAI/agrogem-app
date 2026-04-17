package com.agrogem.app.ui.screens.camera

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(defaultCameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
}

@Immutable
data class CameraUiState(
    val title: String,
    val guideLines: List<String>,
    val hint: String,
)

private fun defaultCameraUiState(): CameraUiState = CameraUiState(
    title = "Captura de planta",
    guideLines = listOf(
        "Asegúrate de que la hoja esté bien iluminada.",
        "Incluye el tallo y al menos 3 hojas visibles.",
        "Evita fondos muy oscuros o muy brillantes.",
    ),
    hint = "Mock: coloca la planta frente a la cámara.",
)
