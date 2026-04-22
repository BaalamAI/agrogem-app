package com.agrogem.app.ui.screens.chat

import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ChatUiStateTest {

    // ========== ChatMessage Tests ==========

    @Test
    fun `ChatMessage is immutable - changing field creates new instance`() {
        val original = ChatMessage(
            id = "msg1",
            text = "Hello",
            sender = MessageSender.User,
            attachments = emptyList(),
            timestamp = 1000L,
        )

        val modified = original.copy(text = "Modified")

        assertEquals("Hello", original.text)
        assertEquals("Modified", modified.text)
        assertNotEquals(original, modified)
    }

    @Test
    fun `ChatMessage stores all fields correctly`() {
        val msg = ChatMessage(
            id = "msg2",
            text = "Test message",
            sender = MessageSender.Assistant,
            attachments = listOf(ChatAttachment.Image(uri = "content://image1.jpg")),
            timestamp = 2000L,
        )

        assertEquals("msg2", msg.id)
        assertEquals("Test message", msg.text)
        assertEquals(MessageSender.Assistant, msg.sender)
        assertEquals(1, msg.attachments.size)
        assertEquals(2000L, msg.timestamp)
    }

    @Test
    fun `ChatMessage with multiple attachments preserves order`() {
        val attachments = listOf(
            ChatAttachment.Image(uri = "img1"),
            ChatAttachment.Audio(uri = "audio1", durationMs = 5000L),
            ChatAttachment.Image(uri = "img2"),
        )
        val msg = ChatMessage(
            id = "msg3",
            text = "",
            sender = MessageSender.User,
            attachments = attachments,
            timestamp = 0L,
        )

        assertEquals(3, msg.attachments.size)
        // Verify attachment data via explicit cast — checks both type and value
        val first = msg.attachments[0] as ChatAttachment.Image
        assertEquals("img1", first.uri)

        val second = msg.attachments[1] as ChatAttachment.Audio
        assertEquals("audio1", second.uri)
        assertEquals(5000L, second.durationMs)
    }

    // ========== MessageSender Tests ==========

    @Test
    fun `MessageSender has exactly User and Assistant`() {
        val values = MessageSender.entries
        assertEquals(2, values.size)
    }

    @Test
    fun `MessageSender User and Assistant are distinct`() {
        assertNotEquals(MessageSender.User, MessageSender.Assistant)
    }

    // ========== ChatAttachment Tests ==========

    @Test
    fun `ChatAttachment Image stores URI`() {
        val attachment = ChatAttachment.Image(uri = "content://test.jpg")
        assertEquals("content://test.jpg", attachment.uri)
    }

    @Test
    fun `ChatAttachment Audio stores URI and duration`() {
        val attachment = ChatAttachment.Audio(uri = "content://test.m4a", durationMs = 10000L)
        assertEquals("content://test.m4a", attachment.uri)
        assertEquals(10000L, attachment.durationMs)
    }

    @Test
    fun `ChatAttachment Image and Audio store distinct data`() {
        val image = ChatAttachment.Image(uri = "test.jpg")
        val audio = ChatAttachment.Audio(uri = "test.m4a", durationMs = 5000L)
        // Verify actual stored values — not just type membership
        assertEquals("test.jpg", image.uri)
        assertEquals("test.m4a", audio.uri)
        assertEquals(5000L, audio.durationMs)
        // Distinct types produce unequal instances even with same outer value
        val image2 = ChatAttachment.Image(uri = "test.jpg")
        assertEquals(image, image2, "Same data produces equal instances")
        assertNotEquals<ChatAttachment>(image, audio)
    }

    // ========== ChatMode Tests ==========

    @Test
    fun `ChatMode Blank is singleton`() {
        val blank1 = ChatMode.Blank
        val blank2 = ChatMode.Blank
        assertEquals(blank1, blank2)
    }

    @Test
    fun `ChatMode Blank carries no data`() {
        val blank = ChatMode.Blank
        // Verify Blank is a singleton — equality confirms identity
        assertEquals(ChatMode.Blank, blank)
        // Blank has no fields; exhaustiveness confirmed by exhaustive when in caller
    }

    @Test
    fun `ChatMode AnalysisSeeded carries analysis context`() {
        val diagnosis = DiagnosisResult(
            pestName = "Roya",
            confidence = 0.92f,
            severity = "Alta",
            affectedArea = "Hojas",
            cause = "Hongo",
            diagnosisText = "Infección fúngica",
            treatmentSteps = listOf("Aplicar fungicida"),
        )
        val seeded = ChatMode.AnalysisSeeded(
            analysisId = "analysis123",
            diagnosis = diagnosis,
        )

        // Verify actual diagnosis data is accessible through the seeded mode
        assertEquals("analysis123", seeded.analysisId)
        assertEquals("Roya", seeded.diagnosis.pestName)
        assertEquals(0.92f, seeded.diagnosis.confidence)
    }

    @Test
    fun `ChatMode sealed interface enables exhaustive when`() {
        fun describe(mode: ChatMode): String = when (mode) {
            is ChatMode.Blank -> "blank"
            is ChatMode.AnalysisSeeded -> "seeded:${mode.analysisId}"
        }

        assertEquals("blank", describe(ChatMode.Blank))
        val testDiagnosis = DiagnosisResult("", 0f, "", "", "", "", emptyList())
        assertEquals("seeded:abc", describe(ChatMode.AnalysisSeeded("abc", testDiagnosis)))
    }

    // ========== VoiceState Tests ==========

    @Test
    fun `VoiceState Idle is singleton`() {
        val idle1 = VoiceState.Idle
        val idle2 = VoiceState.Idle
        assertEquals(idle1, idle2)
    }

    @Test
    fun `VoiceState Listening carries amplitude`() {
        val listening = VoiceState.Listening(amplitude = 0.75f)
        // Verify amplitude value is correctly stored — not just type membership
        assertEquals(0.75f, listening.amplitude)
    }

    @Test
    fun `VoiceState Listening amplitude range is valid`() {
        val low = VoiceState.Listening(0.0f)
        val high = VoiceState.Listening(1.0f)
        assertEquals(0.0f, low.amplitude)
        assertEquals(1.0f, high.amplitude)
    }

    @Test
    fun `VoiceState Processing is singleton`() {
        val processing1 = VoiceState.Processing
        val processing2 = VoiceState.Processing
        assertEquals(processing1, processing2)
    }

    @Test
    fun `VoiceState Error carries message`() {
        val error = VoiceState.Error(message = "Microphone unavailable")
        // Verify error message is correctly stored — not just type membership
        assertEquals("Microphone unavailable", error.message)
    }

    @Test
    fun `VoiceState sealed interface enables exhaustive when`() {
        fun describeState(state: VoiceState): String = when (state) {
            is VoiceState.Idle -> "idle"
            is VoiceState.Listening -> "listening:${state.amplitude}"
            is VoiceState.Processing -> "processing"
            is VoiceState.Error -> "error:${state.message}"
        }

        assertEquals("idle", describeState(VoiceState.Idle))
        assertEquals("listening:0.5", describeState(VoiceState.Listening(0.5f)))
        assertEquals("processing", describeState(VoiceState.Processing))
        assertEquals("error:fail", describeState(VoiceState.Error("fail")))
    }

    // ========== ChatUiState Tests ==========

    @Test
    fun `ChatUiState default values are empty and Idle`() {
        val state = ChatUiState()

        assertEquals<List<ChatMessage>>(emptyList(), state.messages)
        assertEquals("", state.inputText)
        assertEquals(emptyList<ChatAttachment>(), state.attachments)
        assertEquals(ChatMode.Blank, state.mode)
        assertEquals(VoiceState.Idle, state.voiceState)
        assertEquals(false, state.showAttachmentMenu)
    }

    @Test
    fun `ChatUiState is immutable - modifying creates new instance`() {
        val original = ChatUiState()
        val modified = original.copy(inputText = "New input")

        assertEquals("", original.inputText)
        assertEquals("New input", modified.inputText)
    }

    @Test
    fun `ChatUiState seeded mode contains diagnosis context`() {
        val diagnosis = DiagnosisResult(
            pestName = "Test pest",
            confidence = 0.9f,
            severity = "Test",
            affectedArea = "Test area",
            cause = "Test cause",
            diagnosisText = "Test diagnosis",
            treatmentSteps = listOf("Step 1"),
        )
        val state = ChatUiState(
            mode = ChatMode.AnalysisSeeded(analysisId = "id123", diagnosis = diagnosis),
        )

        val seededMode = state.mode as ChatMode.AnalysisSeeded
        assertEquals("id123", seededMode.analysisId)
        assertEquals("Test pest", seededMode.diagnosis.pestName)
    }

    @Test
    fun `ChatUiState with messages preserves order`() {
        val messages = listOf(
            ChatMessage("m1", "First", MessageSender.User, emptyList(), 100L),
            ChatMessage("m2", "Second", MessageSender.Assistant, emptyList(), 200L),
        )
        val state = ChatUiState(messages = messages)

        assertEquals(2, state.messages.size)
        assertEquals("First", state.messages[0].text)
        assertEquals("Second", state.messages[1].text)
    }

    @Test
    fun `ChatUiState with pending attachments`() {
        val attachments = listOf(
            ChatAttachment.Image(uri = "img1"),
            ChatAttachment.Image(uri = "img2"),
        )
        val state = ChatUiState(attachments = attachments, showAttachmentMenu = true)

        assertEquals(2, state.attachments.size)
        assertEquals(true, state.showAttachmentMenu)
    }
}
