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

    @Test
    fun `finish and view report triggers callback and updates state`() {
        val viewModel = AnalysisViewModel()
        var reportOpened = false

        viewModel.onFinishAndViewReport {
            reportOpened = true
        }

        val state = viewModel.uiState.value
        assertTrue(reportOpened)
        assertEquals(1f, state.progress)
        assertTrue(state.steps.all { it.done })
    }

    @Test
    fun `finish and view report can be requested multiple times`() {
        val viewModel = AnalysisViewModel()
        var callbackCount = 0

        viewModel.onFinishAndViewReport { callbackCount++ }
        viewModel.onFinishAndViewReport { callbackCount++ }

        val state = viewModel.uiState.value
        assertEquals(2, callbackCount)
        assertEquals(1f, state.progress)
        assertTrue(state.steps.all { it.done })
    }
}
