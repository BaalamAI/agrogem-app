package com.agrogem.app.ui.screens.chat

import com.agrogem.app.data.chat.domain.ChatFailure
import com.agrogem.app.data.chat.domain.ChatRepository
import com.agrogem.app.data.chat.domain.ChatSendResult
import com.agrogem.app.data.chat.domain.LocalChatRepository
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeRepo(
        result: ChatSendResult = ChatSendResult.Success(
            conversationId = "conv-1",
            messages = listOf(
                ChatMessage(
                    id = "backend_assistant_1",
                    text = "Respuesta del asistente",
                    sender = MessageSender.Assistant,
                    attachments = emptyList(),
                    timestamp = 1000L,
                ),
            ),
        ),
    ): FakeChatRepository = FakeChatRepository(result)

    @Test
    fun `6_1_text_message_appears_in_history_after_send`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())
        val state0 = viewModel.uiState.value
        assertTrue(state0.messages.isEmpty(), "Initially no messages")

        viewModel.onEvent(ChatEvent.InputChanged("How do I treat this?"))
        assertEquals("How do I treat this?", viewModel.uiState.value.inputText)

        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state1 = viewModel.uiState.value
        assertEquals(2, state1.messages.size, "User and assistant messages after send")
        assertEquals("How do I treat this?", state1.messages[0].text)
        assertEquals(MessageSender.User, state1.messages[0].sender)
        assertEquals(MessageSender.Assistant, state1.messages[1].sender)
        assertEquals("", state1.inputText, "Input cleared after send")
    }

    @Test
    fun `send_message_persists_user_and_assistant_messages_locally`() = runTest(testDispatcher) {
        val localRepo = FakeLocalChatRepository()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            localChatRepository = localRepo,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Mensaje local"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(2, localRepo.savedMessages.size)
        assertEquals(MessageSender.User, localRepo.savedMessages[0].sender)
        assertEquals(MessageSender.Assistant, localRepo.savedMessages[1].sender)
    }

    @Test
    fun `6_1_multiple_messages_appear_in_order`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("First"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        viewModel.onEvent(ChatEvent.InputChanged("Second"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.messages.size)
        assertEquals("First", state.messages[0].text)
        assertEquals("Second", state.messages[2].text)
    }

    @Test
    fun `6_1_send_with_empty_input_does_nothing`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())
        viewModel.onEvent(ChatEvent.SendMessage)
        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `6_2_send_with_image_creates_message_with_image_attachment`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo1.jpg"))
        viewModel.onEvent(ChatEvent.InputChanged("Look at this pest"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        val userMessage = state.messages[0]
        assertEquals("Look at this pest", userMessage.text)
        assertEquals(1, userMessage.attachments.size)
        assertIs<ChatAttachment.Image>(userMessage.attachments[0])
        assertEquals("content://media/photo1.jpg", (userMessage.attachments[0] as ChatAttachment.Image).uri)
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
        assertEquals(emptyList(), state.attachments, "Attachments cleared after send")
    }

    @Test
    fun `6_2_send_with_multiple_images_preserves_all_attachments`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo1.jpg"))
        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo2.jpg"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(2, state.messages[0].attachments.size)
        assertEquals(emptyList(), state.attachments)
    }

    @Test
    fun `6_2_image_only_message_sends_without_text`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo1.jpg"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("", state.messages[0].text)
        assertEquals(1, state.messages[0].attachments.size)
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
    }

    @Test
    fun `6_3_stop_voice_with_empty_input_does_not_create_message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        assertIs<VoiceState.Listening>(viewModel.uiState.value.voiceState)

        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        assertEquals(VoiceState.Idle, state.voiceState, "Returns to Idle after stop")
        assertEquals(0, state.messages.size, "No message created for empty voice input")
    }

    @Test
    fun `6_3_voice_with_pending_text_includes_text_in_message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("urgent question"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("urgent question", state.messages[0].text)
        assertEquals(1, state.messages[0].attachments.size)
        assertIs<ChatAttachment.Audio>(state.messages[0].attachments[0])
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
    }

    @Test
    fun `6_3_dismiss_voice_does_not_create_message`() {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("typed text"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        val state = viewModel.uiState.value
        assertEquals(VoiceState.Idle, state.voiceState)
        assertEquals(0, state.messages.size, "No message after dismiss")
        assertEquals("typed text", state.inputText, "Typed text preserved after dismiss")
    }

    @Test
    fun `6_3_voice_twice_creates_two_messages`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("First voice"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)
        advanceUntilIdle()

        viewModel.onEvent(ChatEvent.InputChanged("Second voice"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `6_4_chat_from_analysis_initializes_in_seeded_mode_with_diagnosis`() {
        val diagnosis = DiagnosisResult(
            pestName = "Roya naranja",
            confidence = 0.87f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo Puccinia",
            diagnosisText = "Roya naranja detectada en 30% del follaje.",
            treatmentSteps = listOf("Aplicar fungicida sistémico"),
        )

        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            analysisId = "analysis_session_001",
            diagnosis = diagnosis,
        )

        val state = viewModel.uiState.value
        assertIs<ChatMode.AnalysisSeeded>(state.mode)
        assertEquals("analysis_session_001", state.mode.analysisId)
        assertEquals("Roya naranja", state.mode.diagnosis.pestName)

        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Roya naranja"))
    }

    @Test
    fun `6_4_seeded_chat_allows_follow_up_messages`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            analysisId = "analysis_xyz",
            diagnosis = DiagnosisResult(
                pestName = "Test",
                confidence = 0.9f,
                severity = "Baja",
                affectedArea = "Tallo",
                cause = "Unknown",
                diagnosisText = "Test diagnosis",
                treatmentSteps = listOf("No treatment"),
            ),
        )

        assertEquals(1, viewModel.uiState.value.messages.size)

        viewModel.onEvent(ChatEvent.InputChanged("What should I do next?"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.messages.size)
        assertEquals("What should I do next?", viewModel.uiState.value.messages[1].text)
        assertEquals(MessageSender.User, viewModel.uiState.value.messages[1].sender)
        assertEquals(MessageSender.Assistant, viewModel.uiState.value.messages[2].sender)
    }

    @Test
    fun `6_4_blank_chat_has_no_initial_message`() {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        val state = viewModel.uiState.value
        assertIs<ChatMode.Blank>(state.mode)
        assertEquals(0, state.messages.size, "Blank chat starts with no messages")
    }

    @Test
    fun `6_5_seedFromAnalysis_transitions_blank_to_seeded_mode`() {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())
        assertIs<ChatMode.Blank>(viewModel.uiState.value.mode)
        assertEquals(0, viewModel.uiState.value.messages.size)

        val analysisId = "analysis_runtime_001"
        val diagnosis = DiagnosisResult(
            pestName = "Roya naranja",
            confidence = 0.87f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo Puccinia",
            diagnosisText = "Roya naranja detectada en 30% del follaje.",
            treatmentSteps = listOf("Aplicar fungicida sistémico"),
        )
        viewModel.seedFromAnalysis(analysisId, diagnosis)

        val state = viewModel.uiState.value
        assertIs<ChatMode.AnalysisSeeded>(state.mode)
        assertEquals(analysisId, state.mode.analysisId)
        assertEquals("Roya naranja", state.mode.diagnosis.pestName)
        assertEquals(0.87f, state.mode.diagnosis.confidence)
        assertEquals("Alta", state.mode.diagnosis.severity)
        assertEquals("Hojas", state.mode.diagnosis.affectedArea)
        assertEquals("Hongo Puccinia", state.mode.diagnosis.cause)

        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Roya naranja"))
    }

    @Test
    fun `6_5_seedFromAnalysis_replaces_existing_seed_with_new_diagnosis`() {
        val initialDiagnosis = DiagnosisResult(
            pestName = "Initial Pest",
            confidence = 0.80f,
            severity = "Baja",
            affectedArea = "Tallo",
            cause = "Unknown",
            diagnosisText = "Initial diagnosis text.",
            treatmentSteps = listOf("Initial treatment"),
        )
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            analysisId = "analysis_first",
            diagnosis = initialDiagnosis,
        )
        assertEquals("Initial Pest", viewModel.uiState.value.mode.let { (it as ChatMode.AnalysisSeeded).diagnosis.pestName })

        val newDiagnosis = DiagnosisResult(
            pestName = "Nueva plaga",
            confidence = 0.95f,
            severity = "Crítica",
            affectedArea = "Raíz",
            cause = "Nematodo",
            diagnosisText = "Nueva plaga detectada.",
            treatmentSteps = listOf("Aplicar nematicida"),
        )
        viewModel.seedFromAnalysis("analysis_second", newDiagnosis)

        val state = viewModel.uiState.value
        assertIs<ChatMode.AnalysisSeeded>(state.mode)
        assertEquals("analysis_second", state.mode.analysisId)
        assertEquals("Nueva plaga", state.mode.diagnosis.pestName)
        assertEquals("Crítica", state.mode.diagnosis.severity)
        assertEquals("Raíz", state.mode.diagnosis.affectedArea)
        assertEquals("Nematodo", state.mode.diagnosis.cause)

        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Nueva plaga"))
    }

    @Test
    fun `shared_viewmodel_voice_then_text_same_history`() = runTest(testDispatcher) {
        val chatViewModel = ChatViewModel(chatRepository = fakeRepo())

        chatViewModel.onEvent(ChatEvent.InputChanged("I need help with this"))
        chatViewModel.onEvent(ChatEvent.StartVoiceInput)
        chatViewModel.onEvent(ChatEvent.StopVoiceInput)
        advanceUntilIdle()

        val state = chatViewModel.uiState.value
        assertEquals(2, state.messages.size)
        val userMessage = state.messages[0]
        val assistantMessage = state.messages[1]
        assertEquals("I need help with this", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertEquals(1, userMessage.attachments.size)
        assertIs<ChatAttachment.Audio>(userMessage.attachments[0])
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
    }

    @Test
    fun `6_6_store_recovers_context_when_reopening_chat_by_analysisId`() {
        val localRepo = FakeLocalChatRepository()
        val diagnosis = DiagnosisResult(
            pestName = "Roya naranja",
            confidence = 0.87f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo Puccinia",
            diagnosisText = "Roya naranja detectada en 30% del follaje.",
            treatmentSteps = listOf("Aplicar fungicida sistémico"),
        )
        val analysisId = "analysis_store_001"
        localRepo.getOrCreateAnalysisConversation(analysisId, diagnosis)

        // Fresh ChatViewModel simulating reopening after process navigation
        val chatViewModel = ChatViewModel(chatRepository = fakeRepo())
        assertIs<ChatMode.Blank>(chatViewModel.uiState.value.mode)

        // Recover from store (mirrors what AppNavHost does)
        localRepo.getByAnalysisId(analysisId)?.diagnosis?.let {
            chatViewModel.seedFromAnalysis(analysisId, it)
        }

        val state = chatViewModel.uiState.value
        assertIs<ChatMode.AnalysisSeeded>(state.mode)
        assertEquals(analysisId, state.mode.analysisId)
        assertEquals("Roya naranja", state.mode.diagnosis.pestName)
        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Roya naranja"))
    }

    @Test
    fun `6_6_conversationsViewModel_reflects_store_entries`() = runTest(testDispatcher) {
        val localRepo = FakeLocalChatRepository()
        val viewModel = ConversationsViewModel(localChatRepository = localRepo)
        advanceUntilIdle()

        localRepo.getOrCreateAnalysisConversation("analysis_001", DiagnosisResult(
            pestName = "Broca",
            confidence = 0.95f,
            severity = "Crítica",
            affectedArea = "Fruto",
            cause = "Hypothenemus hampei",
            diagnosisText = "Infestación severa detectada",
            treatmentSteps = listOf("Recolectar bayas caídas"),
        ))

        val refreshed = ConversationsViewModel(localChatRepository = localRepo)
        assertEquals(1, refreshed.uiState.value.analysisConversations.size)
        assertEquals("Análisis: Broca", refreshed.uiState.value.analysisConversations[0].title)
        assertEquals("analysis_001", refreshed.uiState.value.analysisConversations[0].analysisId)
    }

    private class FakeChatRepository(
        var result: ChatSendResult,
    ) : ChatRepository {
        override suspend fun sendMessage(text: String, attachments: List<ChatAttachment>, mode: ChatMode): ChatSendResult {
            return result
        }
    }

    private class FakeLocalChatRepository : LocalChatRepository {
        private val conversations = mutableMapOf<String, Conversation>()
        val savedMessages = mutableListOf<ChatMessage>()

        override fun getOrCreateAnalysisConversation(analysisId: String, diagnosis: DiagnosisResult): Conversation {
            return conversations[analysisId] ?: Conversation(
                id = "conv_$analysisId",
                title = "Análisis: ${diagnosis.pestName}",
                preview = diagnosis.diagnosisText,
                timestamp = 1L,
                timestampLabel = "Ahora",
                analysisId = analysisId,
                diagnosis = diagnosis,
            ).also { conversations[analysisId] = it }
        }

        override fun getByAnalysisId(analysisId: String): Conversation? = conversations[analysisId]
        override fun getById(conversationId: String): Conversation? = conversations.values.firstOrNull { it.id == conversationId }
        override fun listRecent(limit: Long): List<Conversation> = conversations.values.toList()
        override fun saveMessage(conversationId: String, message: ChatMessage) {
            savedMessages.add(message)
        }
        override fun listMessages(conversationId: String): List<ChatMessage> = emptyList()
        override fun createBlankConversation(): Conversation = Conversation(
            id = "conv_blank",
            title = "Nueva conversación",
            preview = "",
            timestamp = 1L,
            timestampLabel = "Ahora",
        )
    }
}
