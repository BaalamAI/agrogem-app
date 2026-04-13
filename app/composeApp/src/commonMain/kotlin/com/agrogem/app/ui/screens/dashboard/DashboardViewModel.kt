package com.agrogem.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(defaultDashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.OnRefreshRequested -> Unit
            DashboardEvent.OnSeeAllRequested -> {
                _uiState.value = _uiState.value.copy(isHistoryVisible = true)
            }
            DashboardEvent.OnHistoryDismissRequested -> {
                _uiState.value = _uiState.value.copy(isHistoryVisible = false)
            }
            is DashboardEvent.OnRecentAnalysisSelected -> Unit
            is DashboardEvent.OnHistoryAnalysisSelected -> {
                _uiState.value = _uiState.value.copy(isHistoryVisible = false)
            }
        }
    }
}

sealed interface DashboardEvent {
    data object OnRefreshRequested : DashboardEvent

    data object OnSeeAllRequested : DashboardEvent

    data object OnHistoryDismissRequested : DashboardEvent

    data class OnRecentAnalysisSelected(val analysisId: String) : DashboardEvent

    data class OnHistoryAnalysisSelected(val analysisId: String) : DashboardEvent
}

internal fun defaultDashboardUiState(): DashboardUiState = DashboardUiState(
    greeting = "¡Buen día, Kevin!",
    subtitle = "Tu finca está estable, pero detectamos señales tempranas para revisar.",
    stats = listOf(
        DashboardStat(
            id = "temperature",
            value = "27°C",
            label = "Temperatura",
            severity = DashboardSeverity.Optimo,
        ),
        DashboardStat(
            id = "humidity",
            value = "64%",
            label = "Humedad",
            severity = DashboardSeverity.Atencion,
        ),
    ),
    recentAnalyses = listOf(
        RecentAnalysis(
            id = "r1",
            cropName = "Tomate",
            lotName = "Lote Norte",
            healthPercent = 92,
            severity = DashboardSeverity.Optimo,
            capturedAt = "Hace 16 min",
        ),
        RecentAnalysis(
            id = "r2",
            cropName = "Pimiento",
            lotName = "Invernadero B",
            healthPercent = 68,
            severity = DashboardSeverity.Atencion,
            capturedAt = "Hace 1 h",
        ),
        RecentAnalysis(
            id = "r3",
            cropName = "Pepino",
            lotName = "Lote Sur",
            healthPercent = 41,
            severity = DashboardSeverity.Critica,
            capturedAt = "Ayer",
        ),
    ),
    historyAnalyses = listOf(
        RecentAnalysis(
            id = "h1",
            cropName = "Tomate",
            lotName = "Lote Norte",
            healthPercent = 92,
            severity = DashboardSeverity.Optimo,
            capturedAt = "Hoy, 08:12",
        ),
        RecentAnalysis(
            id = "h2",
            cropName = "Pimiento",
            lotName = "Invernadero B",
            healthPercent = 68,
            severity = DashboardSeverity.Atencion,
            capturedAt = "Hoy, 07:05",
        ),
        RecentAnalysis(
            id = "h3",
            cropName = "Pepino",
            lotName = "Lote Sur",
            healthPercent = 41,
            severity = DashboardSeverity.Critica,
            capturedAt = "Ayer, 18:44",
        ),
        RecentAnalysis(
            id = "h4",
            cropName = "Berenjena",
            lotName = "Lote Este",
            healthPercent = 86,
            severity = DashboardSeverity.Optimo,
            capturedAt = "Ayer, 10:28",
        ),
        RecentAnalysis(
            id = "h5",
            cropName = "Lechuga",
            lotName = "Hidroponía 2",
            healthPercent = 57,
            severity = DashboardSeverity.Atencion,
            capturedAt = "Hace 2 días",
        ),
        RecentAnalysis(
            id = "h6",
            cropName = "Tomate",
            lotName = "Invernadero A",
            healthPercent = 35,
            severity = DashboardSeverity.Critica,
            capturedAt = "Hace 3 días",
        ),
    ),
    isHistoryVisible = false,
)
