package com.agrogem.app.ui.screens.chat

import androidx.compose.runtime.Immutable
import com.agrogem.app.ui.screens.analysis.DiagnosisResult

/**
 * Root UI state for the chat screen.
 * Exposes all observable chat state as a single immutable snapshot.
 */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val showAttachmentMenu: Boolean = attachments.isNotEmpty(),
    val mode: ChatMode = ChatMode.Blank,
    val onboardingDemoStage: OnboardingDemoStage? = null,
    val voiceState: VoiceState = VoiceState.Idle,
)

/**
 * Represents a single message in the chat history.
 */
@Immutable
data class ChatMessage(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val attachments: List<ChatAttachment> = emptyList(),
    val timestamp: Long,
)

/**
 * Sender of a chat message.
 */
enum class MessageSender {
    User,
    Assistant,
}

/**
 * A media attachment on a chat message.
 */
sealed interface ChatAttachment {
    data class Image(val uri: String) : ChatAttachment
    data class Audio(val uri: String, val durationMs: Long) : ChatAttachment
}

/**
 * Represents the conversation mode — either a blank chat
 * or one seeded with a prior pest analysis.
 */
sealed interface ChatMode {
    /** Fresh conversation with no prior context. */
    data object Blank : ChatMode

    /** Scripted onboarding demo used before the first real app session. */
    data class OnboardingDemo(
        val completed: Boolean = false,
    ) : ChatMode

    /** Conversation seeded with a prior pest analysis. */
    data class AnalysisSeeded(
        val analysisId: String,
        val diagnosis: DiagnosisResult,
    ) : ChatMode
}

/**
 * Represents the state of voice recording interaction.
 */
sealed interface VoiceState {
    /** Voice input is idle, not active. */
    data object Idle : VoiceState

    /** Actively listening and capturing audio. [amplitude] drives the orb animation (0.0 to 1.0). */
    data class Listening(val amplitude: Float) : VoiceState

    /** Audio captured, processing to create a message. */
    data object Processing : VoiceState

    /** Voice input failed with an error. */
    data class Error(val message: String) : VoiceState
}

/**
 * Stage progression used by the onboarding chat demo.
 */
sealed interface OnboardingDemoStage {
    data object Conversation : OnboardingDemoStage
    data object AwaitingLocationPermission : OnboardingDemoStage
    data object AlertsPreferences : OnboardingDemoStage
    data object Final : OnboardingDemoStage
}
