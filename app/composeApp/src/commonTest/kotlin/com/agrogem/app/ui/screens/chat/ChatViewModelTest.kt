package com.agrogem.app.ui.screens.chat

import com.agrogem.app.data.chat.domain.ChatFailure
import com.agrogem.app.data.chat.domain.ChatRepository
import com.agrogem.app.data.chat.domain.ChatSendResult
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class ChatViewModelTest {

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

    // ========== Initialization Tests ==========

    @Test
    fun `default initialization starts in blank mode with empty messages`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        val state = viewModel.uiState.value
        assertIs<ChatMode.Blank>(state.mode)
        assertEquals(emptyList(), state.messages)
        assertEquals("", state.inputText)
        assertEquals(emptyList(), state.attachments)
        assertEquals(VoiceState.Idle, state.voiceState)
        assertEquals(false, state.showAttachmentMenu)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `seeded initialization with analysisId starts in analysis seeded mode with first message`() = runTest(testDispatcher) {
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detected",
            treatmentSteps = listOf("Apply fungicide"),
        )
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            analysisId = "analysis_123",
            diagnosis = diagnosis,
        )

        val state = viewModel.uiState.value
        val seededMode = state.mode as ChatMode.AnalysisSeeded
        assertEquals("analysis_123", seededMode.analysisId)
        assertEquals("Roya", seededMode.diagnosis.pestName)

        // Should have initial assistant message with diagnosis context
        assertEquals(1, state.messages.size)
        val firstMessage = state.messages[0]
        assertEquals(MessageSender.Assistant, firstMessage.sender)
        assertTrue(firstMessage.text.isNotEmpty())
    }

    @Test
    fun `seeded initialization without diagnosis uses mock diagnosis`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            analysisId = "analysis_456",
            diagnosis = null,
        )

        val state = viewModel.uiState.value
        val seededMode = state.mode as ChatMode.AnalysisSeeded
        assertEquals("analysis_456", seededMode.analysisId)
        assertEquals(1, state.messages.size)
    }

    // ========== InputChanged Event Tests ==========

    @Test
    fun `InputChanged updates inputText in state`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("Hello world"))

        assertEquals("Hello world", viewModel.uiState.value.inputText)
    }

    @Test
    fun `InputChanged with empty text clears input`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("Some text"))
        viewModel.onEvent(ChatEvent.InputChanged(""))

        assertEquals("", viewModel.uiState.value.inputText)
    }

    // ========== SendMessage Event Tests ==========

    @Test
    fun `SendMessage shows optimistic user message immediately before API resolves`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("What is this pest?"))
        viewModel.onEvent(ChatEvent.SendMessage)

        // Before advancing coroutines, only optimistic user message should be present
        val stateBefore = viewModel.uiState.value
        assertEquals(1, stateBefore.messages.size)
        assertEquals("What is this pest?", stateBefore.messages[0].text)
        assertEquals(MessageSender.User, stateBefore.messages[0].sender)
        assertEquals(true, stateBefore.isLoading)
        assertEquals(null, stateBefore.error)
    }

    @Test
    fun `SendMessage appends assistant messages on success`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("What is this pest?"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        val assistantMessage = state.messages[1]
        assertEquals("What is this pest?", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
        assertEquals("Respuesta del asistente", assistantMessage.text)
        assertEquals("", state.inputText)
        assertEquals(emptyList(), state.attachments)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `SendMessage does nothing when input is empty`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.SendMessage)

        assertEquals(emptyList(), viewModel.uiState.value.messages)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `SendMessage adds multiple messages in order`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("First message"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        viewModel.onEvent(ChatEvent.InputChanged("Second message"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.messages.size)
        assertEquals("First message", state.messages[0].text)
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
        assertEquals("Second message", state.messages[2].text)
        assertEquals(MessageSender.Assistant, state.messages[3].sender)
    }

    @Test
    fun `SendMessage clears pending attachments`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("Message with photo"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(emptyList(), state.attachments)
    }

    @Test
    fun `SendMessage keeps user message and sets error on failure`() = runTest(testDispatcher) {
        val repo = fakeRepo(
            result = ChatSendResult.Failure(ChatFailure.Network(Exception("timeout")))
        )
        val viewModel = ChatViewModel(chatRepository = repo)

        viewModel.onEvent(ChatEvent.InputChanged("Help!"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Help!", state.messages[0].text)
        assertEquals(MessageSender.User, state.messages[0].sender)
        assertEquals(false, state.isLoading)
        assertEquals("Error de red. Verificá tu conexión e intentá de nuevo.", state.error)
    }

    @Test
    fun `SendMessage emits SessionExpired effect on 404`() = runTest(testDispatcher) {
        val repo = fakeRepo(
            result = ChatSendResult.Failure(ChatFailure.SessionExpired)
        )
        val viewModel = ChatViewModel(chatRepository = repo)

        viewModel.onEvent(ChatEvent.InputChanged("Test"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("La sesión ha expirado, iniciá sesión de nuevo", state.error)

        val effect = viewModel.effects.first()
        assertIs<ChatEffect.SessionExpired>(effect)
    }

    @Test
    fun `SendMessage sets server error text on server failure`() = runTest(testDispatcher) {
        val repo = fakeRepo(
            result = ChatSendResult.Failure(ChatFailure.Server)
        )
        val viewModel = ChatViewModel(chatRepository = repo)

        viewModel.onEvent(ChatEvent.InputChanged("Test"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Error del servidor. Intentá de nuevo en unos momentos.", state.error)
    }

    // ========== DismissError Tests ==========

    @Test
    fun `DismissError clears error from state`() = runTest(testDispatcher) {
        val repo = fakeRepo(
            result = ChatSendResult.Failure(ChatFailure.Network(Exception("fail")))
        )
        val viewModel = ChatViewModel(chatRepository = repo)

        viewModel.onEvent(ChatEvent.InputChanged("Test"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.error != null)

        viewModel.onEvent(ChatEvent.DismissError)

        assertEquals(null, viewModel.uiState.value.error)
    }

    // ========== ToggleAttachmentMenu Event Tests ==========

    @Test
    fun `ToggleAttachmentMenu false hides attachment menu`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(false))

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    @Test
    fun `ToggleAttachmentMenu true shows attachment menu`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))

        assertEquals(true, viewModel.uiState.value.showAttachmentMenu)
    }

    @Test
    fun `ToggleAttachmentMenu toggles state correctly`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))
        assertEquals(true, viewModel.uiState.value.showAttachmentMenu)

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(false))
        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    // ========== VoiceState Transition Tests ==========

    @Test
    fun `StartVoiceInput transitions to Listening state`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)

        val voiceState = viewModel.uiState.value.voiceState
        assertIs<VoiceState.Listening>(voiceState)
    }

    @Test
    fun `DismissVoice returns to Idle state`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
    }

    // ========== Phase 3: Image & Attachment Handling Tests ==========

    @Test
    fun `ImageSelected with valid URI appends Image attachment`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))

        val state = viewModel.uiState.value
        assertEquals(1, state.attachments.size)
        val attachment = state.attachments[0]
        assertIs<ChatAttachment.Image>(attachment)
        assertEquals("content://media/images/photo_001.jpg", attachment.uri)
    }

    @Test
    fun `ImageSelected with null URI does not append attachment`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected(null))

        assertEquals(emptyList(), viewModel.uiState.value.attachments)
    }

    @Test
    fun `ImageSelected accumulates multiple image attachments`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_002.jpg"))

        val state = viewModel.uiState.value
        assertEquals(2, state.attachments.size)
        assertIs<ChatAttachment.Image>(state.attachments[0])
        assertIs<ChatAttachment.Image>(state.attachments[1])
    }

    @Test
    fun `SendMessage with pending image attachment includes image in message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.InputChanged("Look at this pest"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        val assistantMessage = state.messages[1]
        assertEquals("Look at this pest", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertEquals(1, userMessage.attachments.size)
        assertIs<ChatAttachment.Image>(userMessage.attachments[0])
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
    }

    @Test
    fun `SendMessage clears all pending attachments after send`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_002.jpg"))
        viewModel.onEvent(ChatEvent.InputChanged("Multiple photos"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList(), state.attachments)
        assertEquals(2, state.messages.size)
        assertEquals(2, state.messages[0].attachments.size)
    }

    @Test
    fun `SendMessage sends message with only image and no text`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        val assistantMessage = state.messages[1]
        assertEquals("", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertEquals(1, userMessage.attachments.size)
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
    }

    @Test
    fun `RequestCamera sets showAttachmentMenu to false`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))
        viewModel.onEvent(ChatEvent.RequestCamera)

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    @Test
    fun `RequestGallery sets showAttachmentMenu to false`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))
        viewModel.onEvent(ChatEvent.RequestGallery)

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    // ========== Phase 4: Voice State & Recording Tests ==========

    @Test
    fun `StopVoiceInput transitions to Processing then Idle`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        assertIs<VoiceState.Listening>(viewModel.uiState.value.voiceState)

        viewModel.onEvent(ChatEvent.StopVoiceInput)

        // Processing is a transient intermediate state (ephemeral, not observable after onEvent returns)
        // The observable end state is Idle
        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
    }

    @Test
    fun `StopVoiceInput creates audio message after processing`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        // After processing, should return to Idle with user + assistant messages
        assertIs<VoiceState.Idle>(state.voiceState)
        assertEquals(2, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
    }

    @Test
    fun `StopVoiceInput after text input creates message with audio and text`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("What should I do about this?"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        val assistantMessage = state.messages[1]
        assertEquals("What should I do about this?", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertEquals(1, userMessage.attachments.size)
        assertIs<ChatAttachment.Audio>(userMessage.attachments[0])
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
    }

    @Test
    fun `StartVoiceInput while text is pending preserves text in final message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("urgent question"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("urgent question", state.messages[0].text)
    }

    @Test
    fun `DismissVoice from Listening returns to Idle without creating message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
        assertEquals(emptyList(), viewModel.uiState.value.messages)
    }

    @Test
    fun `DismissVoice does not create a message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("some question"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        assertEquals(emptyList(), viewModel.uiState.value.messages)
        assertEquals("some question", viewModel.uiState.value.inputText)
    }

    // ========== Fakes ==========

    private class FakeChatRepository(
        var result: ChatSendResult,
    ) : ChatRepository {
        var lastText: String? = null
        var lastAttachments: List<ChatAttachment>? = null
        var lastMode: ChatMode? = null

        override suspend fun sendMessage(text: String, attachments: List<ChatAttachment>, mode: ChatMode): ChatSendResult {
            lastText = text
            lastAttachments = attachments
            lastMode = mode
            return result
        }
    }
}
