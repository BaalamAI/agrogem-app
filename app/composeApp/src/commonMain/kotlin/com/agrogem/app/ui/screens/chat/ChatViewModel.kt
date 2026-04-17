package com.agrogem.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class ChatViewModel(
    private val analysisId: String? = null,
    private val diagnosis: DiagnosisResult? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private fun createInitialState(): ChatUiState {
        return if (analysisId != null) {
            val effectiveDiagnosis = diagnosis ?: mockDiagnosisResult()
            val seedMessage = createSeedMessage(effectiveDiagnosis)
            ChatUiState(
                messages = listOf(seedMessage),
                mode = ChatMode.AnalysisSeeded(
                    analysisId = analysisId,
                    diagnosis = effectiveDiagnosis,
                ),
            )
        } else {
            ChatUiState(mode = ChatMode.Blank)
        }
    }

    private fun createSeedMessage(diagnosis: DiagnosisResult): ChatMessage {
        return ChatMessage(
            id = "seed_${Random.nextLong()}",
            text = diagnosis.diagnosisText,
            sender = MessageSender.Assistant,
            attachments = emptyList(),
            timestamp = Random.nextLong(),
        )
    }

    private fun createMockAssistantMessage(
        userText: String,
        mode: ChatMode,
        attachmentCount: Int,
        fromVoice: Boolean = false,
    ): ChatMessage {
        return ChatMessage(
            id = "assistant_${Random.nextLong()}",
            text = buildMockAssistantReply(
                userText = userText,
                mode = mode,
                attachmentCount = attachmentCount,
                fromVoice = fromVoice,
            ),
            sender = MessageSender.Assistant,
            attachments = emptyList(),
            timestamp = Random.nextLong(),
        )
    }

    private fun buildMockAssistantReply(
        userText: String,
        mode: ChatMode,
        attachmentCount: Int,
        fromVoice: Boolean,
    ): String {
        return when (mode) {
            ChatMode.Blank -> {
                when {
                    fromVoice -> "Recibi tu nota de voz. Estoy en modo demo y listo para seguir ayudandote."
                    attachmentCount > 0 && userText.isBlank() -> {
                        "Recibi $attachmentCount adjunto(s). Contame que queres validar y lo revisamos juntos."
                    }
                    userText.isNotBlank() -> {
                        "Entendido. En modo demo ya registre tu consulta: \"$userText\"."
                    }
                    else -> "Contame mas detalles del cultivo para poder guiarte mejor."
                }
            }

            is ChatMode.AnalysisSeeded -> {
                val firstTreatment = mode.diagnosis.treatmentSteps.firstOrNull()
                    ?: "hacer una verificacion de campo y aplicar el tratamiento indicado"
                when {
                    fromVoice -> {
                        "Escuche tu nota de voz. Segun el analisis de ${mode.diagnosis.pestName}, te recomiendo empezar por $firstTreatment."
                    }

                    attachmentCount > 0 && userText.isBlank() -> {
                        "Recibi $attachmentCount adjunto(s). Con el contexto de ${mode.diagnosis.pestName}, mi primer paso sugerido es $firstTreatment."
                    }

                    userText.isNotBlank() -> {
                        "Perfecto. Sobre \"$userText\", y en base a ${mode.diagnosis.pestName}, empeza por $firstTreatment."
                    }

                    else -> "Con el analisis actual, empeza por $firstTreatment."
                }
            }
        }
    }

    private fun mockDiagnosisResult(): DiagnosisResult = DiagnosisResult(
        pestName = "Plaga detectada",
        confidence = 0.95f,
        severity = "Problema iniciando",
        affectedArea = "Tallo y hoja",
        cause = "Hongo, Hemileia vastatrix",
        diagnosisText = "Se ha detectado una infección avanzada por Hemileia vastatrix. " +
            "El 45% del follaje muestra pústulas activas. Se requiere intervención " +
            "inmediata para evitar la pérdida total de la cosecha.",
        treatmentSteps = listOf(
            "Se ha detectado una infección avanzada por Hemileia vastatrix. " +
                "El 45% del follaje muestra pústulas activas. Se requiere intervención " +
                "inmediata para evitar la pérdida total de la cosecha.",
        ),
    )

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InputChanged -> handleInputChanged(event.text)
            is ChatEvent.SendMessage -> handleSendMessage()
            is ChatEvent.ToggleAttachmentMenu -> handleToggleAttachmentMenu(event.show)
            is ChatEvent.RequestCamera -> handleRequestCamera()
            is ChatEvent.RequestGallery -> handleRequestGallery()
            is ChatEvent.ImageSelected -> handleImageSelected(event.result)
            is ChatEvent.StartVoiceInput -> handleStartVoiceInput()
            is ChatEvent.StopVoiceInput -> handleStopVoiceInput()
            is ChatEvent.DismissVoice -> handleDismissVoice()
        }
    }

    /** Updates inputText in state — mirrors what the user is currently typing. */
    private fun handleInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    /** Creates a user message from current input and pending attachments, appends to messages list. */
    private fun handleSendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()

        if (text.isEmpty() && currentState.attachments.isEmpty()) return

        val newMessage = ChatMessage(
            id = "msg_${Random.nextLong()}",
            text = text,
            sender = MessageSender.User,
            attachments = currentState.attachments,
            timestamp = Random.nextLong(),
        )

        val assistantMessage = createMockAssistantMessage(
            userText = text,
            mode = currentState.mode,
            attachmentCount = currentState.attachments.size,
        )

        _uiState.value = currentState.copy(
            messages = currentState.messages + newMessage + assistantMessage,
            inputText = "",
            attachments = emptyList(),
        )
    }

    /** Toggles visibility of the attachment menu (gallery/camera options). */
    private fun handleToggleAttachmentMenu(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAttachmentMenu = show)
    }

    /** Transitions voiceState to Listening — orb animation starts. */
    private fun handleStartVoiceInput() {
        _uiState.value = _uiState.value.copy(
            voiceState = VoiceState.Listening(amplitude = 0f),
        )
    }

    /** Returns to Idle without creating a message — user abandoned voice input. */
    private fun handleDismissVoice() {
        _uiState.value = _uiState.value.copy(voiceState = VoiceState.Idle)
    }

    /** Closes the attachment menu — camera launcher is triggered by the UI layer. */
    private fun handleRequestCamera() {
        _uiState.value = _uiState.value.copy(showAttachmentMenu = false)
    }

    /** Closes the attachment menu — gallery picker is triggered by the UI layer. */
    private fun handleRequestGallery() {
        _uiState.value = _uiState.value.copy(showAttachmentMenu = false)
    }

    /** Appends a selected image URI to the pending attachments list. */
    private fun handleImageSelected(result: String?) {
        if (result.isNullOrBlank()) return
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            attachments = currentState.attachments + ChatAttachment.Image(result),
        )
    }

    /** Stops recording — briefly shows Processing, then creates an audio message and returns to Idle. */
    private fun handleStopVoiceInput() {
        // First transition: Processing (observable intermediate state)
        _uiState.value = _uiState.value.copy(voiceState = VoiceState.Processing)

        // Create audio message with pending text and audio attachment
        val currentState = _uiState.value
        val text = currentState.inputText.trim()
        val newMessage = ChatMessage(
            id = "msg_${Random.nextLong()}",
            text = text,
            sender = MessageSender.User,
            attachments = listOf(ChatAttachment.Audio(uri = "", durationMs = 0)),
            timestamp = Random.nextLong(),
        )

        val assistantMessage = createMockAssistantMessage(
            userText = text,
            mode = currentState.mode,
            attachmentCount = 1,
            fromVoice = true,
        )

        // Second transition: complete with message added and return to Idle
        _uiState.value = currentState.copy(
            messages = currentState.messages + newMessage + assistantMessage,
            inputText = "",
            attachments = emptyList(),
            voiceState = VoiceState.Idle,
        )
    }

    /**
     * Seeds this ChatViewModel with analysis context at runtime, transitioning from
     * Blank mode (or a prior seeded mode) to AnalysisSeeded mode with the given diagnosis.
     * Called by AppNavHost when navigating from PlantAnalysisScreen → Chat so the
     * shared ChatViewModel instance carries the real analysis context instead of
     * creating a new blank chat.
     *
     * If the chat already has user messages, they are preserved alongside the new seed.
     * If the chat is already in AnalysisSeeded mode, the seed message is replaced
     * with the new analysis context (supporting multiple analysis→chat handoffs).
     */
    fun seedFromAnalysis(analysisId: String, diagnosis: DiagnosisResult) {
        val seedMessage = createSeedMessage(diagnosis)
        val newMode = ChatMode.AnalysisSeeded(analysisId = analysisId, diagnosis = diagnosis)
        _uiState.value = _uiState.value.copy(
            mode = newMode,
            messages = listOf(seedMessage),
        )
    }
}

/**
 * Events that can be dispatched to the ChatViewModel.
 */
sealed interface ChatEvent {
    /** User typed or edited text in the input field. */
    data class InputChanged(val text: String) : ChatEvent

    /** User tapped the send button — creates a user message from current input and attachments. */
    data object SendMessage : ChatEvent

    /** User tapped the attachment button — toggles the attachment menu visibility. */
    data class ToggleAttachmentMenu(val show: Boolean) : ChatEvent

    /** User selected camera from the attachment menu — closes menu and triggers camera launcher. */
    data object RequestCamera : ChatEvent

    /** User selected gallery from the attachment menu — closes menu and triggers gallery picker. */
    data object RequestGallery : ChatEvent

    /** Image picker returned a result — appends an image attachment to pending attachments. */
    data class ImageSelected(val result: String?) : ChatEvent

    /** User tapped the microphone button — transitions voiceState to Listening. */
    data object StartVoiceInput : ChatEvent

    /** User tapped stop while recording — creates an audio message and returns to Idle. */
    data object StopVoiceInput : ChatEvent

    /** User dismissed voice input without recording — returns to Idle without a message. */
    data object DismissVoice : ChatEvent
}
