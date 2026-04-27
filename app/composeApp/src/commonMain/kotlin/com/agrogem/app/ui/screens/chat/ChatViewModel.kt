package com.agrogem.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.getGemmaManager
import com.agrogem.app.data.getGemmaModelDownloader
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

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
            timestamp = Clock.System.now().toEpochMilliseconds(),
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
            timestamp = Clock.System.now().toEpochMilliseconds(),
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
                    fromVoice -> "Recibí tu nota de voz. Estoy listo para seguir ayudándote."
                    attachmentCount > 0 && userText.isBlank() -> {
                        "Recibi $attachmentCount adjunto(s). Contame que queres validar y lo revisamos juntos."
                    }
                    userText.isNotBlank() -> {
                        "Entendido. Ya registré tu consulta: \"$userText\"."
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
            is ChatEvent.ToggleThinking -> handleToggleThinking(event.enabled)
        }
    }

    private fun handleInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    private fun handleSendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()

        if (text.isEmpty() && currentState.attachments.isEmpty()) return

        val userMessage = ChatMessage(
            id = "msg_${Random.nextLong()}",
            text = text,
            sender = MessageSender.User,
            attachments = currentState.attachments,
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )

        val assistantMessage = createMockAssistantMessage(
            userText = text,
            mode = currentState.mode,
            attachmentCount = currentState.attachments.size
        )

        _uiState.value = currentState.copy(
            messages = currentState.messages + userMessage + assistantMessage,
            inputText = "",
            attachments = emptyList(),
            showAttachmentMenu = false,
        )
    }


    private fun updateAssistantMessage(id: String, text: String, thought: String?, isDone: Boolean) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == id }
        if (index != -1) {
            currentMessages[index] = currentMessages[index].copy(
                text = text,
                thought = thought,
                isStreaming = !isDone
            )
            _uiState.value = _uiState.value.copy(messages = currentMessages)
        }
    }

    private fun handleToggleThinking(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useThinking = enabled)
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
        // Capture state once to avoid double-read race between transitions
        val captured = _uiState.value

        // First transition: Processing (observable intermediate state)
        _uiState.value = captured.copy(voiceState = VoiceState.Processing)

        // Create audio message with pending text and audio attachment
        val text = captured.inputText.trim()
        val newMessage = ChatMessage(
            id = "msg_${Random.nextLong()}",
            text = text,
            sender = MessageSender.User,
            attachments = listOf(ChatAttachment.Audio(uri = "", durationMs = 0)),
            timestamp = Random.nextLong(),
        )

        val assistantMessage = createMockAssistantMessage(
            userText = text,
            mode = captured.mode,
            attachmentCount = 1,
            fromVoice = true,
        )

        // Second transition: complete with message added and return to Idle
        _uiState.value = captured.copy(
            messages = captured.messages + newMessage + assistantMessage,
            inputText = "",
            attachments = emptyList(),
            showAttachmentMenu = false,
            voiceState = VoiceState.Idle,
        )
    }

    /**
     * Resets the chat to a blank state, clearing messages and mode.
     * Used when opening a new free chat from the conversations list.
     */
    fun resetToBlank() {
        _uiState.value = ChatUiState(mode = ChatMode.Blank)
    }

    /**
     * Seeds this ChatViewModel with analysis context at runtime, transitioning from
     * Blank mode (or a prior seeded mode) to AnalysisSeeded mode with the given diagnosis.
     */
    fun seedFromAnalysis(analysisId: String, diagnosis: DiagnosisResult) {
        val currentState = _uiState.value
        val seedMessage = createSeedMessage(diagnosis)
        val newMode = ChatMode.AnalysisSeeded(analysisId = analysisId, diagnosis = diagnosis)

        val newMessages = when (currentState.mode) {
            is ChatMode.AnalysisSeeded -> {
                // Replace existing seed (first message), preserve the rest
                listOf(seedMessage) + currentState.messages.drop(1)
            }
            else -> {
                // Prepend seed to existing messages (preserves user messages in Blank mode)
                listOf(seedMessage) + currentState.messages
            }
        }

        _uiState.value = currentState.copy(
            mode = newMode,
            messages = newMessages,
        )
    }
}

/**
 * Events that can be dispatched to the ChatViewModel.
 */
sealed interface ChatEvent {
    data class InputChanged(val text: String) : ChatEvent
    data object SendMessage : ChatEvent
    data class ToggleAttachmentMenu(val show: Boolean) : ChatEvent
    data object RequestCamera : ChatEvent
    data object RequestGallery : ChatEvent
    data class ImageSelected(val result: String?) : ChatEvent
    data object StartVoiceInput : ChatEvent
    data object StopVoiceInput : ChatEvent
    data object DismissVoice : ChatEvent
    data class ToggleThinking(val enabled: Boolean) : ChatEvent
}
