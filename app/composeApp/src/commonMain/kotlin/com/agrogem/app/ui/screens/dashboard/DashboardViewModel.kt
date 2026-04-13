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
    greeting = "BUENOS DÍAS, AGRICULTOR",
    subtitle = "Tu cultivo está saludable hoy.",
    stats = listOf(
        DashboardStat(
            id = "temperature",
            value = "24°C",
            label = "TEMPERATURA",
            severity = DashboardSeverity.Optimo,
            badgeLabel = "ÓPTIMO",
        ),
        DashboardStat(
            id = "humidity",
            value = "42%",
            label = "HUMEDAD\nSUELO",
            severity = DashboardSeverity.Atencion,
            badgeLabel = "BAJO",
        ),
    ),
    recentAnalyses = listOf(
        RecentAnalysis(
            id = "r1",
            cropName = "Albahaca",
            lotName = "Sin plagas detectadas",
            healthPercent = 98,
            severity = DashboardSeverity.Optimo,
            capturedAt = "Hoy, 08:12",
        ),
        RecentAnalysis(
            id = "r2",
            cropName = "Tomate",
            lotName = "Estrés hídrico leve",
            healthPercent = 72,
            severity = DashboardSeverity.Atencion,
            capturedAt = "Hoy, 07:05",
        ),
        RecentAnalysis(
            id = "r3",
            cropName = "Pimiento",
            lotName = "Presencia inicial de manchas",
            healthPercent = 41,
            severity = DashboardSeverity.Critica,
            capturedAt = "Ayer, 18:44",
        ),
    ),
    historyAnalyses = listOf(
        RecentAnalysis(
            id = "h1",
            cropName = "Albahaca",
            lotName = "Sin plagas detectadas",
            healthPercent = 98,
            severity = DashboardSeverity.Optimo,
            capturedAt = "Hoy, 08:12",
        ),
        RecentAnalysis(
            id = "h2",
            cropName = "Tomate",
            lotName = "Estrés hídrico leve",
            healthPercent = 72,
            severity = DashboardSeverity.Atencion,
            capturedAt = "Hoy, 07:05",
        ),
        RecentAnalysis(
            id = "h3",
            cropName = "Pimiento",
            lotName = "Presencia inicial de manchas",
            healthPercent = 41,
            severity = DashboardSeverity.Critica,
            capturedAt = "Ayer, 18:44",
        ),
        RecentAnalysis(
            id = "h4",
            cropName = "Lechuga",
            lotName = "Nutrición estable en hidroponía",
            healthPercent = 86,
            severity = DashboardSeverity.Optimo,
            capturedAt = "Ayer, 10:28",
        ),
        RecentAnalysis(
            id = "h5",
            cropName = "Espinaca",
            lotName = "Baja humedad superficial",
            healthPercent = 57,
            severity = DashboardSeverity.Atencion,
            capturedAt = "Hace 2 días",
        ),
        RecentAnalysis(
            id = "h6",
            cropName = "Tomate",
            lotName = "Avance de lesión fúngica",
            healthPercent = 35,
            severity = DashboardSeverity.Critica,
            capturedAt = "Hace 3 días",
        ),
    ),
    isHistoryVisible = false,
)
