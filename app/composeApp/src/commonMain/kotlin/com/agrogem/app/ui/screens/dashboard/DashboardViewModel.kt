package com.agrogem.app.ui.screens.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(defaultDashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnSeeAllRequested -> {
                _uiState.value = _uiState.value.copy(isHistoryVisible = true)
            }
            is DashboardEvent.OnHistoryDismissRequested -> {
                _uiState.value = _uiState.value.copy(isHistoryVisible = false)
            }
            is DashboardEvent.OnHistoryAnalysisSelected -> {
                _uiState.value = _uiState.value.copy(isHistoryVisible = false)
            }
        }
    }
}

sealed interface DashboardEvent {
    data object OnSeeAllRequested : DashboardEvent
    data object OnHistoryDismissRequested : DashboardEvent
    data class OnHistoryAnalysisSelected(val id: String) : DashboardEvent
}

@Immutable
data class DashboardUiState(
    val greeting: String,
    val stats: List<DashboardStat>,
    val recentAnalyses: List<RecentAnalysis>,
    val historyAnalyses: List<HistoryAnalysis>,
    val isHistoryVisible: Boolean,
)

@Immutable
data class DashboardStat(
    val label: String,
    val value: String,
)

@Immutable
data class RecentAnalysis(
    val id: String,
    val title: String,
    val severity: String,
)

@Immutable
data class HistoryAnalysis(
    val id: String,
    val title: String,
    val severity: String,
)

private fun defaultDashboardUiState(): DashboardUiState = DashboardUiState(
    greeting = "Buenos días, agricultor",
    stats = listOf(
        DashboardStat(label = "Lotes activos", value = "8"),
        DashboardStat(label = "Alertas", value = "3"),
    ),
    recentAnalyses = listOf(
        RecentAnalysis(id = "r1", title = "Lote Norte", severity = "Crítica"),
        RecentAnalysis(id = "r2", title = "Lote Sur", severity = "Óptimo"),
        RecentAnalysis(id = "r3", title = "Lote Este", severity = "Atención"),
    ),
    historyAnalyses = listOf(
        HistoryAnalysis(id = "h1", title = "Lote Norte - Historial", severity = "Crítica"),
        HistoryAnalysis(id = "h2", title = "Lote Sur - Historial", severity = "Óptimo"),
        HistoryAnalysis(id = "h3", title = "Lote Este - Historial", severity = "Atención"),
        HistoryAnalysis(id = "h4", title = "Lote Oeste - Historial", severity = "Crítica"),
    ),
    isHistoryVisible = false,
)
