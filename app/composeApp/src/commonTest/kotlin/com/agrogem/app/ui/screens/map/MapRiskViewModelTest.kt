package com.agrogem.app.ui.screens.map

import kotlin.test.Test
import kotlin.test.assertTrue

class MapRiskViewModelTest {

    @Test
    fun `default map risk state exposes markers and alerts`() {
        val viewModel = MapRiskViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.markers.isNotEmpty())
        assertTrue(state.alerts.isNotEmpty())
    }
}
