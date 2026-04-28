package com.agrogem.app.ui.screens.chat

import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `with store observes analysis conversations from store`() = runTest(testDispatcher) {
        val store = ConversationStore()
        val viewModel = ConversationsViewModel(conversationStore = store)

        // Initially empty
        val initial = viewModel.uiState.value
        assertTrue(initial.analysisConversations.isEmpty())
        assertTrue(initial.normalConversations.isEmpty())
        assertEquals(false, initial.isLoading)

        // Save a conversation to the store
        store.save("analysis_001", sampleDiagnosis(pestName = "Roya"))
        advanceUntilIdle()

        val updated = viewModel.uiState.value
        assertEquals(1, updated.analysisConversations.size)
        assertEquals("Análisis: Roya", updated.analysisConversations[0].title)
        assertEquals("analysis_001", updated.analysisConversations[0].analysisId)
    }

    @Test
    fun `with store sorts conversations by timestamp descending`() = runTest(testDispatcher) {
        val store = ConversationStore()
        val viewModel = ConversationsViewModel(conversationStore = store)

        store.save("analysis_old", sampleDiagnosis(pestName = "Old"))
        advanceUntilIdle()

        // Small real delay to ensure distinct millisecond timestamps from Clock.System
        Thread.sleep(10)

        store.save("analysis_new", sampleDiagnosis(pestName = "New"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.analysisConversations.size)
        assertEquals("Análisis: New", state.analysisConversations[0].title)
        assertEquals("Análisis: Old", state.analysisConversations[1].title)
    }

    @Test
    fun `without store falls back to mock state`() = runTest(testDispatcher) {
        val viewModel = ConversationsViewModel(conversationStore = null)

        val state = viewModel.uiState.value
        assertEquals(2, state.analysisConversations.size)
        assertEquals(2, state.normalConversations.size)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `store removal updates ui state`() = runTest(testDispatcher) {
        val store = ConversationStore()
        val viewModel = ConversationsViewModel(conversationStore = store)

        store.save("analysis_001", sampleDiagnosis())
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.analysisConversations.size)

        store.remove("analysis_001")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.analysisConversations.isEmpty())
    }

    private fun sampleDiagnosis(
        pestName: String = "Roya",
    ): DiagnosisResult = DiagnosisResult(
        pestName = pestName,
        confidence = 0.92f,
        severity = "Alta",
        affectedArea = "Hojas",
        cause = "Hongo",
        diagnosisText = "Infección detectada",
        treatmentSteps = listOf("Aplicar fungicida"),
    )
}
