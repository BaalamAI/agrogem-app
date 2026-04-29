package com.agrogem.app.ui.screens.chat

import com.agrogem.app.data.chat.domain.LocalChatRepository
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationsViewModelTest {

    @Test
    fun `with repository lists conversations`() {
        val viewModel = ConversationsViewModel(
            localChatRepository = FakeLocalChatRepository(
                listOf(conversation("analysis_1", sampleDiagnosis("Roya"), 100L)),
            ),
        )

        val state = viewModel.uiState.value
        assertEquals(1, state.analysisConversations.size)
        assertEquals("Análisis: Roya", state.analysisConversations[0].title)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `without repository falls back to mock state`() {
        val viewModel = ConversationsViewModel(localChatRepository = null)
        val state = viewModel.uiState.value
        assertEquals(2, state.analysisConversations.size)
        assertEquals(2, state.normalConversations.size)
    }

    private class FakeLocalChatRepository(
        private val conversations: List<Conversation>,
    ) : LocalChatRepository {
        override fun getOrCreateAnalysisConversation(analysisId: String, diagnosis: DiagnosisResult): Conversation =
            conversations.first()

        override fun getByAnalysisId(analysisId: String): Conversation? =
            conversations.firstOrNull { it.analysisId == analysisId }

        override fun getById(conversationId: String): Conversation? =
            conversations.firstOrNull { it.id == conversationId }

        override fun listRecent(limit: Long): List<Conversation> = conversations
        override fun saveMessage(conversationId: String, message: ChatMessage) {}
        override fun listMessages(conversationId: String): List<ChatMessage> = emptyList()
        override fun createBlankConversation(): Conversation = conversations.first()
    }

    private fun conversation(analysisId: String, diagnosis: DiagnosisResult, timestamp: Long): Conversation = Conversation(
        id = "conv_$analysisId",
        title = "Análisis: ${diagnosis.pestName}",
        preview = diagnosis.diagnosisText,
        timestamp = timestamp,
        timestampLabel = "Ahora",
        analysisId = analysisId,
        diagnosis = diagnosis,
    )

    private fun sampleDiagnosis(pestName: String): DiagnosisResult = DiagnosisResult(
        pestName = pestName,
        confidence = 0.9f,
        severity = "Alta",
        affectedArea = "Hojas",
        cause = "Hongo",
        diagnosisText = "Infección detectada",
        treatmentSteps = listOf("Aplicar fungicida"),
    )
}
