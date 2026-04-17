package com.agrogem.app.ui.screens.chat

import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatViewModelTest {

    // ========== Initialization Tests ==========

    @Test
    fun `default initialization starts in blank mode with empty messages`() {
        val viewModel = ChatViewModel()

        val state = viewModel.uiState.value
        assertIs<ChatMode.Blank>(state.mode)
        assertEquals(emptyList(), state.messages)
        assertEquals("", state.inputText)
        assertEquals(emptyList(), state.attachments)
        assertEquals(VoiceState.Idle, state.voiceState)
        assertEquals(false, state.showAttachmentMenu)
    }

    @Test
    fun `seeded initialization with analysisId starts in analysis seeded mode with first message`() {
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
    fun `seeded initialization without diagnosis uses mock diagnosis`() {
        val viewModel = ChatViewModel(
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
    fun `InputChanged updates inputText in state`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("Hello world"))

        assertEquals("Hello world", viewModel.uiState.value.inputText)
    }

    @Test
    fun `InputChanged with empty text clears input`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("Some text"))
        viewModel.onEvent(ChatEvent.InputChanged(""))

        assertEquals("", viewModel.uiState.value.inputText)
    }

    // ========== SendMessage Event Tests ==========

    @Test
    fun `SendMessage with text adds user and assistant messages and clears input`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("What is this pest?"))
        viewModel.onEvent(ChatEvent.SendMessage)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        val assistantMessage = state.messages[1]
        assertEquals("What is this pest?", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
        assertTrue(assistantMessage.text.isNotBlank())
        assertEquals("", state.inputText)
        assertEquals(emptyList(), state.attachments)
    }

    @Test
    fun `SendMessage does nothing when input is empty`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.SendMessage)

        assertEquals(emptyList(), viewModel.uiState.value.messages)
    }

    @Test
    fun `SendMessage adds multiple messages in order`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("First message"))
        viewModel.onEvent(ChatEvent.SendMessage)
        viewModel.onEvent(ChatEvent.InputChanged("Second message"))
        viewModel.onEvent(ChatEvent.SendMessage)

        val state = viewModel.uiState.value
        assertEquals(4, state.messages.size)
        assertEquals("First message", state.messages[0].text)
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
        assertEquals("Second message", state.messages[2].text)
        assertEquals(MessageSender.Assistant, state.messages[3].sender)
    }

    @Test
    fun `SendMessage clears pending attachments`() {
        val viewModel = ChatViewModel()

        // Simulate pending attachments (would come from ImageSelected in full flow)
        // For now, test that send clears any existing attachments
        viewModel.onEvent(ChatEvent.InputChanged("Message with photo"))
        viewModel.onEvent(ChatEvent.SendMessage)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(emptyList(), state.attachments)
    }

    // ========== ToggleAttachmentMenu Event Tests ==========

    @Test
    fun `ToggleAttachmentMenu false hides attachment menu`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(false))

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    @Test
    fun `ToggleAttachmentMenu true shows attachment menu`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))

        assertEquals(true, viewModel.uiState.value.showAttachmentMenu)
    }

    @Test
    fun `ToggleAttachmentMenu toggles state correctly`() {
        val viewModel = ChatViewModel()

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))
        assertEquals(true, viewModel.uiState.value.showAttachmentMenu)

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(false))
        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    // ========== VoiceState Transition Tests ==========

    @Test
    fun `StartVoiceInput transitions to Listening state`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.StartVoiceInput)

        val voiceState = viewModel.uiState.value.voiceState
        assertIs<VoiceState.Listening>(voiceState)
    }

    @Test
    fun `DismissVoice returns to Idle state`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
    }

    // ========== Phase 3: Image & Attachment Handling Tests ==========

    @Test
    fun `ImageSelected with valid URI appends Image attachment`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))

        val state = viewModel.uiState.value
        assertEquals(1, state.attachments.size)
        val attachment = state.attachments[0]
        assertIs<ChatAttachment.Image>(attachment)
        assertEquals("content://media/images/photo_001.jpg", attachment.uri)
    }

    @Test
    fun `ImageSelected with null URI does not append attachment`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected(null))

        assertEquals(emptyList(), viewModel.uiState.value.attachments)
    }

    @Test
    fun `ImageSelected accumulates multiple image attachments`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_002.jpg"))

        val state = viewModel.uiState.value
        assertEquals(2, state.attachments.size)
        assertIs<ChatAttachment.Image>(state.attachments[0])
        assertIs<ChatAttachment.Image>(state.attachments[1])
    }

    @Test
    fun `SendMessage with pending image attachment includes image in message`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.InputChanged("Look at this pest"))
        viewModel.onEvent(ChatEvent.SendMessage)

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
    fun `SendMessage clears all pending attachments after send`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_002.jpg"))
        viewModel.onEvent(ChatEvent.InputChanged("Multiple photos"))
        viewModel.onEvent(ChatEvent.SendMessage)

        val state = viewModel.uiState.value
        assertEquals(emptyList(), state.attachments)
        assertEquals(2, state.messages.size)
        assertEquals(2, state.messages[0].attachments.size)
    }

    @Test
    fun `SendMessage sends message with only image and no text`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/images/photo_001.jpg"))
        viewModel.onEvent(ChatEvent.SendMessage)

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
    fun `RequestCamera sets showAttachmentMenu to false`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))
        viewModel.onEvent(ChatEvent.RequestCamera)

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    @Test
    fun `RequestGallery sets showAttachmentMenu to false`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true))
        viewModel.onEvent(ChatEvent.RequestGallery)

        assertEquals(false, viewModel.uiState.value.showAttachmentMenu)
    }

    // ========== Phase 4: Voice State & Recording Tests ==========

    @Test
    fun `StopVoiceInput transitions to Processing then Idle`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        assertIs<VoiceState.Listening>(viewModel.uiState.value.voiceState)

        viewModel.onEvent(ChatEvent.StopVoiceInput)

        // Processing is a transient intermediate state (ephemeral, not observable after onEvent returns)
        // The observable end state is Idle
        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
    }

    @Test
    fun `StopVoiceInput creates audio message after processing`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        // After processing, should return to Idle with user + assistant messages
        assertIs<VoiceState.Idle>(state.voiceState)
        assertEquals(2, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
    }

    @Test
    fun `StopVoiceInput after text input creates message with audio and text`() {
        val viewModel = ChatViewModel()

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
    fun `StartVoiceInput while text is pending preserves text in final message`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("urgent question"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("urgent question", state.messages[0].text)
    }

    @Test
    fun `DismissVoice from Listening returns to Idle without creating message`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        assertEquals(VoiceState.Idle, viewModel.uiState.value.voiceState)
        assertEquals(emptyList(), viewModel.uiState.value.messages)
    }

    @Test
    fun `DismissVoice does not create a message`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("some question"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        assertEquals(emptyList(), viewModel.uiState.value.messages)
        assertEquals("some question", viewModel.uiState.value.inputText)
    }
}
