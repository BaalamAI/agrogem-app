package com.agrogem.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.GemmaPreparationStateHolder
import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaModelDownloader
import com.agrogem.app.data.SpeechSynthesizer
import com.agrogem.app.data.SpeechRecognizer
import com.agrogem.app.data.chat.domain.ChatFailure
import com.agrogem.app.data.chat.domain.ChatRepository
import com.agrogem.app.data.chat.domain.ChatSendResult
import com.agrogem.app.data.connectivity.ConnectivityMonitor
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.risk.domain.DiseaseRisk
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.risk.domain.RiskSeverity
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.weather.domain.WeatherRepository
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.session.SessionSnapshot
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

sealed class ChatEffect {
    data object SessionExpired : ChatEffect()
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val analysisId: String? = null,
    private val diagnosis: DiagnosisResult? = null,
    private val gemmaManager: GemmaManager? = null,
    private val gemmaModelDownloader: GemmaModelDownloader? = null,
    private val gemmaPreparationStateHolder: GemmaPreparationStateHolder? = null,
    private val geolocationRepository: GeolocationRepository? = null,
    private val riskRepository: RiskRepository? = null,
    private val weatherRepository: WeatherRepository? = null,
    private val soilRepository: SoilRepository? = null,
    private val connectivityMonitor: ConnectivityMonitor? = null,
    private val sessionLocalStore: SessionLocalStore? = null,
    private val speechRecognizer: SpeechRecognizer? = null,
    private val speechSynthesizer: SpeechSynthesizer? = null,
) : ViewModel() {

    private val gemmaPreparation = gemmaPreparationStateHolder
        ?: if (gemmaManager != null && gemmaModelDownloader != null) {
            GemmaPreparationStateHolder(gemmaManager = gemmaManager, modelDownloader = gemmaModelDownloader)
        } else {
            null
        }

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effectChannel = Channel<ChatEffect>(Channel.BUFFERED)
    val effects: Flow<ChatEffect> = _effectChannel.receiveAsFlow()

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



    private fun buildBackendFallbackText(userText: String, diagnosis: DiagnosisResult): String {
        return buildString {
            appendLine("[Contexto del análisis previo]")
            appendLine("- Plaga/Enfermedad: ${diagnosis.pestName}")
            appendLine("- Severidad: ${diagnosis.severity}")
            appendLine("- Área afectada: ${diagnosis.affectedArea}")
            appendLine("- Causa: ${diagnosis.cause}")
            appendLine("- Diagnóstico: ${diagnosis.diagnosisText}")
            if (diagnosis.treatmentSteps.isNotEmpty()) {
                appendLine("- Tratamiento:")
                diagnosis.treatmentSteps.forEachIndexed { index, step ->
                    appendLine("  ${index + 1}. $step")
                }
            }
            appendLine("[/Contexto del análisis previo]")
            appendLine()
            append(userText)
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
            is ChatEvent.VoicePermissionDenied -> handleVoicePermissionDenied()
            is ChatEvent.DismissError -> handleDismissError()
            is ChatEvent.ToggleThinking -> handleToggleThinking(event.enabled)
            is ChatEvent.PlayAssistantMessage -> handlePlayAssistantMessage(event.messageId)
        }
    }

    override fun onCleared() {
        speechSynthesizer?.stop()
        super.onCleared()
    }

    private fun handleInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    private fun handleSendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()

        if (text.isEmpty() && currentState.attachments.isEmpty()) return

        val optimisticMessage = ChatMessage(
            id = "opt_${Random.nextLong()}",
            text = text,
            sender = MessageSender.User,
            attachments = currentState.attachments,
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )

        _uiState.value = currentState.copy(
            messages = currentState.messages + optimisticMessage,
            inputText = "",
            attachments = emptyList(),
            showAttachmentMenu = false,
            isLoading = true,
            error = null,
        )

        val mode = currentState.mode
        viewModelScope.launch {
            trySendGemmaOrFallback(mode, text, currentState.attachments)
        }
    }

    private suspend fun sendViaBackend(
        text: String,
        attachments: List<ChatAttachment>,
        mode: ChatMode,
    ) {
        val enrichedText = when (mode) {
            is ChatMode.AnalysisSeeded -> buildBackendFallbackText(text, mode.diagnosis)
            ChatMode.Blank -> text
        }
        val result = chatRepository.sendMessage(
            text = enrichedText,
            attachments = attachments,
            mode = mode,
        )
        when (result) {
            is ChatSendResult.Success -> {
                val assistantMessages = result.messages.filter { it.sender == MessageSender.Assistant }
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessages,
                    isLoading = false,
                )
            }
            is ChatSendResult.Failure -> {
                val errorText = when (result.reason) {
                    is ChatFailure.SessionExpired -> "La sesión ha expirado, iniciá sesión de nuevo"
                    is ChatFailure.Network -> "Error de red. Verificá tu conexión e intentá de nuevo."
                    is ChatFailure.Server -> "Error del servidor. Intentá de nuevo en unos momentos."
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorText,
                )
                if (result.reason is ChatFailure.SessionExpired) {
                    _effectChannel.send(ChatEffect.SessionExpired)
                }
            }
        }
    }

    private suspend fun trySendGemmaOrFallback(
        mode: ChatMode,
        text: String,
        attachments: List<ChatAttachment>,
    ) {
        val manager = gemmaManager
        val holder = gemmaPreparation

        val canUseGemma = manager != null && holder != null

        if (!canUseGemma) {
            sendViaBackend(text, attachments, mode)
            return
        }

        if (!holder.ensureReady()) {
            sendViaBackend(text, attachments, mode)
            return
        }

        val assistantMessageId = "assistant_${Random.nextLong()}"
        val assistantPlaceholder = ChatMessage(
            id = assistantMessageId,
            text = "...",
            sender = MessageSender.Assistant,
            attachments = emptyList(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            isStreaming = true,
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + assistantPlaceholder,
        )

        val systemPrompt = when (mode) {
            ChatMode.Blank -> {
                val envContext = fetchGeneralEnvironmentContext()
                val profileContext = fetchOnboardingProfileContext()
                buildGeneralSystemPrompt(envContext, profileContext)
            }
            is ChatMode.AnalysisSeeded -> {
                val pestRiskContext = fetchPestRiskContext(mode.diagnosis)
                val diseaseRiskContext = fetchDiseaseRiskContext(mode.diagnosis)
                buildAnalysisSystemPrompt(mode.diagnosis, pestRiskContext, diseaseRiskContext)
            }
        }

        try {
            val imageUris = attachments
                .filterIsInstance<ChatAttachment.Image>()
                .map { it.uri }

            var accumulatedText = ""
            manager.sendMessageStream(
                systemPrompt = systemPrompt,
                userPrompt = text,
                images = imageUris,
                temperature = 0.4f,
            ).collect { response ->
                if (response.text.isNotEmpty()) {
                    accumulatedText += response.text
                    updateAssistantMessage(assistantMessageId, accumulatedText, response.thought, response.isDone)
                } else if (response.isDone) {
                    updateAssistantMessage(assistantMessageId, accumulatedText, null, true)
                }
            }
            updateAssistantMessage(assistantMessageId, accumulatedText, null, true)
            _uiState.value = _uiState.value.copy(isLoading = false)
        } catch (e: Exception) {
            val messagesWithoutPlaceholder = _uiState.value.messages.filter { it.id != assistantMessageId }
            _uiState.value = _uiState.value.copy(messages = messagesWithoutPlaceholder)
            sendViaBackend(text, attachments, mode)
        }
    }

    private fun buildGeneralSystemPrompt(
        environmentContext: String? = null,
        onboardingProfileContext: String? = null,
    ): String {
        return buildString {
            append("Eres un asistente agronómico experto. ")
            append("Responde de forma clara, práctica y basada en evidencia sobre agricultura, ")
            append("manejo de cultivos, plagas, enfermedades, suelo, clima y mejores prácticas agrícolas. ")
            append("Si no tenés información suficiente, pedí más detalles al usuario. ")
            append("Responde en español y mantén un tono profesional pero accesible.")
            if (!environmentContext.isNullOrBlank()) {
                append(environmentContext)
            }
            if (!onboardingProfileContext.isNullOrBlank()) {
                append(onboardingProfileContext)
            }
        }
    }

    private suspend fun fetchOnboardingProfileContext(): String? {
        val snapshot = sessionLocalStore?.read() ?: return null
        return snapshot.toOnboardingContextBlock()
    }

    private fun SessionSnapshot.toOnboardingContextBlock(): String? {
        val compactFields = buildList {
            name?.trim().takeUnless { it.isNullOrEmpty() }?.let { add("- Nombre: $it") }
            crops?.trim().takeUnless { it.isNullOrEmpty() }?.let { add("- Cultivos: $it") }
            area?.trim().takeUnless { it.isNullOrEmpty() }?.let { add("- Área: $it") }
            stage?.trim().takeUnless { it.isNullOrEmpty() }?.let { add("- Etapa: $it") }
        }
        if (compactFields.isEmpty()) return null
        return buildString {
            append("\n\nContexto base del productor (onboarding):\n")
            compactFields.forEach { appendLine(it) }
        }
    }

    private suspend fun fetchGeneralEnvironmentContext(): String? {
        val monitor = connectivityMonitor ?: return null
        if (!monitor.isOnline()) return null

        val geoRepo = geolocationRepository ?: return null
        val location = geoRepo.observeResolvedLocation().first() ?: return null

        val weatherRepo = weatherRepository ?: return null
        val soilRepo = soilRepository ?: return null

        val weather = weatherRepo.getCurrentWeather(location.coordinates).getOrNull()
        val soil = soilRepo.getSoil(location.coordinates).getOrNull()

        if (weather == null && soil == null) return null

        return buildString {
            append("\n\nContexto del campo:\n")
            append("- Ubicación: ${location.display.primary}\n")
            weather?.let {
                append("- Clima: ${it.temperatureCelsius}, ${it.humidity} humedad, ${it.description}\n")
            }
            soil?.let {
                append("- Suelo: textura ${it.dominantTexture}, pH ${it.summary.topHorizonPh}")
                if (it.interpretation.isNotBlank()) {
                    append(" — ${it.interpretation}")
                }
                append("\n")
            }
        }
    }

    private fun buildAnalysisSystemPrompt(
        diagnosis: DiagnosisResult,
        pestRiskContext: String? = null,
        diseaseRiskContext: String? = null,
    ): String {
        return buildString {
            append("Eres un asistente agronómico experto. ")
            append("El usuario está consultando sobre un diagnóstico previo. ")
            append("Responde de forma clara, práctica y contextualizada usando la siguiente información:\n\n")
            append("- Plaga/Enfermedad: ${diagnosis.pestName}\n")
            append("- Confianza del diagnóstico: ${(diagnosis.confidence * 100).toInt()}%\n")
            append("- Severidad: ${diagnosis.severity}\n")
            append("- Área afectada: ${diagnosis.affectedArea}\n")
            append("- Causa: ${diagnosis.cause}\n")
            append("- Diagnóstico: ${diagnosis.diagnosisText}\n")
            append("- Pasos de tratamiento:\n")
            diagnosis.treatmentSteps.forEachIndexed { index, step ->
                append("  ${index + 1}. $step\n")
            }
            if (!pestRiskContext.isNullOrBlank()) {
                append(pestRiskContext)
            }
            if (!diseaseRiskContext.isNullOrBlank()) {
                append(diseaseRiskContext)
            }
            append("\nResponde en español y mantén un tono profesional pero accesible.")
        }
    }

    private suspend fun fetchPestRiskContext(diagnosis: DiagnosisResult? = null): String? {
        val geoRepo = geolocationRepository ?: return null
        val riskRepo = riskRepository ?: return null
        val location = geoRepo.observeResolvedLocation().first() ?: return null
        val risks = riskRepo.getPestRisks(location.coordinates).getOrNull() ?: return null
        if (risks.isEmpty()) return null

        val relevantRisks = if (diagnosis != null) {
            matchDiagnosisToRisks(diagnosis, risks, ::aliasesForPest).ifEmpty {
                risks.filter { it.severity != RiskSeverity.Optimo }
                    .ifEmpty { risks.take(1) }
            }
        } else {
            risks.filter { it.severity != RiskSeverity.Optimo }
                .ifEmpty { risks.take(1) }
        }

        return buildString {
            append("\n\nRiesgos de plagas en la región (${location.display.primary}):\n")
            relevantRisks.forEach { risk ->
                append("- ${risk.displayName}: ${risk.interpretation.ifBlank { risk.severity.name }}\n")
            }
        }
    }

    private suspend fun fetchDiseaseRiskContext(diagnosis: DiagnosisResult? = null): String? {
        val geoRepo = geolocationRepository ?: return null
        val riskRepo = riskRepository ?: return null
        val location = geoRepo.observeResolvedLocation().first() ?: return null
        val risks = riskRepo.getDiseaseRisks(location.coordinates).getOrNull() ?: return null
        if (risks.isEmpty()) return null

        val relevantRisks = if (diagnosis != null) {
            matchDiagnosisToRisks(diagnosis, risks, ::aliasesForDisease).ifEmpty {
                risks.filter { it.severity != RiskSeverity.Optimo }
                    .ifEmpty { risks.take(1) }
            }
        } else {
            risks.filter { it.severity != RiskSeverity.Optimo }
                .ifEmpty { risks.take(1) }
        }

        return buildString {
            append("\n\nRiesgos de enfermedades en la región (${location.display.primary}):\n")
            relevantRisks.forEach { risk ->
                append("- ${risk.displayName}: ${risk.interpretation.ifBlank { risk.severity.name }}\n")
            }
        }
    }

    /**
     * Matches a diagnosis to the most relevant risk entries available.
     * Returns an empty list when no match is found so callers can fall back to generic summaries.
     */
    private fun matchDiagnosisToRisks(
        diagnosis: DiagnosisResult,
        risks: List<DiseaseRisk>,
        aliases: (String) -> List<String>,
    ): List<DiseaseRisk> {
        val searchText = normalizeForMatch("${diagnosis.pestName} ${diagnosis.cause}")
        return risks.filter { risk ->
            aliases(risk.diseaseName).any { alias ->
                searchText.contains(normalizeForMatch(alias))
            }
        }.distinctBy { it.diseaseName }
    }

    private fun normalizeForMatch(text: String): String = text
        .lowercase()
        .fold(StringBuilder()) { acc, char ->
            acc.append(
                when (char) {
                    'á', 'à', 'ä', 'â', 'ã' -> 'a'
                    'é', 'è', 'ë', 'ê' -> 'e'
                    'í', 'ì', 'ï', 'î' -> 'i'
                    'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
                    'ú', 'ù', 'ü', 'û' -> 'u'
                    else -> char
                }
            )
            acc
        }
        .toString()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .trim()

    private fun aliasesForPest(backendKey: String): List<String> = when (backendKey) {
        "spider_mite" -> listOf("acaro", "arana", "spider", "mite", "tetranychus", "arana roja", "red spider", "acaro rojo")
        "whitefly" -> listOf("mosca blanca", "whitefly", "white fly", "aleurodido", "aleurodicus", "bemisia", "tabaci")
        "broad_mite" -> listOf("acaro ancho", "broad mite", "polyphagotarsonemus", "latus")
        "white_grub" -> listOf("gusano blanco", "white grub", "coleoptera", "scarab", "melolontha", "phyllo")
        "thrips" -> listOf("thrips", "trips", "frankliniella", "occidentalis")
        "leafminer" -> listOf("minador", "leafminer", "leaf miner", "fly miner", "liriomyza")
        "fall_armyworm" -> listOf("gusano cogollero", "fall armyworm", "spodoptera", "frugiperda", "armyworm", "cogollero")
        "root_knot_nematode" -> listOf("nematodo", "root knot", "meloidogyne", "agalla", "nematodo del nudo")
        "coffee_berry_borer" -> listOf("broca", "coffee berry borer", "hypothenemus", "hampei", "berry borer")
        else -> listOf(backendKey.replace("_", " "))
    }

    private fun aliasesForDisease(backendKey: String): List<String> = when (backendKey) {
        "coffee_rust" -> listOf("roya", "roya del cafe", "roya del café", "hemileia", "vastatrix", "coffee rust")
        "late_blight" -> listOf("tizon tardio", "tizón tardío", "phytophthora", "infestans", "late blight", "mildiu", "mildiú")
        "corn_rust" -> listOf("roya del maiz", "roya del maíz", "puccinia", "corn rust")
        else -> listOf(backendKey.replace("_", " "))
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

    private fun handleDismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun handlePlayAssistantMessage(messageId: String) {
        val currentState = _uiState.value
        val message = currentState.messages.firstOrNull { it.id == messageId } ?: return

        if (message.sender != MessageSender.Assistant) return
        if (message.isStreaming) return

        val text = message.text.trim()
        if (text.isEmpty()) return

        if (currentState.speakingMessageId == messageId) {
            speechSynthesizer?.stop()
            _uiState.value = currentState.copy(speakingMessageId = null)
            return
        }

        val started = speechSynthesizer?.speak(text) == true
        _uiState.value = currentState.copy(
            speakingMessageId = if (started) messageId else null,
        )
    }

    /** Toggles visibility of the attachment menu (gallery/camera options). */
    private fun handleToggleAttachmentMenu(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAttachmentMenu = show)
    }

    /** Transitions voiceState to Listening — orb animation starts. */
    private fun handleStartVoiceInput() {
        speechRecognizer?.cancel()
        speechRecognizer?.startListening(
            onPartialResult = { text ->
                _uiState.value = _uiState.value.copy(inputText = text)
            },
            onFinalResult = { text ->
                _uiState.value = _uiState.value.copy(inputText = text)
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.Error(message))
            },
        )
        _uiState.value = _uiState.value.copy(
            voiceState = VoiceState.Listening(amplitude = 0f),
            error = null,
        )
    }

    /** Returns to Idle without creating a message — user abandoned voice input. */
    private fun handleDismissVoice() {
        speechRecognizer?.cancel()
        _uiState.value = _uiState.value.copy(voiceState = VoiceState.Idle)
    }

    /** Permission denied for microphone — surface error in chat. */
    private fun handleVoicePermissionDenied() {
        _uiState.value = _uiState.value.copy(
            error = "Se requiere permiso de micrófono para usar la voz",
        )
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

    /** Stops recording, commits the user voice message, and sends through the real chat pipeline. */
    private fun handleStopVoiceInput() {
        speechRecognizer?.stopListening()
        val captured = _uiState.value
        val text = captured.inputText.trim()

        // Avoid sending junk when there is no text and audio is still a placeholder
        if (text.isEmpty()) {
            _uiState.value = captured.copy(voiceState = VoiceState.Idle)
            return
        }

        val voiceAttachment = ChatAttachment.Audio(uri = "", durationMs = 0)

        val userMessage = ChatMessage(
            id = "msg_${Random.nextLong()}",
            text = text,
            sender = MessageSender.User,
            attachments = listOf(voiceAttachment),
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )

        _uiState.value = captured.copy(
            messages = captured.messages + userMessage,
            inputText = "",
            attachments = emptyList(),
            voiceState = VoiceState.Idle,
            isLoading = true,
            error = null,
        )

        val mode = captured.mode
        viewModelScope.launch {
            trySendGemmaOrFallback(mode, text, listOf(voiceAttachment))
        }
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

    /** Microphone permission was denied by the user. */
    data object VoicePermissionDenied : ChatEvent

    /** User dismissed the error banner. */
    data object DismissError : ChatEvent

    /** Toggle thinking mode for Gemma responses. */
    data class ToggleThinking(val enabled: Boolean) : ChatEvent

    /** Manual TTS playback for a specific assistant message. */
    data class PlayAssistantMessage(val messageId: String) : ChatEvent
}
