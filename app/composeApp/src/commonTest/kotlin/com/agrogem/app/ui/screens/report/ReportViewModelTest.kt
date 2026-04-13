package com.agrogem.app.ui.screens.report

import kotlin.test.Test
import kotlin.test.assertTrue

class ReportViewModelTest {

    @Test
    fun `default report state contains diagnosis and recommendations`() {
        val viewModel = ReportViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.diagnosis.isNotBlank())
        assertTrue(state.recommendations.isNotEmpty())
    }
}
