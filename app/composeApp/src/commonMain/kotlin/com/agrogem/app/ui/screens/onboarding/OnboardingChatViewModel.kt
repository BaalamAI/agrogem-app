package com.agrogem.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.agrogem.app.data.GemmaPreparationStateHolder
import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaPreparationStatus
import com.agrogem.app.data.getGemmaManager
import com.agrogem.app.data.getGemmaModelDownloader
import com.agrogem.app.data.geolocation.createGeolocationRepository
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.location.DeviceLocationProvider
import com.agrogem.app.data.location.createDeviceLocationProvider
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.MessageSender

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

private const val LOCATION_RESOLUTION_TIMEOUT_MS = 10_000L

private enum class OnboardingField {
    Name,
    Crops,
    Area,
    Stage,
}

/**
 * Dedicated state holder for the onboarding chat flow.
 *
 * Isolates onboarding-specific state machine and orchestration from the
 * real in-app [com.agrogem.app.ui.screens.chat.ChatViewModel] so the two
 * can evolve independently.
 */
class OnboardingChatViewModel(
    private val assistant: OnboardingAssistant = GemmaOnboardingAssistant(),
    private val geolocationRepository: GeolocationRepository = createGeolocationRepository(),
    private val deviceLocationProvider: DeviceLocationProvider = createDeviceLocationProvider(),
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(OnboardingChatUiState())
    val uiState: StateFlow<OnboardingChatUiState> = _uiState.asStateFlow()

    fun startOnboardingChat() {
        if (_uiState.value.onboardingChatStage != null) return
        _uiState.value = OnboardingChatUiState(
            onboardingChatStage = OnboardingChatStage.Preparing,
            gemmaPreparationStatus = currentPreparationStatus(),
        )
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching { assistant.prepareIfNeeded() }
            val status = currentPreparationStatus()
            _uiState.value = when (status) {
                GemmaPreparationStatus.Ready,
                is GemmaPreparationStatus.Unavailable,
                -> OnboardingChatUiState(
                    messages = listOf(initialAssistantMessage(status)),
                    onboardingChatStage = OnboardingChatStage.Conversation,
                    onboardingStep = 0,
                    userName = null,
                    userCrops = null,
                    userArea = null,
                    userStage = null,
                    locationEnabled = false,
                    locationShared = false,
                    gemmaPreparationStatus = status,
                )

                GemmaPreparationStatus.NotPrepared,
                GemmaPreparationStatus.Downloading,
                GemmaPreparationStatus.Preparing,
                -> _uiState.value.copy(
                    onboardingChatStage = OnboardingChatStage.Preparing,
                    gemmaPreparationStatus = status,
                )
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val currentState = _uiState.value
        if (currentState.onboardingChatStage == null) return
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val latestState = _uiState.value
            _uiState.value = latestState.copy(isResolvingLocation = granted)
            val locationShared = if (!granted) {
                false
            } else {
                runCatching {
                    withTimeout(LOCATION_RESOLUTION_TIMEOUT_MS) {
                        deviceLocationProvider.getCurrentLatLng()
                    }
                }.getOrNull()?.fold(
                    onSuccess = { latLng -> geolocationRepository.reverseGeocode(latLng).isSuccess },
                    onFailure = { false },
                ) == true
            }
            val finalState = _uiState.value
            _uiState.value = finalState.copy(
                onboardingChatStage = OnboardingChatStage.AlertsPreferences,
                locationShared = locationShared,
                isResolvingLocation = false,
            )
        }
    }

    fun continueOnboardingAfterLocationPermission() {
        onLocationPermissionResult(granted = true)
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
        if (currentState.isLoading) return

        val step = currentState.onboardingStep
        val userMessage = ChatMessage(
            id = "onboard_user_${Random.nextLong()}",
            text = trimmed,
            sender = MessageSender.User,
            attachments = emptyList(),
            timestamp = Random.nextLong(),
        )

        val extracted = extractObviousOnboardingFields(trimmed)
        val nextName = when {
            currentState.userName != null -> currentState.userName
            step == 0 -> extracted.name ?: trimmed
            else -> extracted.name
        }
        val nextCrops = when {
            currentState.userCrops != null -> currentState.userCrops
            step == 1 -> extracted.crops ?: trimmed
            else -> extracted.crops
        }
        val nextArea = when {
            currentState.userArea != null -> currentState.userArea
            step == 2 -> extracted.area ?: trimmed
            else -> extracted.area
        }
        val nextUserStage = when {
            currentState.userStage != null -> currentState.userStage
            step == 3 -> extracted.stage ?: trimmed
            else -> extracted.stage
        }

        val nextStep = nextMissingStep(
            userName = nextName,
            userCrops = nextCrops,
            userArea = nextArea,
            userStage = nextUserStage,
        )
        val nextStage = if (nextStep >= 4) {
            OnboardingChatStage.AwaitingLocationPermission
        } else {
            OnboardingChatStage.Conversation
        }

        val baseNextState = currentState.copy(
            messages = currentState.messages + userMessage,
            inputText = "",
            onboardingStep = nextStep,
            onboardingChatStage = OnboardingChatStage.Conversation,
            userName = nextName,
            userCrops = nextCrops,
            userArea = nextArea,
            userStage = nextUserStage,
        )
        val thinkingMessageId = "onboard_assistant_thinking_${Random.nextLong()}"
        val thinkingState = baseNextState.copy(
            gemmaPreparationStatus = currentPreparationStatus(),
            isLoading = true,
            messages = baseNextState.messages + ChatMessage(
                id = thinkingMessageId,
                text = "AgroGemma está pensando...",
                sender = MessageSender.Assistant,
                attachments = emptyList(),
                timestamp = Random.nextLong(),
                isStreaming = true,
            ),
        )
        _uiState.value = thinkingState

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val replyText = assistant.reply(
                step = nextStep,
                userText = trimmed,
                draftState = baseNextState,
            )
            _uiState.value = thinkingState.copy(
                gemmaPreparationStatus = currentPreparationStatus(),
                isLoading = false,
                onboardingChatStage = nextStage,
                messages = thinkingState.messages.replaceMessage(
                    messageId = thinkingMessageId,
                    replacement = ChatMessage(
                        id = "onboard_assistant_${Random.nextLong()}",
                        text = replyText,
                        sender = MessageSender.Assistant,
                        attachments = emptyList(),
                        timestamp = Random.nextLong(),
                    ),
                ),
            )
        }
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    override fun onCleared() {
        scope.coroutineContext.cancel()
    }

    private fun currentPreparationStatus(): GemmaPreparationStatus =
        runCatching { assistant.preparationStatus.value }
            .getOrDefault(GemmaPreparationStatus.NotPrepared)

    private fun initialAssistantMessage(status: GemmaPreparationStatus): ChatMessage {
        val text = when (status) {
            GemmaPreparationStatus.Ready -> "Para empezar, ¿cómo te llamás? 😊"
            is GemmaPreparationStatus.Unavailable -> "AgroGemma no está disponible ahora mismo, pero igual te voy guiando paso a paso. Para empezar, ¿cómo te llamás? 😊"
            GemmaPreparationStatus.NotPrepared,
            GemmaPreparationStatus.Downloading,
            GemmaPreparationStatus.Preparing,
            -> "Para empezar, ¿cómo te llamás? 😊"
        }
        return assistantMessage(text)
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
    data object Preparing : OnboardingChatStage
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
    val gemmaPreparationStatus: GemmaPreparationStatus = GemmaPreparationStatus.NotPrepared,
    val isLoading: Boolean = false,
    val isResolvingLocation: Boolean = false,
) {

    /**
     * Derives onboarding progress from message count and stage.
     * Progress advances incrementally with each conversation exchange,
     * then jumps to stage-based milestones for location, alerts, and final.
     */
    val onboardingProgress: Float
        get() = when (onboardingChatStage) {
            is OnboardingChatStage.Preparing -> 0.04f
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

private fun List<ChatMessage>.replaceMessage(messageId: String, replacement: ChatMessage): List<ChatMessage> =
    map { message -> if (message.id == messageId) replacement else message }

interface OnboardingAssistant {
    val preparationStatus: StateFlow<GemmaPreparationStatus>
    suspend fun prepareIfNeeded()
    suspend fun reply(step: Int, userText: String, draftState: OnboardingChatUiState): String
}

private class GemmaOnboardingAssistant(
    private val gemmaProvider: () -> GemmaManager = { getGemmaManager() },
    private val preparationProvider: () -> GemmaPreparationStateHolder = {
        GemmaPreparationStateHolder(getGemmaManager(), getGemmaModelDownloader())
    },
) : OnboardingAssistant {

    private var preparationHolder: GemmaPreparationStateHolder? = null
    private var gemmaManager: GemmaManager? = null

    override val preparationStatus: StateFlow<GemmaPreparationStatus>
        get() = holder().status

    override suspend fun prepareIfNeeded() {
        ensureReady()
    }

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
        val holder = runCatching { holder() }.getOrNull() ?: return false

        gemmaManager = runCatching {
            gemmaManager ?: gemmaProvider().also { gemmaManager = it }
        }.getOrNull()

        return holder.ensureReady()
    }

    private fun holder(): GemmaPreparationStateHolder =
        preparationHolder ?: preparationProvider().also { preparationHolder = it }

    private fun buildPrompt(step: Int, userText: String, state: OnboardingChatUiState): String {
        val objective = when (step) {
            1 -> "Agradecé y preguntá solo por cultivos. Si ya mencionó alguno, podés nombrarlo y pedir que complete si falta algo."
            2 -> "Agradecé y preguntá solo por el área. Sé explícita: preguntá cuántas manzanas o hectáreas tiene cada cultivo y aclarale que, si puede, te diga las dimensiones por separado de cada uno."
            3 -> "Agradecé y preguntá solo por la etapa del cultivo. Sé explícita y ofrecé opciones claras como: alistando la tierra (preparación y siembra), saliendo el puyón (nacencia), poniéndose shule (crecimiento), dando la flor (floración), cargando el fruto (llenado), punto de corte (cosecha)."
            4 -> "Agradecé y explicá brevemente que ahora pediremos ubicación"
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

private data class OnboardingExtractedFields(
    val name: String? = null,
    val crops: String? = null,
    val area: String? = null,
    val stage: String? = null,
)

private fun nextMissingStep(
    userName: String?,
    userCrops: String?,
    userArea: String?,
    userStage: String?,
): Int = when {
    userName.isNullOrBlank() -> 0
    userCrops.isNullOrBlank() -> 1
    userArea.isNullOrBlank() -> 2
    userStage.isNullOrBlank() -> 3
    else -> 4
}

private fun extractObviousOnboardingFields(text: String): OnboardingExtractedFields {
    val lower = text.lowercase()
    return OnboardingExtractedFields(
        name = extractName(text),
        crops = extractCrops(text, lower),
        area = extractArea(text, lower),
        stage = extractStage(text, lower),
    )
}

private fun extractName(text: String): String? {
    val pattern = Regex("(?i)(?:me\\s+llamo|soy)\\s+([a-záéíóúñ][a-záéíóúñ' -]{1,40})")
    val value = pattern.find(text)?.groupValues?.getOrNull(1)?.trim() ?: return null
    return value.takeIf { it.length >= 2 }
}

private fun extractCrops(text: String, lower: String): String? {
    val startsWithCropsSignal = listOf("tengo", "cultivo", "siembro", "sembré", "sembrado", "produzco").any {
        lower.contains(it)
    }
    if (!startsWithCropsSignal) return null
    return text.trim().takeIf { it.isNotBlank() }
}

private fun extractArea(text: String, lower: String): String? {
    val hasNumber = Regex("\\d").containsMatchIn(text)
    val hasAreaUnit = listOf("ha", "hect", "m2", "m²", "manzana", "acre", "acres").any { lower.contains(it) }
    return text.trim().takeIf { hasNumber && hasAreaUnit }
}

private fun extractStage(text: String, lower: String): String? {
    val stageKeywords = listOf(
        "alistando", "preparación", "preparacion", "siembra", "nacencia", "puyón", "puyon",
        "crecimiento", "floración", "floracion", "llenado", "cosecha", "etapa",
    )
    return text.trim().takeIf { stageKeywords.any { keyword -> lower.contains(keyword) } }
}

private fun scriptedReply(step: Int): String = when (step) {
    1 -> "Mucho gusto. ¿Qué cultivo o cultivos tenés? Podés mencionar más de uno. 🌱"
    2 -> "¡Buenísimo! ¿Cuántas manzanas o hectáreas tiene cada cultivo? Si podés, decime las dimensiones por separado; o sea, cuánto mide cada cultivo. 📐"
    3 -> "¿En qué etapa está tu cultivo? Puede ser:\n\n1. Alistando la tierra — preparación y siembra\n2. Saliendo el puyón — nacencia\n3. Poniéndose shule — crecimiento\n4. Dando la flor — floración\n5. Cargando el fruto — llenado\n6. Punto de corte — cosecha"
    4 -> "Para darte consejos más exactos necesito saber dónde están tus cultivos. Así puedo ver la temperatura, lluvias y condiciones de tu zona. 📍"
    else -> "Entendido. Para seguir, necesito saber dónde están tus cultivos así te doy info de tu zona. 📍"
}
