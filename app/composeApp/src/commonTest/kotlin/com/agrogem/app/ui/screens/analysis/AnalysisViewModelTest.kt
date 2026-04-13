package com.agrogem.app.ui.screens.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalysisViewModelTest {

    @Test
    fun `finish event marks all steps as done`() {
        val viewModel = AnalysisViewModel()

        viewModel.onEvent(AnalysisEvent.OnFinishRequested)

        val state = viewModel.uiState.value
        assertEquals(1f, state.progress)
        assertTrue(state.steps.all { it.done })
        assertTrue(state.status.contains("completado"))
    }
}
