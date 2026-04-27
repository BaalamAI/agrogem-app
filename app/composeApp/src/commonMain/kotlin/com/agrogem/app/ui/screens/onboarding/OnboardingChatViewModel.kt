package com.agrogem.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.MessageSender

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Dedicated state holder for the onboarding chat flow.
 *
 * Isolates onboarding-specific state machine and orchestration from the
 * real in-app [com.agrogem.app.ui.screens.chat.ChatViewModel] so the two
 * can evolve independently.
 */
private val PHONE_REGEX = Regex("^\\+?[0-9]{7,15}$")

class OnboardingChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingChatUiState())
    val uiState: StateFlow<OnboardingChatUiState> = _uiState.asStateFlow()

    fun startOnboardingChat() {
        if (_uiState.value.onboardingChatStage != null) return
        _uiState.value = OnboardingChatUiState(
            messages = listOf(
                assistantMessage("Para empezar, ¿cómo te llamás? 😊"),
            ),
            onboardingChatStage = OnboardingChatStage.Conversation,
            onboardingStep = 0,
            userName = null,
            userPhone = null,
            userPassword = null,
        )
    }

    fun continueOnboardingAfterLocationPermission() {
        val currentState = _uiState.value
        if (currentState.onboardingChatStage == null) return
        _uiState.value = currentState.copy(
            onboardingChatStage = OnboardingChatStage.AlertsPreferences,
        )
    }

    fun completeOnboarding(alertsEnabled: Boolean = true) {
        val currentState = _uiState.value
        if (currentState.onboardingChatStage == null) return
        _uiState.value = currentState.copy(
            onboardingChatStage = OnboardingChatStage.Final,
            alertsEnabled = alertsEnabled,
        )
    }

    fun skipOnboardingAlerts() {
        val currentState = _uiState.value
        if (currentState.onboardingChatStage == null) return
        _uiState.value = currentState.copy(
            onboardingChatStage = OnboardingChatStage.Final,
            alertsEnabled = false,
        )
    }

    /**
     * Handles a real user message during the onboarding chat.
     * Advances a step-based state machine: each user send triggers the next
     * assistant prompt. After the fourth exchange the flow transitions to
     * the location-permission stage.
     */
    fun sendOnboardingMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val currentState = _uiState.value
        if (currentState.onboardingChatStage != OnboardingChatStage.Conversation) return

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
            OnboardingChatStage.AwaitingLocationPermission
        } else {
            OnboardingChatStage.Conversation
        }

        _uiState.value = currentState.copy(
            messages = currentState.messages + userMessage + assistantMessage,
            inputText = "",
            onboardingStep = nextStep,
            onboardingChatStage = nextStage,
            userName = if (step == 0) trimmed else currentState.userName,
        )
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun onPhoneChanged(text: String) {
        _uiState.value = _uiState.value.copy(userPhone = text)
    }

    fun onPasswordChanged(text: String) {
        _uiState.value = _uiState.value.copy(userPassword = text)
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
        id = "onboard_assistant_${Random.nextLong()}",
        text = text,
        sender = MessageSender.Assistant,
        attachments = emptyList(),
        timestamp = Random.nextLong(),
    )
}

/**
 * Stage progression used by the onboarding chat.
 */
sealed interface OnboardingChatStage {
    data object Conversation : OnboardingChatStage
    data object AwaitingLocationPermission : OnboardingChatStage
    data object AlertsPreferences : OnboardingChatStage
    data object Final : OnboardingChatStage
}

/**
 * UI state specific to the onboarding chat.
 *
 * Mirrors only the fields needed for the scripted onboarding flow,
 * avoiding any dependency on real-chat concepts (attachments, voice,
 * camera, gallery, etc.).
 */
@androidx.compose.runtime.Immutable
data class OnboardingChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val onboardingChatStage: OnboardingChatStage? = null,
    val onboardingStep: Int = 0,
    val alertsEnabled: Boolean = false,
    val userName: String? = null,
    val userPhone: String? = null,
    val userPassword: String? = null,
) {
    val isPhoneValid: Boolean
        get() = userPhone?.matches(PHONE_REGEX) ?: false

    val isPasswordValid: Boolean
        get() {
            val length = userPassword?.length ?: 0
            return length >= 8 && length <= 128
        }

    val isFormValid: Boolean
        get() = isPhoneValid && isPasswordValid

    /**
     * Derives onboarding progress from message count and stage.
     * Progress advances incrementally with each conversation exchange,
     * then jumps to stage-based milestones for location, alerts, and final.
     */
    val onboardingProgress: Float
        get() = when (onboardingChatStage) {
            is OnboardingChatStage.Conversation -> {
                // 1 initial assistant message + up to 8 more (4 exchanges × 2).
                // Full flow ≈ 12 conceptual message slots (9 conversation + location + alerts + final).
                (messages.size.coerceAtMost(9) / 12f).coerceAtLeast(0.04f)
            }
            is OnboardingChatStage.AwaitingLocationPermission -> 10f / 12f
            is OnboardingChatStage.AlertsPreferences -> 11f / 12f
            is OnboardingChatStage.Final -> 1f
            null -> 0f
        }
}
