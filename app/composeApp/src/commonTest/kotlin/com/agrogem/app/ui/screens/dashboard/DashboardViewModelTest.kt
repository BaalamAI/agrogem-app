package com.agrogem.app.ui.screens.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardViewModelTest {

    @Test
    fun `default ui state contains expected dashboard sections`() {
        val viewModel = DashboardViewModel()

        val state = viewModel.uiState.value

        assertTrue(state.greeting.isNotBlank())
        assertEquals(2, state.stats.size)
        assertEquals(3, state.recentAnalyses.size)
        assertTrue(state.historyAnalyses.isNotEmpty())
        assertEquals(false, state.isHistoryVisible)
    }

    @Test
    fun `history events toggle visibility while preserving mock data`() {
        val viewModel = DashboardViewModel()
        val initial = viewModel.uiState.value

        viewModel.onEvent(DashboardEvent.OnSeeAllRequested)

        val visibleState = viewModel.uiState.value
        assertEquals(true, visibleState.isHistoryVisible)
        assertEquals(initial.historyAnalyses, visibleState.historyAnalyses)

        viewModel.onEvent(DashboardEvent.OnHistoryAnalysisSelected("h1"))

        val hiddenState = viewModel.uiState.value
        assertEquals(false, hiddenState.isHistoryVisible)
        assertEquals(initial.historyAnalyses, hiddenState.historyAnalyses)
    }

    @Test
    fun `rapid history events keep deterministic final visibility`() {
        val viewModel = DashboardViewModel()

        repeat(100) {
            viewModel.onEvent(DashboardEvent.OnSeeAllRequested)
            viewModel.onEvent(DashboardEvent.OnHistoryDismissRequested)
        }

        assertEquals(false, viewModel.uiState.value.isHistoryVisible)
    }
}
