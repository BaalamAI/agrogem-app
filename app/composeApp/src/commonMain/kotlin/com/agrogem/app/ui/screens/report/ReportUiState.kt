package com.agrogem.app.ui.screens.report

import androidx.compose.runtime.Immutable

@Immutable
data class ReportUiState(
    val title: String,
    val crop: String,
    val lot: String,
    val healthScore: Int,
    val statusLabel: String,
    val diagnosis: String,
    val recommendations: List<String>,
)
