package com.agrogem.app.ui.screens.camera

import androidx.compose.runtime.Immutable

@Immutable
data class CameraUiState(
    val title: String,
    val subtitle: String,
    val guideLines: List<String>,
    val primaryActionLabel: String,
    val hint: String,
)
