package com.agrogem.app.ui.screens.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraViewModelTest {

    @Test
    fun `default ui state exposes visual guidance content`() {
        val viewModel = CameraViewModel()

        val state = viewModel.uiState.value

        assertTrue(state.title.isNotBlank())
        assertEquals(3, state.guideLines.size)
        assertTrue(state.hint.contains("Mock"))
    }
}
