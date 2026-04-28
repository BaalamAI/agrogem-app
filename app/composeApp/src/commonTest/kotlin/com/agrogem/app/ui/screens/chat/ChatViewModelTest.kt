package com.agrogem.app.ui.screens.chat

import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaModelDownloader
import com.agrogem.app.data.GemmaResponse
import com.agrogem.app.data.SpeechRecognizer
import com.agrogem.app.data.SpeechSynthesizer
import com.agrogem.app.data.chat.domain.ChatFailure
import com.agrogem.app.data.chat.domain.ChatRepository
import com.agrogem.app.data.chat.domain.ChatSendResult
import com.agrogem.app.data.connectivity.ConnectivityMonitor
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.LocationDisplay
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.risk.domain.DiseaseRisk
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.risk.domain.RiskSeverity
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.domain.SoilProfile
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.weather.domain.CurrentWeather
import com.agrogem.app.data.weather.domain.WeatherRepository
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import kotlin.test.assertFalse

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
    fun `PlayAssistantMessage speaks final assistant text manually`() = runTest(testDispatcher) {
        val speechSynthesizer = FakeSpeechSynthesizer()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            speechSynthesizer = speechSynthesizer,
        )

        viewModel.onEvent(ChatEvent.InputChanged("¿Qué hago?"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val assistantMessage = viewModel.uiState.value.messages.last()
        viewModel.onEvent(ChatEvent.PlayAssistantMessage(assistantMessage.id))

        assertEquals(1, speechSynthesizer.speakCallCount)
        assertEquals("Respuesta del asistente", speechSynthesizer.lastSpokenText)
        assertEquals(assistantMessage.id, viewModel.uiState.value.speakingMessageId)
    }

    @Test
    fun `PlayAssistantMessage ignores non assistant empty or streaming text`() = runTest(testDispatcher) {
        val speechSynthesizer = FakeSpeechSynthesizer()
        val repo = fakeRepo(
            result = ChatSendResult.Success(
                conversationId = "conv-1",
                messages = listOf(
                    ChatMessage(
                        id = "assistant_streaming",
                        text = "En progreso",
                        sender = MessageSender.Assistant,
                        timestamp = 1000L,
                        isStreaming = true,
                    ),
                    ChatMessage(
                        id = "assistant_empty",
                        text = "   ",
                        sender = MessageSender.Assistant,
                        timestamp = 1001L,
                    ),
                ),
            ),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            speechSynthesizer = speechSynthesizer,
        )

        viewModel.onEvent(ChatEvent.InputChanged("hola"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val userMessage = viewModel.uiState.value.messages.first { it.sender == MessageSender.User }
        viewModel.onEvent(ChatEvent.PlayAssistantMessage(userMessage.id))
        viewModel.onEvent(ChatEvent.PlayAssistantMessage("assistant_streaming"))
        viewModel.onEvent(ChatEvent.PlayAssistantMessage("assistant_empty"))

        assertEquals(0, speechSynthesizer.speakCallCount)
        assertEquals(null, viewModel.uiState.value.speakingMessageId)
    }

    @Test
    fun `PlayAssistantMessage stops current on same tap and restarts for another message`() = runTest(testDispatcher) {
        val speechSynthesizer = FakeSpeechSynthesizer()
        val repo = fakeRepo(
            result = ChatSendResult.Success(
                conversationId = "conv-1",
                messages = listOf(
                    ChatMessage(id = "assistant_1", text = "Primero", sender = MessageSender.Assistant, timestamp = 1000L),
                    ChatMessage(id = "assistant_2", text = "Segundo", sender = MessageSender.Assistant, timestamp = 1001L),
                ),
            ),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            speechSynthesizer = speechSynthesizer,
        )

        viewModel.onEvent(ChatEvent.InputChanged("hola"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        viewModel.onEvent(ChatEvent.PlayAssistantMessage("assistant_1"))
        viewModel.onEvent(ChatEvent.PlayAssistantMessage("assistant_1"))
        viewModel.onEvent(ChatEvent.PlayAssistantMessage("assistant_2"))

        assertEquals(2, speechSynthesizer.speakCallCount)
        assertEquals(1, speechSynthesizer.stopCallCount)
        assertEquals("Segundo", speechSynthesizer.lastSpokenText)
        assertEquals("assistant_2", viewModel.uiState.value.speakingMessageId)
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
    fun `StopVoiceInput with no text avoids sending junk`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        assertIs<VoiceState.Idle>(state.voiceState)
        assertEquals(0, state.messages.size)
    }

    @Test
    fun `StopVoiceInput after text input creates message with audio and text`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("What should I do about this?"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        val assistantMessage = state.messages[1]
        assertEquals("What should I do about this?", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertEquals(1, userMessage.attachments.size)
        assertIs<ChatAttachment.Audio>(userMessage.attachments[0])
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
        assertEquals("Respuesta del asistente", assistantMessage.text)
    }

    @Test
    fun `StartVoiceInput while text is pending preserves text in final message`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.InputChanged("urgent question"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)
        advanceUntilIdle()

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

    // ========== Phase 5: Speech-to-Text Tests ==========

    @Test
    fun `StartVoiceInput with speech recognizer starts listening`() = runTest(testDispatcher) {
        val recognizer = FakeSpeechRecognizer()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            speechRecognizer = recognizer,
        )

        viewModel.onEvent(ChatEvent.StartVoiceInput)

        assertEquals(1, recognizer.startListeningCallCount)
        assertIs<VoiceState.Listening>(viewModel.uiState.value.voiceState)
    }

    @Test
    fun `Partial result from speech recognizer updates inputText`() = runTest(testDispatcher) {
        val recognizer = FakeSpeechRecognizer()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            speechRecognizer = recognizer,
        )

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        recognizer.simulatePartialResult("Hello")

        assertEquals("Hello", viewModel.uiState.value.inputText)

        recognizer.simulatePartialResult("Hello world")

        assertEquals("Hello world", viewModel.uiState.value.inputText)
    }

    @Test
    fun `Speech recognizer error sets voiceState to Error`() = runTest(testDispatcher) {
        val recognizer = FakeSpeechRecognizer()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            speechRecognizer = recognizer,
        )

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        recognizer.simulateError("Error de audio")

        val voiceState = viewModel.uiState.value.voiceState
        assertIs<VoiceState.Error>(voiceState)
        assertEquals("Error de audio", voiceState.message)
    }

    @Test
    fun `StopVoiceInput with speech recognizer sends transcribed message`() = runTest(testDispatcher) {
        val recognizer = FakeSpeechRecognizer()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            speechRecognizer = recognizer,
        )

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        recognizer.simulatePartialResult("What should I do?")
        viewModel.onEvent(ChatEvent.StopVoiceInput)
        advanceUntilIdle()

        assertEquals(1, recognizer.stopListeningCallCount)
        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("What should I do?", state.messages[0].text)
        assertEquals(MessageSender.User, state.messages[0].sender)
        assertEquals(1, state.messages[0].attachments.size)
        assertIs<ChatAttachment.Audio>(state.messages[0].attachments[0])
    }

    @Test
    fun `DismissVoice with speech recognizer cancels listening`() = runTest(testDispatcher) {
        val recognizer = FakeSpeechRecognizer()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            speechRecognizer = recognizer,
        )

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        // StartVoiceInput calls cancel() defensively before startListening(),
        // so cancel is invoked twice: once on start and once on dismiss.
        assertEquals(2, recognizer.cancelCallCount)
        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
    }

    @Test
    fun `VoicePermissionDenied sets error state`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.VoicePermissionDenied)

        assertEquals("Se requiere permiso de micrófono para usar la voz", viewModel.uiState.value.error)
        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
    }

    @Test
    fun `StartVoiceInput without speech recognizer still transitions to Listening`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository = fakeRepo())

        viewModel.onEvent(ChatEvent.StartVoiceInput)

        assertIs<VoiceState.Listening>(viewModel.uiState.value.voiceState)
    }

    @Test
    fun `StopVoiceInput with empty transcript returns to Idle without sending`() = runTest(testDispatcher) {
        val recognizer = FakeSpeechRecognizer()
        val viewModel = ChatViewModel(
            chatRepository = fakeRepo(),
            speechRecognizer = recognizer,
        )

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        assertEquals(0, viewModel.uiState.value.messages.size)
        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
    }

    // ========== Phase 2: Gemma-first contextual chat tests ==========

    @Test
    fun `AnalysisSeeded mode sends via Gemma when model is downloaded and initialized`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("What should I do?"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        assertTrue(gemma.lastSystemPrompt?.contains("Roya") == true, "System prompt should include diagnosis context")
        assertTrue(gemma.lastSystemPrompt?.contains("Aplicar fungicida") == true, "System prompt should include treatment steps")
        assertEquals("What should I do?", gemma.lastUserPrompt)

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size, "Seed + user + assistant")
        val assistantMessage = state.messages[2]
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
        assertEquals("Gemma response", assistantMessage.text)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `Blank mode sends via Gemma when model is downloaded and initialized`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val viewModel = ChatViewModel(
            chatRepository = repo,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Hello"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        assertEquals("Hello", gemma.lastUserPrompt)
        assertTrue(gemma.lastSystemPrompt?.contains("asistente agronómico experto") == true, "System prompt should be general agricultural assistant")
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    // ========== Blank-mode environment context enrichment tests ==========

    @Test
    fun `Blank mode enriches Gemma prompt with weather and soil when location and repos succeed`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val weather = CurrentWeather(
            temperatureCelsius = "24°C",
            humidity = "65%",
            cloudCover = "30%",
            uvIndex = "7",
            description = "Parcialmente nublado",
            locationName = "Zacapa",
            dateLabel = "Hoy",
        )
        val soil = SoilProfile(
            lat = 14.9726,
            lon = -89.5301,
            dominantTexture = "Arcillosa",
            domainHorizons = emptyList(),
            interpretation = "Apto para café con drenaje moderado",
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            weatherRepository = FakeWeatherRepository(Result.success(weather)),
            soilRepository = FakeSoilRepository(Result.success(soil)),
            connectivityMonitor = FakeConnectivityMonitor(online = true),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Hello"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("asistente agronómico experto") == true, "General prompt should be preserved")
        assertTrue(prompt?.contains("Contexto del campo") == true, "Prompt should include environment context header")
        assertTrue(prompt?.contains("Zacapa, Guatemala") == true, "Prompt should include location")
        assertTrue(prompt?.contains("24°C") == true, "Prompt should include weather temperature")
        assertTrue(prompt?.contains("Arcillosa") == true, "Prompt should include soil texture")
        assertTrue(prompt?.contains("Apto para café con drenaje moderado") == true, "Prompt should include soil interpretation")
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `Blank mode continues with general prompt when no saved location exists`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val viewModel = ChatViewModel(
            chatRepository = repo,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(null),
            weatherRepository = FakeWeatherRepository(Result.failure(Exception("no location"))),
            soilRepository = FakeSoilRepository(Result.failure(Exception("no location"))),
            connectivityMonitor = FakeConnectivityMonitor(online = true),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Hello"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("asistente agronómico experto") == true, "General prompt should be preserved")
        assertTrue(prompt?.contains("Contexto del campo") != true, "Prompt should NOT include environment context when no location")
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `Blank mode continues with partial context when weather fetch fails`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val soil = SoilProfile(
            lat = 14.9726,
            lon = -89.5301,
            dominantTexture = "Franco",
            domainHorizons = emptyList(),
            interpretation = "",
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            weatherRepository = FakeWeatherRepository(Result.failure(Exception("network error"))),
            soilRepository = FakeSoilRepository(Result.success(soil)),
            connectivityMonitor = FakeConnectivityMonitor(online = true),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Hello"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Contexto del campo") == true, "Prompt should include environment context")
        assertTrue(prompt?.contains("Zacapa, Guatemala") == true, "Prompt should include location")
        assertTrue(prompt?.contains("Franco") == true, "Prompt should include soil texture")
        assertTrue(prompt?.contains("Clima") != true, "Prompt should NOT include weather when fetch fails")
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `Blank mode continues with partial context when soil fetch fails`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val weather = CurrentWeather(
            temperatureCelsius = "28°C",
            humidity = "70%",
            cloudCover = "10%",
            uvIndex = "9",
            description = "Soleado",
            locationName = "Zacapa",
            dateLabel = "Hoy",
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            weatherRepository = FakeWeatherRepository(Result.success(weather)),
            soilRepository = FakeSoilRepository(Result.failure(Exception("network error"))),
            connectivityMonitor = FakeConnectivityMonitor(online = true),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Hello"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Contexto del campo") == true, "Prompt should include environment context")
        assertTrue(prompt?.contains("28°C") == true, "Prompt should include weather temperature")
        assertTrue(prompt?.contains("Suelo") != true, "Prompt should NOT include soil section when fetch fails")
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `Blank mode skips enrichment when offline`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val weather = CurrentWeather(
            temperatureCelsius = "24°C",
            humidity = "65%",
            cloudCover = "30%",
            uvIndex = "7",
            description = "Parcialmente nublado",
            locationName = "Zacapa",
            dateLabel = "Hoy",
        )
        val soil = SoilProfile(
            lat = 14.9726,
            lon = -89.5301,
            dominantTexture = "Arcillosa",
            domainHorizons = emptyList(),
            interpretation = "Apto para café",
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            weatherRepository = FakeWeatherRepository(Result.success(weather)),
            soilRepository = FakeSoilRepository(Result.success(soil)),
            connectivityMonitor = FakeConnectivityMonitor(online = false),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Hello"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("asistente agronómico experto") == true, "General prompt should be preserved")
        assertTrue(prompt?.contains("Contexto del campo") != true, "Prompt should NOT include environment context when offline")
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `AnalysisSeeded path is unaffected by weather and soil repositories`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val weather = CurrentWeather(
            temperatureCelsius = "24°C",
            humidity = "65%",
            cloudCover = "30%",
            uvIndex = "7",
            description = "Parcialmente nublado",
            locationName = "Zacapa",
            dateLabel = "Hoy",
        )
        val soil = SoilProfile(
            lat = 14.9726,
            lon = -89.5301,
            dominantTexture = "Arcillosa",
            domainHorizons = emptyList(),
            interpretation = "Apto para café",
        )
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            weatherRepository = FakeWeatherRepository(Result.success(weather)),
            soilRepository = FakeSoilRepository(Result.success(soil)),
            connectivityMonitor = FakeConnectivityMonitor(online = true),
        )

        viewModel.onEvent(ChatEvent.InputChanged("What should I do?"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya") == true, "System prompt should include diagnosis context")
        assertTrue(prompt?.contains("Contexto del campo") != true, "AnalysisSeeded prompt should NOT include general environment context")
        assertEquals(3, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `AnalysisSeeded falls back to backend when model is not downloaded`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called when model not downloaded")
        assertEquals(0, gemma.streamCallCount, "Gemma stream should NOT be called")
        assertEquals(3, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `AnalysisSeeded falls back to backend when Gemma initialization fails`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = false, shouldThrowOnInit = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called when Gemma init fails")
        assertEquals(0, gemma.streamCallCount, "Gemma stream should NOT be called")
        assertEquals(3, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `AnalysisSeeded falls back to backend when Gemma stream throws`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true, shouldThrowOnStream = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called when Gemma stream throws")
        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size, "Placeholder should be removed and backend response added")
        assertEquals("Respuesta del asistente", state.messages[2].text)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `AnalysisSeeded uses streaming placeholders during Gemma inference`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(
            initialInitialized = true,
            responses = listOf(
                GemmaResponse(text = "Hello ", isDone = false),
                GemmaResponse(text = "world", isDone = false),
                GemmaResponse(text = "", isDone = true),
            ),
        )
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        val assistantMessage = state.messages[2]
        assertEquals("Hello world", assistantMessage.text)
        assertEquals(false, assistantMessage.isStreaming)
        assertEquals(false, state.isLoading)
    }

    // ========== Pest-risk context enrichment tests ==========

    @Test
    fun `AnalysisSeeded enriches Gemma prompt with pest risk when location and risk exist`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val pestRisks = listOf(
            DiseaseRisk(
                diseaseName = "spider_mite",
                displayName = "Ácaro arañero",
                score = 0.75,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado de ácaro arañero",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(Result.success(pestRisks)),
        )

        viewModel.onEvent(ChatEvent.InputChanged("What should I do?"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya") == true, "System prompt should include diagnosis context")
        assertTrue(prompt?.contains("Zacapa, Guatemala") == true, "System prompt should include location")
        assertTrue(prompt?.contains("Ácaro arañero") == true, "System prompt should include pest risk")
        assertTrue(prompt?.contains("Riesgo moderado de ácaro arañero") == true, "System prompt should include interpretation")
    }

    @Test
    fun `AnalysisSeeded continues normally when no saved location exists`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(null),
            riskRepository = FakeRiskRepository(Result.failure(Exception("no-op"))),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya") == true, "System prompt should still include diagnosis")
        assertTrue(prompt?.contains("Riesgos de plagas en la región") != true, "System prompt should NOT include pest risk section")
        assertEquals(3, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `AnalysisSeeded continues normally when pest risk fetch fails`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(Result.failure(Exception("network error"))),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya") == true, "System prompt should still include diagnosis")
        assertTrue(prompt?.contains("Riesgos de plagas en la región") != true, "System prompt should NOT include pest risk section after failure")
        assertEquals(3, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `AnalysisSeeded uses top non-optimal pest risk for prompt enrichment`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val pestRisks = listOf(
            DiseaseRisk(
                diseaseName = "spider_mite",
                displayName = "Ácaro arañero",
                score = 0.75,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado",
                factors = emptyList(),
            ),
            DiseaseRisk(
                diseaseName = "other_pest",
                displayName = "Otra plaga",
                score = 0.95,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo crítico",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(Result.success(pestRisks)),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Otra plaga") == true, "Should include critical risk")
        assertTrue(prompt?.contains("Riesgo crítico") == true, "Should include critical interpretation")
        assertTrue(prompt?.contains("Ácaro arañero") == true, "Should include attention risk")
    }

    @Test
    fun `AnalysisSeeded prefers matched pest risk when diagnosis matches a known pest`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val pestRisks = listOf(
            DiseaseRisk(
                diseaseName = "spider_mite",
                displayName = "Ácaro arañero",
                score = 0.75,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado",
                factors = emptyList(),
            ),
            DiseaseRisk(
                diseaseName = "other_pest",
                displayName = "Otra plaga",
                score = 0.95,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo crítico",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Ácaro rojo",
            confidence = 0.88f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Tetranychus urticae",
            diagnosisText = "Infestación detectada",
            treatmentSteps = listOf("Aplicar acaricida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(Result.success(pestRisks)),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Ácaro arañero") == true, "Should include matched pest risk")
        assertTrue(prompt?.contains("Riesgo moderado") == true, "Should include matched interpretation")
        assertTrue(prompt?.contains("Otra plaga") != true, "Should NOT include unmatched generic risks")
    }

    @Test
    fun `AnalysisSeeded falls back to generic summary when diagnosis does not match any known pest`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val pestRisks = listOf(
            DiseaseRisk(
                diseaseName = "spider_mite",
                displayName = "Ácaro arañero",
                score = 0.75,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado",
                factors = emptyList(),
            ),
            DiseaseRisk(
                diseaseName = "other_pest",
                displayName = "Otra plaga",
                score = 0.95,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo crítico",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Mosca blanca",
            confidence = 0.8f,
            severity = "Media",
            affectedArea = "Hojas",
            cause = "Bemisia tabaci",
            diagnosisText = "Presencia detectada",
            treatmentSteps = listOf("Aplicar insecticida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(Result.success(pestRisks)),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Ácaro arañero") == true, "Should include generic fallback risk")
        assertTrue(prompt?.contains("Otra plaga") == true, "Should include generic fallback critical risk")
        assertTrue(prompt?.contains("Riesgo crítico") == true, "Should include generic fallback interpretation")
    }

    @Test
    fun `AnalysisSeeded matches whitefly diagnosis to whitefly pest risk via aliases`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val pestRisks = listOf(
            DiseaseRisk(
                diseaseName = "whitefly",
                displayName = "Mosca blanca",
                score = 0.85,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo alto de mosca blanca",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Mosca blanca",
            confidence = 0.88f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Bemisia tabaci",
            diagnosisText = "Infestación detectada",
            treatmentSteps = listOf("Aplicar insecticida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(Result.success(pestRisks)),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Mosca blanca") == true, "Should include matched whitefly risk")
        assertTrue(prompt?.contains("Riesgo alto de mosca blanca") == true, "Should include matched interpretation")
    }

    @Test
    fun `AnalysisSeeded matches coffee berry borer diagnosis to broca pest risk via aliases`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val pestRisks = listOf(
            DiseaseRisk(
                diseaseName = "coffee_berry_borer",
                displayName = "Broca del café",
                score = 0.92,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo crítico de broca",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Broca",
            confidence = 0.95f,
            severity = "Crítica",
            affectedArea = "Fruto",
            cause = "Hypothenemus hampei",
            diagnosisText = "Infestación severa detectada",
            treatmentSteps = listOf("Recolectar bayas caídas"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(Result.success(pestRisks)),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Broca del café") == true, "Should include matched coffee berry borer risk")
        assertTrue(prompt?.contains("Riesgo crítico de broca") == true, "Should include matched interpretation")
    }

    // ========== Disease-risk context enrichment tests ==========

    @Test
    fun `AnalysisSeeded enriches Gemma prompt with disease risk when location and risk exist`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val diseaseRisks = listOf(
            DiseaseRisk(
                diseaseName = "coffee_rust",
                displayName = "Roya del café",
                score = 0.82,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado de roya",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(
                pestResult = Result.success(emptyList()),
                diseaseResult = Result.success(diseaseRisks),
            ),
        )

        viewModel.onEvent(ChatEvent.InputChanged("What should I do?"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya del café") == true, "System prompt should include disease risk")
        assertTrue(prompt?.contains("Riesgo moderado de roya") == true, "System prompt should include disease interpretation")
        assertTrue(prompt?.contains("Riesgos de enfermedades en la región") == true, "System prompt should include disease risk section")
    }

    @Test
    fun `AnalysisSeeded continues normally when disease risk fetch fails`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(
                pestResult = Result.success(emptyList()),
                diseaseResult = Result.failure(Exception("network error")),
            ),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(0, repo.sendMessageCallCount, "Backend should NOT be called when Gemma is available")
        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya") == true, "System prompt should still include diagnosis")
        assertTrue(prompt?.contains("Riesgos de enfermedades en la región") != true, "System prompt should NOT include disease risk section after failure")
        assertEquals(3, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `AnalysisSeeded enriches prompt with both pest and disease risks when both available`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val pestRisks = listOf(
            DiseaseRisk(
                diseaseName = "whitefly",
                displayName = "Mosca blanca",
                score = 0.85,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo alto de mosca blanca",
                factors = emptyList(),
            ),
        )
        val diseaseRisks = listOf(
            DiseaseRisk(
                diseaseName = "late_blight",
                displayName = "Tizón tardío",
                score = 0.78,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado de tizón",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Mosca blanca",
            confidence = 0.88f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Bemisia tabaci",
            diagnosisText = "Infestación detectada",
            treatmentSteps = listOf("Aplicar insecticida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(
                pestResult = Result.success(pestRisks),
                diseaseResult = Result.success(diseaseRisks),
            ),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Mosca blanca") == true, "Should include pest risk")
        assertTrue(prompt?.contains("Riesgos de plagas en la región") == true, "Should include pest risk section")
        assertTrue(prompt?.contains("Tizón tardío") == true, "Should include disease risk")
        assertTrue(prompt?.contains("Riesgos de enfermedades en la región") == true, "Should include disease risk section")
    }

    @Test
    fun `AnalysisSeeded matches roya diagnosis to coffee rust disease risk via aliases`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val diseaseRisks = listOf(
            DiseaseRisk(
                diseaseName = "coffee_rust",
                displayName = "Roya del café",
                score = 0.82,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado de roya",
                factors = emptyList(),
            ),
            DiseaseRisk(
                diseaseName = "late_blight",
                displayName = "Tizón tardío",
                score = 0.95,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo crítico de tizón",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hemileia vastatrix",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(
                pestResult = Result.success(emptyList()),
                diseaseResult = Result.success(diseaseRisks),
            ),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya del café") == true, "Should include matched coffee rust risk")
        assertTrue(prompt?.contains("Riesgo moderado de roya") == true, "Should include matched interpretation")
        assertTrue(prompt?.contains("Tizón tardío") != true, "Should NOT include unmatched generic disease risk")
    }

    @Test
    fun `AnalysisSeeded falls back to generic disease risks when diagnosis does not match any known disease`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val diseaseRisks = listOf(
            DiseaseRisk(
                diseaseName = "coffee_rust",
                displayName = "Roya del café",
                score = 0.75,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado",
                factors = emptyList(),
            ),
            DiseaseRisk(
                diseaseName = "late_blight",
                displayName = "Tizón tardío",
                score = 0.95,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo crítico",
                factors = emptyList(),
            ),
        )
        val diagnosis = DiagnosisResult(
            pestName = "Mosca blanca",
            confidence = 0.8f,
            severity = "Media",
            affectedArea = "Hojas",
            cause = "Bemisia tabaci",
            diagnosisText = "Presencia detectada",
            treatmentSteps = listOf("Aplicar insecticida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
            geolocationRepository = FakeGeolocationRepository(location),
            riskRepository = FakeRiskRepository(
                pestResult = Result.success(emptyList()),
                diseaseResult = Result.success(diseaseRisks),
            ),
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        val prompt = gemma.lastSystemPrompt
        assertTrue(prompt?.contains("Roya del café") == true, "Should include generic fallback disease risk")
        assertTrue(prompt?.contains("Tizón tardío") == true, "Should include generic fallback critical disease risk")
        assertTrue(prompt?.contains("Riesgo crítico") == true, "Should include generic fallback interpretation")
    }

    // ========== Backend fallback context preservation tests ==========

    @Test
    fun `AnalysisSeeded fallback to backend includes diagnosis context in text`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called")
        val sentText = repo.lastText
        assertTrue(sentText?.contains("[Contexto del análisis previo]") == true, "Should include context header")
        assertTrue(sentText?.contains("Plaga/Enfermedad: Roya") == true, "Should include pest name")
        assertTrue(sentText?.contains("Severidad: Alta") == true, "Should include severity")
        assertTrue(sentText?.contains("Área afectada: Hojas") == true, "Should include affected area")
        assertTrue(sentText?.contains("Causa: Hongo") == true, "Should include cause")
        assertTrue(sentText?.contains("Diagnóstico: Infección detectada") == true, "Should include diagnosis text")
        assertTrue(sentText?.contains("Tratamiento:") == true, "Should include treatment header")
        assertTrue(sentText?.contains("Aplicar fungicida") == true, "Should include treatment step")
        assertTrue(sentText?.contains("Help") == true, "Should include original user text")
    }

    @Test
    fun `Blank fallback to backend sends text unchanged`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val repo = fakeRepo()
        val viewModel = ChatViewModel(
            chatRepository = repo,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Hello"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called")
        assertEquals("Hello", repo.lastText, "Text should be unchanged for Blank mode")
    }

    @Test
    fun `AnalysisSeeded fallback after Gemma init failure includes diagnosis context`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = false, shouldThrowOnInit = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called after init failure")
        val sentText = repo.lastText
        assertTrue(sentText?.contains("Plaga/Enfermedad: Roya") == true, "Should include diagnosis context")
        assertTrue(sentText?.contains("Help") == true, "Should include original user text")
    }

    @Test
    fun `AnalysisSeeded fallback after Gemma stream error includes diagnosis context`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true, shouldThrowOnStream = true)
        val downloader = FakeGemmaModelDownloader(downloaded = true)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called after stream error")
        val sentText = repo.lastText
        assertTrue(sentText?.contains("Plaga/Enfermedad: Roya") == true, "Should include diagnosis context")
        assertTrue(sentText?.contains("Help") == true, "Should include original user text")
    }

    @Test
    fun `AnalysisSeeded fallback omits treatment section when treatment steps are empty`() = runTest(testDispatcher) {
        val gemma = FakeGemmaManager(initialInitialized = true)
        val downloader = FakeGemmaModelDownloader(downloaded = false)
        val repo = fakeRepo()
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección detectada",
            treatmentSteps = emptyList(),
        )
        val viewModel = ChatViewModel(
            chatRepository = repo,
            analysisId = "analysis_123",
            diagnosis = diagnosis,
            gemmaManager = gemma,
            gemmaModelDownloader = downloader,
        )

        viewModel.onEvent(ChatEvent.InputChanged("Help"))
        viewModel.onEvent(ChatEvent.SendMessage)
        advanceUntilIdle()

        assertEquals(1, repo.sendMessageCallCount, "Backend should be called")
        val sentText = repo.lastText
        assertTrue(sentText?.contains("Plaga/Enfermedad: Roya") == true, "Should include diagnosis context")
        assertTrue(sentText?.contains("Tratamiento:") != true, "Should NOT include treatment header when empty")
        assertTrue(sentText?.contains("Help") == true, "Should include original user text")
    }

    // ========== Fakes ==========

    private class FakeChatRepository(
        var result: ChatSendResult,
    ) : ChatRepository {
        var lastText: String? = null
        var lastAttachments: List<ChatAttachment>? = null
        var lastMode: ChatMode? = null
        var sendMessageCallCount = 0

        override suspend fun sendMessage(text: String, attachments: List<ChatAttachment>, mode: ChatMode): ChatSendResult {
            lastText = text
            lastAttachments = attachments
            lastMode = mode
            sendMessageCallCount++
            return result
        }
    }

    private class FakeGemmaManager(
        initialInitialized: Boolean = false,
        var shouldThrowOnInit: Boolean = false,
        var shouldThrowOnStream: Boolean = false,
        var responses: List<GemmaResponse> = listOf(GemmaResponse(text = "Gemma response", isDone = true)),
    ) : GemmaManager {
        private val _isInitialized = MutableStateFlow(initialInitialized)
        override val isInitialized: Flow<Boolean> = _isInitialized

        var lastSystemPrompt: String? = null
        var lastUserPrompt: String? = null
        var streamCallCount = 0

        override suspend fun initialize(modelPath: String) {
            if (shouldThrowOnInit) throw Exception("Init failed")
            _isInitialized.value = true
        }

        override suspend fun sendMessage(
            systemPrompt: String,
            userPrompt: String,
            images: List<String>,
            audioPath: String?,
            temperature: Float,
        ): String {
            lastSystemPrompt = systemPrompt
            lastUserPrompt = userPrompt
            return responses.joinToString("") { it.text }
        }

        override fun sendMessageStream(
            systemPrompt: String,
            userPrompt: String,
            images: List<String>,
            audioPath: String?,
            temperature: Float,
        ): Flow<GemmaResponse> {
            if (shouldThrowOnStream) throw Exception("Stream failed")
            streamCallCount++
            lastSystemPrompt = systemPrompt
            lastUserPrompt = userPrompt
            return responses.asFlow()
        }

        override fun close() {}
    }

    private class FakeGemmaModelDownloader(
        var downloaded: Boolean = false,
        private val path: String = "/fake/model.litertlm",
    ) : GemmaModelDownloader {
        override suspend fun downloadModel(url: String): Result<String> = Result.success(path)
        override fun isModelDownloaded(): Boolean = downloaded
        override fun getModelPath(): String = path
    }

    private class FakeGeolocationRepository(
        private val resolved: ResolvedLocation?
    ) : GeolocationRepository {
        override suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation> =
            Result.failure(UnsupportedOperationException())

        override suspend fun saveResolvedLocation(location: ResolvedLocation) {}

        override fun observeResolvedLocation(): Flow<ResolvedLocation?> = flowOf(resolved)
    }

    private class FakeRiskRepository(
        private val pestResult: Result<List<DiseaseRisk>>,
        private val diseaseResult: Result<List<DiseaseRisk>> = Result.failure(UnsupportedOperationException("Not used in chat tests")),
    ) : RiskRepository {
        override suspend fun getDiseaseRisks(latLng: LatLng?): Result<List<DiseaseRisk>> = diseaseResult

        override suspend fun getPestRisks(latLng: LatLng?): Result<List<DiseaseRisk>> = pestResult
    }

    private class FakeWeatherRepository(
        private val result: Result<CurrentWeather>,
    ) : WeatherRepository {
        override suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather> = result
    }

    private class FakeSoilRepository(
        private val result: Result<SoilProfile>,
    ) : SoilRepository {
        override suspend fun getSoil(latLng: LatLng): Result<SoilProfile> = result
    }

    private class FakeConnectivityMonitor(
        private val online: Boolean,
    ) : ConnectivityMonitor {
        override fun isOnline(): Boolean = online
    }

    private class FakeSpeechRecognizer : SpeechRecognizer {
        var startListeningCallCount = 0
        var stopListeningCallCount = 0
        var cancelCallCount = 0
        private var onPartialResult: ((String) -> Unit)? = null
        private var onFinalResult: ((String) -> Unit)? = null
        private var onError: ((String) -> Unit)? = null

        override fun startListening(
            onPartialResult: (String) -> Unit,
            onFinalResult: (String) -> Unit,
            onError: (String) -> Unit,
        ) {
            startListeningCallCount++
            this.onPartialResult = onPartialResult
            this.onFinalResult = onFinalResult
            this.onError = onError
        }

        override fun stopListening() {
            stopListeningCallCount++
        }

        override fun cancel() {
            cancelCallCount++
        }

        fun simulatePartialResult(text: String) {
            onPartialResult?.invoke(text)
        }

        fun simulateFinalResult(text: String) {
            onFinalResult?.invoke(text)
        }

        fun simulateError(message: String) {
            onError?.invoke(message)
        }
    }

    private class FakeSpeechSynthesizer : SpeechSynthesizer {
        var speakCallCount = 0
        var stopCallCount = 0
        var lastSpokenText: String? = null
        var shouldFailSpeak = false

        override fun speak(text: String): Boolean {
            if (shouldFailSpeak) return false
            speakCallCount++
            lastSpokenText = text
            return true
        }

        override fun stop() {
            stopCallCount++
        }
    }
}
