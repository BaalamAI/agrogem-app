package com.agrogem.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.MessageSender

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Dedicated state holder for the onboarding chat demo.
 *
 * Isolates onboarding-specific state machine and orchestration from the
 * real in-app [com.agrogem.app.ui.screens.chat.ChatViewModel] so the two
 * can evolve independently.
 */
class OnboardingChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingChatUiState())
    val uiState: StateFlow<OnboardingChatUiState> = _uiState.asStateFlow()

    fun startOnboardingDemo() {
        if (_uiState.value.onboardingDemoStage != null) return
        _uiState.value = OnboardingChatUiState(
            messages = listOf(
                assistantMessage("Para empezar, ¿cómo te llamás? 😊"),
            ),
            onboardingDemoStage = OnboardingDemoStage.Conversation,
            onboardingStep = 0,
        )
    }

    fun continueOnboardingDemoAfterLocationPermission() {
        val currentState = _uiState.value
        if (currentState.onboardingDemoStage == null) return
        _uiState.value = currentState.copy(
            onboardingDemoStage = OnboardingDemoStage.AlertsPreferences,
        )
    }

    fun completeOnboardingDemo(alertsEnabled: Boolean = true) {
        val currentState = _uiState.value
        if (currentState.onboardingDemoStage == null) return
        _uiState.value = currentState.copy(
            onboardingDemoStage = OnboardingDemoStage.Final,
            alertsEnabled = alertsEnabled,
        )
    }

    fun skipOnboardingAlerts() {
        val currentState = _uiState.value
        if (currentState.onboardingDemoStage == null) return
        _uiState.value = currentState.copy(
            onboardingDemoStage = OnboardingDemoStage.Final,
            alertsEnabled = false,
        )
    }

    /**
     * Handles a real user message during the onboarding demo.
     * Advances a step-based state machine: each user send triggers the next
     * assistant prompt. After the fourth exchange the flow transitions to
     * the location-permission stage.
     */
    fun sendOnboardingUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val currentState = _uiState.value
        if (currentState.onboardingDemoStage != OnboardingDemoStage.Conversation) return

        val step = currentState.onboardingStep
        val userMessage = ChatMessage(
            id = "onboard_user_${Random.nextLong()}",
            text = trimmed,
            sender = MessageSender.User,
            attachments = emptyList(),
            timestamp = Random.nextLong(),
        )

        val replyText = buildOnboardingReply(step, trimmed)
        val assistantMessage = ChatMessage(
            id = "onboard_assistant_${Random.nextLong()}",
            text = replyText,
            sender = MessageSender.Assistant,
            attachments = emptyList(),
            timestamp = Random.nextLong(),
        )

        val nextStep = step + 1
        val nextStage = if (nextStep >= 4) {
            OnboardingDemoStage.AwaitingLocationPermission
        } else {
            OnboardingDemoStage.Conversation
        }

        _uiState.value = currentState.copy(
            messages = currentState.messages + userMessage + assistantMessage,
            inputText = "",
            onboardingStep = nextStep,
            onboardingDemoStage = nextStage,
        )
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    private fun buildOnboardingReply(step: Int, userText: String): String {
        return when (step) {
            0 -> "Mucho gusto. ¿Qué cultivo o cultivos tenés? Podés mencionar más de uno. 🌱"
            1 -> "¡Buenísimo! ¿Cuántas manzanas o hectáreas tiene tu cultivo? Si podés dame las dimensiones por separado mejor. 📐"
            2 -> "¿Cuánto tiempo tiene que lo sembraste? ¿En qué etapa está?\n\n1. Alistando la tierra — preparación y siembra\n2. Saliendo el puyón — nacencia\n3. Poniéndose shule — crecimiento\n4. Dando la flor — floración\n5. Cargando el fruto — llenado\n6. Punto de corte — cosecha"
            3 -> "Para darte consejos más exactos necesito saber dónde están tus cultivos. Así puedo ver la temperatura, lluvias y condiciones de tu zona. 📍"
            else -> "Entendido. Para seguir, necesito saber dónde están tus cultivos así te doy info de tu zona. 📍"
        }
    }

    private fun assistantMessage(text: String): ChatMessage = ChatMessage(
        id = "demo_assistant_${Random.nextLong()}",
        text = text,
        sender = MessageSender.Assistant,
        attachments = emptyList(),
        timestamp = Random.nextLong(),
    )
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

/**
 * UI state specific to the onboarding chat demo.
 *
 * Mirrors only the fields needed for the scripted onboarding flow,
 * avoiding any dependency on real-chat concepts (attachments, voice,
 * camera, gallery, etc.).
 */
@androidx.compose.runtime.Immutable
data class OnboardingChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val onboardingDemoStage: OnboardingDemoStage? = null,
    val onboardingStep: Int = 0,
    val alertsEnabled: Boolean = false,
) {
    /**
     * Derives onboarding progress from message count and stage.
     * Progress advances incrementally with each conversation exchange,
     * then jumps to stage-based milestones for location, alerts, and final.
     */
    val onboardingProgress: Float
        get() = when (onboardingDemoStage) {
            is OnboardingDemoStage.Conversation -> {
                // 1 initial assistant message + up to 8 more (4 exchanges × 2).
                // Full flow ≈ 12 conceptual message slots (9 conversation + location + alerts + final).
                (messages.size.coerceAtMost(9) / 12f).coerceAtLeast(0.04f)
            }
            is OnboardingDemoStage.AwaitingLocationPermission -> 10f / 12f
            is OnboardingDemoStage.AlertsPreferences -> 11f / 12f
            is OnboardingDemoStage.Final -> 1f
            null -> 0f
        }
}
