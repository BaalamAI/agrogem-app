package com.agrogem.app.ui.screens.dashboard

import androidx.compose.runtime.Immutable

@Immutable
data class DashboardUiState(
    val greeting: String,
    val subtitle: String,
    val stats: List<DashboardStat>,
    val recentAnalyses: List<RecentAnalysis>,
    val historyAnalyses: List<RecentAnalysis>,
    val isHistoryVisible: Boolean,
)

@Immutable
data class DashboardStat(
    val id: String,
    val value: String,
    val label: String,
    val severity: DashboardSeverity,
)

@Immutable
data class RecentAnalysis(
    val id: String,
    val cropName: String,
    val lotName: String,
    val healthPercent: Int,
    val severity: DashboardSeverity,
    val capturedAt: String,
)

enum class DashboardSeverity {
    Optimo,
    Atencion,
    Critica,
}
