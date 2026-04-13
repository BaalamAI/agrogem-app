package com.agrogem.app.ui.screens.analysis

import androidx.compose.runtime.Immutable

@Immutable
data class AnalysisUiState(
    val title: String,
    val subtitle: String,
    val progress: Float,
    val status: String,
    val steps: List<AnalysisStep>,
)

@Immutable
data class AnalysisStep(
    val id: String,
    val label: String,
    val done: Boolean,
)
