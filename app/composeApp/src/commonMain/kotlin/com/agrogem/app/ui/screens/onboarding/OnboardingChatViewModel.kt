package com.agrogem.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaModelDownloader
import com.agrogem.app.data.getGemmaManager
import com.agrogem.app.data.getGemmaModelDownloader
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.MessageSender

import kotlinx.coroutines.runBlocking
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
class OnboardingChatViewModel(
    private val assistant: OnboardingAssistant = GemmaOnboardingAssistant(),
) : ViewModel() {

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
            userCrops = null,
            userArea = null,
            userStage = null,
            locationEnabled = false,
            locationShared = false,
        )
    }

    fun continueOnboardingAfterLocationPermission() {
        val currentState = _uiState.value
        if (currentState.onboardingChatStage == null) return
        _uiState.value = currentState.copy(
            onboardingChatStage = OnboardingChatStage.AlertsPreferences,
            locationShared = true,
        )
    }

    fun completeOnboarding(alertsEnabled: Boolean = true) {
        val currentState = _uiState.value
        if (currentState.onboardingChatStage == null) return
        _uiState.value = currentState.copy(
            onboardingChatStage = OnboardingChatStage.Final,
            alertsEnabled = alertsEnabled,
            locationEnabled = currentState.locationShared,
        )
    }

    fun skipOnboardingAlerts() {
        val currentState = _uiState.value
        if (currentState.onboardingChatStage == null) return
        _uiState.value = currentState.copy(
            onboardingChatStage = OnboardingChatStage.Final,
            alertsEnabled = false,
            locationEnabled = currentState.locationShared,
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

        val nextStep = step + 1
        val nextStage = if (nextStep >= 4) {
            OnboardingChatStage.AwaitingLocationPermission
        } else {
            OnboardingChatStage.Conversation
        }

        val nextState = currentState.copy(
            messages = currentState.messages + userMessage,
            inputText = "",
            onboardingStep = nextStep,
            onboardingChatStage = nextStage,
            userName = if (step == 0) trimmed else currentState.userName,
            userCrops = if (step == 1) trimmed else currentState.userCrops,
            userArea = if (step == 2) trimmed else currentState.userArea,
            userStage = if (step == 3) trimmed else currentState.userStage,
        )
        val replyText = runBlocking {
            assistant.reply(
                step = step,
                userText = trimmed,
                draftState = nextState,
            )
        }
        _uiState.value = nextState.copy(
            messages = nextState.messages + ChatMessage(
                id = "onboard_assistant_${Random.nextLong()}",
                text = replyText,
                sender = MessageSender.Assistant,
                attachments = emptyList(),
                timestamp = Random.nextLong(),
            ),
        )
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
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
    val userCrops: String? = null,
    val userArea: String? = null,
    val userStage: String? = null,
    val locationShared: Boolean = false,
    val locationEnabled: Boolean = false,
) {

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

interface OnboardingAssistant {
    suspend fun reply(step: Int, userText: String, draftState: OnboardingChatUiState): String
}

private class GemmaOnboardingAssistant(
    private val gemmaProvider: () -> GemmaManager = { getGemmaManager() },
    private val modelDownloaderProvider: () -> GemmaModelDownloader = { getGemmaModelDownloader() },
) : OnboardingAssistant {

    private var isReady = false
    private var gemmaManager: GemmaManager? = null
    private var modelDownloader: GemmaModelDownloader? = null

    override suspend fun reply(step: Int, userText: String, draftState: OnboardingChatUiState): String {
        val fallback = scriptedReply(step)
        if (!ensureReady()) return fallback
        return runCatching {
            gemmaManager?.sendMessage(
                systemPrompt = ONBOARDING_SYSTEM_PROMPT,
                userPrompt = buildPrompt(step, userText, draftState),
                temperature = 0.35f,
            ) ?: fallback
        }.getOrNull()?.trim().takeUnless { it.isNullOrBlank() } ?: fallback
    }

    private suspend fun ensureReady(): Boolean {
        if (isReady) return true
        val downloader = runCatching {
            modelDownloader ?: modelDownloaderProvider().also { modelDownloader = it }
        }.getOrNull() ?: return false
        val manager = runCatching {
            gemmaManager ?: gemmaProvider().also { gemmaManager = it }
        }.getOrNull() ?: return false
        if (!downloader.isModelDownloaded()) return false
        return runCatching {
            manager.initialize(downloader.getModelPath())
            isReady = true
            true
        }.getOrDefault(false)
    }

    private fun buildPrompt(step: Int, userText: String, state: OnboardingChatUiState): String {
        val objective = when (step) {
            0 -> "Agradecé su nombre y preguntá solo por cultivos"
            1 -> "Agradecé y preguntá solo por área"
            2 -> "Agradecé y preguntá solo por etapa del cultivo"
            3 -> "Agradecé y explicá brevemente que ahora pediremos ubicación"
            else -> "Mantené foco en onboarding"
        }
        return """
            Objetivo actual: $objective.
            Respuesta del usuario: $userText
            Datos capturados:
            - nombre: ${state.userName ?: ""}
            - cultivos: ${state.userCrops ?: ""}
            - area: ${state.userArea ?: ""}
            - etapa: ${state.userStage ?: ""}
            Respondé con máximo 2 oraciones.
        """.trimIndent()
    }

}

private const val ONBOARDING_SYSTEM_PROMPT = """
Sos AgroGemma, asistente agrícola cálida y concreta.
Mantené respuestas breves, en español rioplatense, y pedí una sola cosa por turno.
No pidas teléfono, contraseña ni crear cuenta.
Objetivo exclusivo: completar onboarding con nombre, cultivos, área, etapa y preparar permiso de ubicación.
"""

private fun scriptedReply(step: Int): String = when (step) {
    0 -> "Mucho gusto. ¿Qué cultivo o cultivos tenés? Podés mencionar más de uno. 🌱"
    1 -> "¡Buenísimo! ¿Cuántas manzanas o hectáreas tiene tu cultivo? Si podés dame las dimensiones por separado mejor. 📐"
    2 -> "¿Cuánto tiempo tiene que lo sembraste? ¿En qué etapa está?\n\n1. Alistando la tierra — preparación y siembra\n2. Saliendo el puyón — nacencia\n3. Poniéndose shule — crecimiento\n4. Dando la flor — floración\n5. Cargando el fruto — llenado\n6. Punto de corte — cosecha"
    3 -> "Para darte consejos más exactos necesito saber dónde están tus cultivos. Así puedo ver la temperatura, lluvias y condiciones de tu zona. 📍"
    else -> "Entendido. Para seguir, necesito saber dónde están tus cultivos así te doy info de tu zona. 📍"
}
