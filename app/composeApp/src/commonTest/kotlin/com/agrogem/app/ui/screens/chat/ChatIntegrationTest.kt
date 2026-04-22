package com.agrogem.app.ui.screens.chat

import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Phase 6 — Integration-level tests for chat flows.
 * These tests verify end-to-end state continuity through the ChatViewModel API
 * as would be experienced when ChatScreen, ChatConfirm, and VoiceReady share
 * the same ChatViewModel instance (Phase 5 architecture requirement).
 *
 * 6.1: Chat send flow — text message appears in history
 * 6.2: Attachment send flow — message with image appears, attachments cleared
 * 6.3: Voice continuity — voice message appears after navigating back
 * 6.4: Seeded initialization — chat seeded with analysis context
 */
class ChatIntegrationTest {

    // ========== Phase 6.1: Chat Send Flow ==========

    @Test
    fun `6_1_text_message_appears_in_history_after_send`() {
        // Simulates: ChatScreen launches, user enters text, sends → message in history
        val viewModel = ChatViewModel()
        val state0 = viewModel.uiState.value
        assertTrue(state0.messages.isEmpty(), "Initially no messages")

        // User types a message
        viewModel.onEvent(ChatEvent.InputChanged("How do I treat this?"))
        assertEquals("How do I treat this?", viewModel.uiState.value.inputText)

        // User sends
        viewModel.onEvent(ChatEvent.SendMessage)

        val state1 = viewModel.uiState.value
        assertEquals(2, state1.messages.size, "User and assistant messages after send")
        assertEquals("How do I treat this?", state1.messages[0].text)
        assertEquals(MessageSender.User, state1.messages[0].sender)
        assertEquals(MessageSender.Assistant, state1.messages[1].sender)
        assertEquals("", state1.inputText, "Input cleared after send")
    }

    @Test
    fun `6_1_multiple_messages_appear_in_order`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("First"))
        viewModel.onEvent(ChatEvent.SendMessage)
        viewModel.onEvent(ChatEvent.InputChanged("Second"))
        viewModel.onEvent(ChatEvent.SendMessage)

        val state = viewModel.uiState.value
        assertEquals(4, state.messages.size)
        assertEquals("First", state.messages[0].text)
        assertEquals("Second", state.messages[2].text)
    }

    @Test
    fun `6_1_send_with_empty_input_does_nothing`() {
        val viewModel = ChatViewModel()
        viewModel.onEvent(ChatEvent.SendMessage)
        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    // ========== Phase 6.2: Attachment Send Flow ==========

    @Test
    fun `6_2_send_with_image_creates_message_with_image_attachment`() {
        // Simulates: ChatScreen with pending attachment, send → message shows text + image
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo1.jpg"))
        viewModel.onEvent(ChatEvent.InputChanged("Look at this pest"))
        viewModel.onEvent(ChatEvent.SendMessage)

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
    fun `6_2_send_with_multiple_images_preserves_all_attachments`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo1.jpg"))
        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo2.jpg"))
        viewModel.onEvent(ChatEvent.SendMessage)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(2, state.messages[0].attachments.size)
        assertEquals(emptyList(), state.attachments)
    }

    @Test
    fun `6_2_image_only_message_sends_without_text`() {
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.ImageSelected("content://media/photo1.jpg"))
        viewModel.onEvent(ChatEvent.SendMessage)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("", state.messages[0].text)
        assertEquals(1, state.messages[0].attachments.size)
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
    }

    // ========== Phase 6.3: Voice Continuity ==========

    @Test
    fun `6_3_stop_voice_creates_audio_message_in_same_viewmodel`() {
        // This test verifies the core continuity requirement:
        // When VoiceReady and Chat share the SAME ChatViewModel instance,
        // a voice message recorded in VoiceReady appears in Chat's message history.
        val viewModel = ChatViewModel()

        // Simulate: microphone tap → VoiceReady screen (StartVoiceInput)
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        assertIs<VoiceState.Listening>(viewModel.uiState.value.voiceState)

        // Simulate: stop recording in VoiceReady → VoiceState.Processing → Idle + message created
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        assertEquals(VoiceState.Idle, state.voiceState, "Returns to Idle after stop")
        assertEquals(2, state.messages.size, "Audio + assistant messages created in same ViewModel")
        assertEquals(MessageSender.User, state.messages[0].sender)
        assertIs<ChatAttachment.Audio>(state.messages[0].attachments[0])
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
    }

    @Test
    fun `6_3_voice_with_pending_text_includes_text_in_message`() {
        // Simulates: user types text in Chat, then navigates to VoiceReady, records, returns
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("urgent question"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("urgent question", state.messages[0].text)
        assertEquals(1, state.messages[0].attachments.size)
        assertIs<ChatAttachment.Audio>(state.messages[0].attachments[0])
        assertEquals(MessageSender.Assistant, state.messages[1].sender)
    }

    @Test
    fun `6_3_dismiss_voice_does_not_create_message`() {
        // Simulates: user opens VoiceReady, taps dismiss → no message created
        val viewModel = ChatViewModel()

        viewModel.onEvent(ChatEvent.InputChanged("typed text"))
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.DismissVoice)

        val state = viewModel.uiState.value
        assertEquals(VoiceState.Idle, state.voiceState)
        assertEquals(0, state.messages.size, "No message after dismiss")
        assertEquals("typed text", state.inputText, "Typed text preserved after dismiss")
    }

    @Test
    fun `6_3_voice_twice_creates_two_messages`() {
        val viewModel = ChatViewModel()

        // First voice recording
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        // Second voice recording
        viewModel.onEvent(ChatEvent.StartVoiceInput)
        viewModel.onEvent(ChatEvent.StopVoiceInput)

        assertEquals(4, viewModel.uiState.value.messages.size)
    }

    // ========== Phase 6.4: Seeded Initialization ==========

    @Test
    fun `6_4_chat_from_analysis_initializes_in_seeded_mode_with_diagnosis`() {
        // Simulates: analyze pest, tap "chat" → ChatScreen initializes in AnalysisSeeded mode
        val diagnosis = DiagnosisResult(
            pestName = "Roya naranja",
            confidence = 0.87f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo Puccinia",
            diagnosisText = "Roya naranja detectada en 30% del follaje.",
            treatmentSteps = listOf("Aplicar fungicida sistémico"),
        )

        // This is how AppShell creates ChatViewModel when navigating from analysis
        val viewModel = ChatViewModel(
            analysisId = "analysis_session_001",
            diagnosis = diagnosis,
        )

        val state = viewModel.uiState.value
        assertIs<ChatMode.AnalysisSeeded>(state.mode)
        assertEquals("analysis_session_001", state.mode.analysisId)
        assertEquals("Roya naranja", state.mode.diagnosis.pestName)

        // First message is from assistant with diagnosis context
        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Roya naranja"))
    }

    @Test
    fun `6_4_seeded_chat_allows_follow_up_messages`() {
        val viewModel = ChatViewModel(
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

        // User can send follow-up messages
        viewModel.onEvent(ChatEvent.InputChanged("What should I do next?"))
        viewModel.onEvent(ChatEvent.SendMessage)

        assertEquals(3, viewModel.uiState.value.messages.size)
        assertEquals("What should I do next?", viewModel.uiState.value.messages[1].text)
        assertEquals(MessageSender.User, viewModel.uiState.value.messages[1].sender)
        assertEquals(MessageSender.Assistant, viewModel.uiState.value.messages[2].sender)
    }

    @Test
    fun `6_4_blank_chat_has_no_initial_message`() {
        // Simulates: user opens chat directly (no analysis context)
        val viewModel = ChatViewModel()

        val state = viewModel.uiState.value
        assertIs<ChatMode.Blank>(state.mode)
        assertEquals(0, state.messages.size, "Blank chat starts with no messages")
    }

    // ========== Phase 6.5: Runtime Post-Analysis Chat Handoff ==========

    /**
     * Verifies that a blank ChatViewModel can be seeded at runtime via seedFromAnalysis,
     * simulating the real navigation handoff from Analysis → Chat where AppNavHost
     * calls chatViewModel.seedFromAnalysis(...) before navigating to the chat screen.
     */
    @Test
    fun `6_5_seedFromAnalysis_transitions_blank_to_seeded_mode`() {
        // Arrange: ChatViewModel created without analysis context (blank mode)
        val viewModel = ChatViewModel()
        assertIs<ChatMode.Blank>(viewModel.uiState.value.mode)
        assertEquals(0, viewModel.uiState.value.messages.size)

        // Act: AppNavHost calls seedFromAnalysis before navigating to chat
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

        // Assert: State transitions to AnalysisSeeded with correct context
        val state = viewModel.uiState.value
        assertIs<ChatMode.AnalysisSeeded>(state.mode)
        assertEquals(analysisId, state.mode.analysisId)
        assertEquals("Roya naranja", state.mode.diagnosis.pestName)
        assertEquals(0.87f, state.mode.diagnosis.confidence)
        assertEquals("Alta", state.mode.diagnosis.severity)
        assertEquals("Hojas", state.mode.diagnosis.affectedArea)
        assertEquals("Hongo Puccinia", state.mode.diagnosis.cause)

        // Seed message is present (assistant message with diagnosis text)
        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Roya naranja"))
    }

    /**
     * Verifies that seedFromAnalysis replaces the existing seed with a new analysis context.
     * This simulates the case where a user does a new analysis and navigates to chat again —
     * the new analysis seed replaces the previous one, resetting the chat to a fresh
     * seeded conversation with the new diagnosis.
     */
    @Test
    fun `6_5_seedFromAnalysis_replaces_existing_seed_with_new_diagnosis`() {
        // Arrange: ChatViewModel seeded with initial analysis
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
            analysisId = "analysis_first",
            diagnosis = initialDiagnosis,
        )
        assertEquals("Initial Pest", viewModel.uiState.value.mode.let { (it as ChatMode.AnalysisSeeded).diagnosis.pestName })

        // Act: User does new analysis, navigates to chat with new diagnosis
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

        // Assert: New analysis seed replaces the previous one
        val state = viewModel.uiState.value
        assertIs<ChatMode.AnalysisSeeded>(state.mode)
        assertEquals("analysis_second", state.mode.analysisId)
        assertEquals("Nueva plaga", state.mode.diagnosis.pestName)
        assertEquals("Crítica", state.mode.diagnosis.severity)
        assertEquals("Raíz", state.mode.diagnosis.affectedArea)
        assertEquals("Nematodo", state.mode.diagnosis.cause)

        // Fresh seed message from new analysis
        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Nueva plaga"))
    }

    // ========== Shared Instance Continuity (Phase 5 architecture requirement) ==========

    @Test
    fun `shared_viewmodel_voice_then_text_same_history`() {
        // Critical test: When ChatViewModel is shared between Chat and VoiceReady,
        // messages from VoiceReady appear in Chat's message list.
        // This is the core architectural requirement that Phase 5 fixes.
        val chatViewModel = ChatViewModel()

        // User is in Chat, types text
        chatViewModel.onEvent(ChatEvent.InputChanged("I need help with this"))

        // User taps microphone → VoiceReady (same ViewModel)
        chatViewModel.onEvent(ChatEvent.StartVoiceInput)

        // User stops recording → returns to Chat with audio message
        chatViewModel.onEvent(ChatEvent.StopVoiceInput)

        // Result: Chat history has user audio message + mock assistant reply
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
}
