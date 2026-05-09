package com.agrogem.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.agent.ToolCallTracker
import com.agrogem.app.agent.ToolIntentSupervisor
import com.agrogem.app.agent.createAgroGemToolBundle
import com.agrogem.app.data.GemmaChatSession
import com.agrogem.app.data.GemmaModelOption
import com.agrogem.app.data.GemmaPreparation
import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaModelDownloader
import com.agrogem.app.data.InferenceLifecycle
import com.agrogem.app.data.AudioRecorder
import com.agrogem.app.data.SpeechSynthesizer
import com.agrogem.app.data.SpeechRecognizer
import com.agrogem.app.data.chat.domain.ChatFailure
import com.agrogem.app.data.chat.domain.ChatRepository
import com.agrogem.app.data.chat.domain.ChatSendResult
import com.agrogem.app.data.chat.domain.LocalChatRepository
import com.agrogem.app.data.connectivity.ConnectivityMonitor
import com.agrogem.app.data.environment.domain.EnvironmentRepository
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.risk.domain.DiseaseRisk
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.risk.domain.RiskSeverity
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.session.SessionSnapshot
import com.agrogem.app.util.platformLog
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
    private val localChatRepository: LocalChatRepository? = null,
    private val analysisId: String? = null,
    private val diagnosis: DiagnosisResult? = null,
    private val gemmaManager: GemmaManager? = null,
    private val gemmaModelDownloader: GemmaModelDownloader? = null,
    gemmaPreparation: GemmaPreparation? = null,
    private val geolocationRepository: GeolocationRepository? = null,
    private val riskRepository: RiskRepository? = null,
    private val environmentRepository: EnvironmentRepository? = null,
    private val connectivityMonitor: ConnectivityMonitor? = null,
    private val sessionLocalStore: SessionLocalStore? = null,
    private val speechRecognizer: SpeechRecognizer? = null,
    private val speechSynthesizer: SpeechSynthesizer? = null,
    private val audioRecorder: AudioRecorder? = null,
    // VM-scoped tracker. Tools mark their invocations here; the supervisor reads it.
    // Kept per-VM (not a process-wide singleton) so concurrent ChatViewModel instances
    // can't see each other's tool calls. Tests inject a shared tracker so the fake
    // GemmaManager and the VM look at the same state.
    private val toolCallTracker: ToolCallTracker = ToolCallTracker(),
) : ViewModel() {

    private var currentConversationId: String? = null
    private var chatSession: GemmaChatSession? = null
    private var voiceRecordingStartedAtMs: Long? = null
    private var sendJob: kotlinx.coroutines.Job? = null

    private val toolBundle by lazy { createAgroGemToolBundle(toolCallTracker) }

    private val gemmaPreparation: GemmaPreparation? = gemmaPreparation
        ?: if (gemmaManager != null && gemmaModelDownloader != null) {
            GemmaPreparation(gemmaManager = gemmaManager, modelDownloader = gemmaModelDownloader)
        } else {
            null
        }

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effectChannel = Channel<ChatEffect>(Channel.BUFFERED)
    val effects: Flow<ChatEffect> = _effectChannel.receiveAsFlow()

    init {
        // Sweep any chat_message left flagged is_streaming=1 from a previous
        // process that was SIGKILL'd mid-inference. We do this once per process
        // (companion flag) so re-entering chat mid-stream — e.g. the VM is
        // recreated after a config change — doesn't nuke the active message.
        if (!hasSweptOrphans) {
            hasSweptOrphans = true
            runCatching { localChatRepository?.markOrphanStreamingMessagesAsFailed() }
                .onFailure { platformLog("ChatViewModel", "Orphan sweep failed: ${it.message}") }
        }

        // Reflect the persisted/selected model into the UI state so the dropdown
        // shows the correct active option on chat open.
        gemmaPreparation?.let { prep ->
            _uiState.value = _uiState.value.copy(
                selectedModel = prep.selectedModel.value,
                gemmaPreparationStatus = prep.status.value,
                gemmaDownloadProgress = prep.downloadProgress.value,
            )
            viewModelScope.launch {
                prep.selectedModel.collect { option ->
                    _uiState.value = _uiState.value.copy(selectedModel = option)
                }
            }
            viewModelScope.launch {
                prep.status.collect { status ->
                    _uiState.value = _uiState.value.copy(gemmaPreparationStatus = status)
                }
            }
            viewModelScope.launch {
                prep.downloadProgress.collect { progress ->
                    _uiState.value = _uiState.value.copy(gemmaDownloadProgress = progress)
                }
            }
        }
    }

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
            is ChatEvent.NewSession -> handleNewSession()
            is ChatEvent.RemoveAttachment -> handleRemoveAttachment(event.index)
            is ChatEvent.StopGeneration -> handleStopGeneration()
            is ChatEvent.SelectModel -> handleSelectModel(event.option)
            is ChatEvent.ConfirmModelDownload -> handleConfirmModelDownload()
            is ChatEvent.CancelModelDownload -> handleCancelModelDownload()
        }
    }

    private fun handleSelectModel(option: GemmaModelOption) {
        val prep = gemmaPreparation ?: return
        if (option == _uiState.value.selectedModel) return

        // If the model file isn't local yet, ask the user before downloading.
        // (Switching to a different model deletes the previous file, so any
        // switch effectively means a re-download — we still confirm.)
        _uiState.value = _uiState.value.copy(pendingModelSwitch = option)
    }

    private fun handleConfirmModelDownload() {
        val prep = gemmaPreparation ?: return
        val target = _uiState.value.pendingModelSwitch ?: return
        _uiState.value = _uiState.value.copy(
            pendingModelSwitch = null,
            isSwitchingModel = true,
        )
        // Reset chat session — the engine is going to be reinitialized.
        chatSession?.close()
        chatSession = null
        viewModelScope.launch {
            val ok = runCatching { prep.switchModel(target) }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                isSwitchingModel = false,
                error = if (!ok) "No se pudo cambiar el modelo. Verificá tu conexión e intentá de nuevo." else _uiState.value.error,
            )
        }
    }

    private fun handleCancelModelDownload() {
        _uiState.value = _uiState.value.copy(pendingModelSwitch = null)
    }

    private fun handleNewSession() {
        speechSynthesizer?.stop()
        audioRecorder?.cancel()
        voiceRecordingStartedAtMs = null
        chatSession?.close()
        chatSession = null
        currentConversationId = null

        _uiState.value = createInitialState()
    }

    override fun onCleared() {
        // Cancel any in-flight send before closing the session — otherwise the
        // native engine/ thread can keep writing into a closed Conversation and
        // SIGSEGV in liblitertlm_jni.
        sendJob?.cancel()
        sendJob = null
        speechSynthesizer?.stop()
        audioRecorder?.cancel()
        chatSession?.close()
        chatSession = null
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
        val previousJob = sendJob
        sendJob = viewModelScope.launch {
            // Wait for any prior stream to finish before starting a new one. The native
            // LiteRT-LM Conversation cannot run two sendMessage() calls concurrently;
            // overlapping calls null-deref in liblitertlm_jni.
            runCatching { previousJob?.cancelAndJoin() }
            val localConversationId = runCatching {
                val id = resolveConversationId(mode)
                if (id != null) {
                    localChatRepository?.saveMessage(id, optimisticMessage)
                }
                id
            }.getOrElse { error ->
                platformLog("ChatViewModel", "Local chat persistence failed before send: ${error.message}")
                null
            }
            if (localConversationId != null) {
                currentConversationId = localConversationId
            }
            trySendGemmaOrFallback(mode, text, currentState.attachments)
        }
    }

    private fun handleStopGeneration() {
        val jobToCancel = sendJob
        sendJob = null
        val sessionToClose = chatSession
        chatSession = null
        val current = _uiState.value
        val messages = current.messages.toMutableList()
        val streamingIndex = messages.indexOfLast { it.isStreaming }
        if (streamingIndex != -1) {
            val stopped = messages[streamingIndex].copy(isStreaming = false, streamStartedAt = null)
            messages[streamingIndex] = stopped
            // Persist the stopped state so the orphan sweep on next launch doesn't
            // re-overwrite the user-visible content with the "interrupted" copy.
            currentConversationId?.let { conversationId ->
                runCatching { localChatRepository?.saveMessage(conversationId, stopped) }
                    .onFailure { platformLog("ChatViewModel", "Stop-state persist failed: ${it.message}") }
            }
        }
        _uiState.value = current.copy(messages = messages, isLoading = false)
        // Drop the Gemma session so the next turn starts on a fresh Conversation.
        // Joining the cancelled job before close lets the native engine wind down
        // cleanly; otherwise the engine/ thread can write into stale Kotlin state
        // and SIGSEGV in liblitertlm_jni.
        viewModelScope.launch {
            runCatching { jobToCancel?.cancelAndJoin() }
            runCatching { sessionToClose?.close() }
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
                currentConversationId?.let { conversationId ->
                    assistantMessages.forEach { message ->
                        localChatRepository?.saveMessage(conversationId, message)
                    }
                }
                val newMessages = _uiState.value.messages + assistantMessages
                _uiState.value = _uiState.value.copy(
                    messages = newMessages,
                    isLoading = false,
                    contextWarning = computeContextWarning(newMessages),
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

        // Acquire foreground-service slot for the entire Gemma path. Released in
        // the matching finally below so coroutine cancellation also tears it down.
        // This protects the process from Android LMK during the multi-second
        // engine init / vision prefill / thinking phases.
        InferenceLifecycle.start("Procesando con Gemma…")
        try {

        toolCallTracker.reset()
        val assistantMessageId = "assistant_${Random.nextLong()}"
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val assistantPlaceholder = ChatMessage(
            id = assistantMessageId,
            text = "",
            sender = MessageSender.Assistant,
            attachments = emptyList(),
            timestamp = nowMs,
            isStreaming = true,
            thinkingPhase = ThinkingPhase.Preparing,
            streamStartedAt = nowMs,
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + assistantPlaceholder,
        )

        if (!holder.ensureReady()) {
            val msgs = _uiState.value.messages.filter { it.id != assistantMessageId }
            _uiState.value = _uiState.value.copy(messages = msgs)
            sendViaBackend(text, attachments, mode)
            return
        }
        updateAssistantPhase(assistantMessageId, ThinkingPhase.Thinking)

        // If the user attached images but the engine started in text-only mode (fast cold
        // start), try to lazily upgrade to a vision-capable engine. Only models marked
        // `visionCapable = true` succeed; on text-only finetunes enableVision returns
        // false and we fall back to backend.
        val hasImageAttachments = attachments.any { it is ChatAttachment.Image }
        if (hasImageAttachments && !manager.supportsVision) {
            platformLog("AgroGemTools", "Gemma sin visión: intentando activar modo visión")
            updateAssistantPhase(assistantMessageId, ThinkingPhase.ActivatingVision)
            val visionReady = holder.ensureVisionReady()
            if (!visionReady) {
                platformLog("AgroGemTools", "Modelo no soporta visión: enviando imagen vía backend")
                val msgs = _uiState.value.messages.filter { it.id != assistantMessageId }
                _uiState.value = _uiState.value.copy(messages = msgs)
                sendViaBackend(text, attachments, mode)
                return
            }
            // Engine was swapped — invalidate any open session so we build a fresh one
            // against the new vision-capable engine. This also drops the prior chat KV-cache.
            chatSession?.close()
            chatSession = null
            platformLog("AgroGemTools", "Modo visión activado; sesión anterior descartada")
        }

        platformLog("AgroGemTools", "ChatVM tracker reset, awaiting tools")

        // Persist the streaming placeholder now that we're committed to Gemma
        // (past the ensureReady / vision-upgrade fallbacks). If Android's LMK
        // kills the process mid-thinking, the row exists with is_streaming=1
        // and the next-launch orphan sweep marks it as failed instead of
        // leaving a frozen "thinking…" bubble in the chat history.
        currentConversationId?.let { conversationId ->
            runCatching { localChatRepository?.saveMessage(conversationId, assistantPlaceholder) }
                .onFailure { platformLog("ChatViewModel", "Streaming placeholder persist failed: ${it.message}") }
        }

        try {
            val imageUris = attachments
                .filterIsInstance<ChatAttachment.Image>()
                .map { it.uri }
            val expectedTools = ToolIntentSupervisor.expectedToolsFor(text)

            suspend fun startNewChatSession(): GemmaChatSession {
                val hasImage = imageUris.isNotEmpty()
                val systemPrompt = when {
                    hasImage && mode == ChatMode.Blank -> buildImageDiagnosisSystemPrompt()
                    mode == ChatMode.Blank -> {
                        val envContext = fetchGeneralEnvironmentContext()
                        val profileContext = fetchOnboardingProfileContext()
                        buildGeneralSystemPrompt(envContext, profileContext)
                    }
                    mode is ChatMode.AnalysisSeeded -> {
                        val pestRiskContext = fetchPestRiskContext(mode.diagnosis)
                        val diseaseRiskContext = fetchDiseaseRiskContext(mode.diagnosis)
                        buildAnalysisSystemPrompt(mode.diagnosis, pestRiskContext, diseaseRiskContext)
                    }
                    else -> buildGeneralSystemPrompt()
                }
                return manager.startChatSession(
                    systemPrompt = systemPrompt,
                    temperature = 0.4f,
                    toolBundle = toolBundle,
                ).also { chatSession = it }
            }

            suspend fun sendGemmaAttempt(promptText: String) = coroutineScope {
                var accumulatedText = ""
                var latestThought: String? = null
                var lastEmitMs = 0L
                val throttleMs = 60L
                val toolObserverJob = launch {
                    platformLog("AgroGemTools", "ChatVM observer launched, about to subscribe")
                    toolCallTracker.calledTools.collect { tools ->
                        platformLog("AgroGemTools", "ChatVM observer received tools=$tools (size=${tools.size})")
                        if (tools.isNotEmpty()) {
                            val (cleanText, extractedThought) = stripThinkingTokens(accumulatedText)
                            updateAssistantMessage(
                                id = assistantMessageId,
                                text = cleanText,
                                thought = latestThought ?: extractedThought,
                                isDone = false,
                                toolsUsed = tools.toList(),
                                thinkingPhase = ThinkingPhase.CallingTools,
                            )
                        }
                    }
                }
                try {
                    val session = chatSession ?: startNewChatSession()
                    session.sendMessage(
                        text = promptText,
                        images = imageUris,
                    ).collect { response ->
                        if (response.error != null) {
                            // Surface as exception so the outer catch falls back to backend.
                            throw IllegalStateException("Gemma inference failed: ${response.error}")
                        }
                        if (response.text.isNotEmpty()) {
                            accumulatedText += response.text
                        }
                        if (response.thought != null) {
                            // LiteRT-LM emits each thought channel chunk as a delta token, mirroring
                            // how `response.text` is accumulated above. Concatenating here keeps the
                            // dropdown showing the full reasoning instead of only the last token.
                            latestThought = (latestThought ?: "") + response.thought
                        }
                        val nowMs = Clock.System.now().toEpochMilliseconds()
                        val shouldEmit = response.isDone || (nowMs - lastEmitMs) >= throttleMs
                        if (shouldEmit) {
                            lastEmitMs = nowMs
                            val (cleanText, extractedThought) = stripThinkingTokens(accumulatedText)
                            updateAssistantMessage(
                                id = assistantMessageId,
                                text = cleanText,
                                thought = latestThought ?: extractedThought,
                                isDone = response.isDone,
                                toolsUsed = toolCallTracker.calledTools.value.toList(),
                            )
                        }
                    }
                } finally {
                    toolObserverJob.cancelAndJoin()
                }
                val (cleanText, extractedThought) = stripThinkingTokens(accumulatedText)
                updateAssistantMessage(
                    id = assistantMessageId,
                    text = cleanText,
                    thought = latestThought ?: extractedThought,
                    isDone = true,
                    toolsUsed = toolCallTracker.calledTools.value.toList(),
                )
            }

            sendGemmaAttempt(text)

            if (ToolIntentSupervisor.shouldRetry(expectedTools, toolCallTracker.calledTools.value)) {
                platformLog("AgroGemTools", "Supervisor retry (preserving KV-cache), expected=$expectedTools called=${toolCallTracker.calledTools.value}")
                // Don't close the session — sending the retry prompt as a new turn keeps the
                // KV-cache and roughly halves retry latency vs. rebuilding the session.
                toolCallTracker.reset()
                updateAssistantMessage(
                    id = assistantMessageId,
                    text = "",
                    thought = null,
                    isDone = false,
                    thinkingPhase = ThinkingPhase.Retrying,
                    resetStreamStart = true,
                )
                sendGemmaAttempt(ToolIntentSupervisor.buildRetryPrompt(text, expectedTools))
            }

            val savedAssistant = _uiState.value.messages.firstOrNull { it.id == assistantMessageId }
            if (savedAssistant != null) {
                currentConversationId?.let { conversationId ->
                    localChatRepository?.saveMessage(conversationId, savedAssistant)
                }
            }
            val afterGemma = _uiState.value
            _uiState.value = afterGemma.copy(
                isLoading = false,
                contextWarning = computeContextWarning(afterGemma.messages),
            )
        } catch (e: Throwable) {
            chatSession?.close()
            chatSession = null
            val messagesWithoutPlaceholder = _uiState.value.messages.filter { it.id != assistantMessageId }
            _uiState.value = _uiState.value.copy(messages = messagesWithoutPlaceholder)
            // Remove the persisted streaming placeholder so the next-launch orphan
            // sweep doesn't render an "interrupted" bubble even though the user
            // got a successful backend response from the fallback below.
            runCatching { localChatRepository?.deleteMessage(assistantMessageId) }
                .onFailure { platformLog("ChatViewModel", "Placeholder cleanup failed: ${it.message}") }
            sendViaBackend(text, attachments, mode)
        }

        } finally {
            InferenceLifecycle.stop()
        }
    }

    private fun buildGeneralSystemPrompt(
        environmentContext: String? = null,
        onboardingProfileContext: String? = null,
    ): String {
        return buildString {
            append("<|think|>\n")
            append(ANTI_CHAIN_OF_THOUGHT_RULE)
            append("Eres AgroGem, un asistente agronómico experto especializado en salud de cultivos. ")
            append("Toda respuesta debe orientarse al campo, el cultivo y la toma de decisiones agrícolas. ")
            append("Prioriza detección y explicación de enfermedades, hongos, bacterias, virus, insectos, ácaros, nematodos, plagas, deficiencias y estrés ambiental. ")
            append("Responde con diagnóstico diferencial cuando aplique: síntomas observables, causa probable, factores que lo favorecen y acciones prácticas de manejo. ")
            append("Cuando el usuario use nombres populares o regionales de plagas o enfermedades (p. ej. 'gallina ciega', 'cogollero', 'chamusco', 'palomilla', 'gusano soldado'), identificá el organismo agrícola correspondiente y respondé con información agronómica específica, no con el significado coloquial. ")
            append("Si la pregunta no es agrícola, redirígela brevemente hacia un enfoque útil para agricultura o explica que está fuera del alcance del agente. ")
            append("Si no tenés información suficiente, pedí los datos mínimos: cultivo, etapa, parte afectada, síntomas, avance, ubicación y foto si ayuda. ")
            append("Responde en español y mantén un tono profesional pero accesible.")
            append("\n\nTenés acceso a herramientas que consultan datos reales del campo del usuario: ")
            append("`get_current_weather` (clima actual), `get_soil_profile` (perfil de suelo), ")
            append("`get_pest_risks` (riesgos de plagas), `request_user_location` (pedir GPS al usuario), ")
            append("`resolve_location_by_name` (resolver un lugar por nombre). ")
            append("Usá estas herramientas para enriquecer el contexto agronómico; no reemplazan la observación del cultivo ni autorizan a inventar datos. ")
            append("Cuando el usuario pregunte por clima, suelo, plagas o condiciones que favorecen enfermedades, llamá la herramienta correspondiente en lugar de pedir más detalles. ")
            append("Si una herramienta devuelve un error indicando que la ubicación no está disponible, ")
            append("preferí llamar `request_user_location` primero (le pide GPS al usuario, es lo más preciso). ")
            append("Si el usuario rechaza el permiso o el GPS falla, recién entonces pedile su municipio y país y llamá `resolve_location_by_name`. ")
            append("Después de cualquiera de las dos, volvé a llamar la herramienta original.")
            append("\n\nPolítica de errores de herramientas (CRÍTICO): ")
            append("Cuando una herramienta devuelve un campo `error`, leé el mensaje y seguí la instrucción que contiene. ")
            append("Si dice que verifiques ortografía, pedile al usuario que confirme cómo escribió el lugar y volvé a llamar la herramienta con la corrección. ")
            append("Si dice que falta una ubicación, llamá `request_user_location` (preferido) o pedile al usuario su lugar y llamá `resolve_location_by_name`. ")
            append("Si dice que no hay datos para esa ubicación o que el servicio falló, comunicáselo al usuario con la razón concreta. ")
            append("NUNCA inventes datos numéricos (temperaturas, humedad, pH, riesgos o plagas) que no vinieron de una herramienta. ")
            append("NUNCA presentes un diagnóstico visual como certeza absoluta: separá lo observado, lo probable y lo que falta confirmar. ")
            append("NUNCA te rindas en silencio: si una herramienta falla, siempre respondé al usuario explicando qué pasó y, cuando aplique, pediéndole los datos que necesitás para reintentar.")
            append("\n\nFormato de respuesta (IMPORTANTE): ")
            append("Escribí como máximo 2 o 3 párrafos cortos. ")
            append("Al final de cada respuesta, planteá 1 o 2 preguntas de seguimiento breves que inviten al usuario a profundizar. ")
            append("Formulá esas preguntas como ofertas concretas: p. ej. '¿Te cuento tres cosas para mejorar tu cultivo?' o '¿Querés que te explique cómo identificar si el problema avanzó?' o '¿Te detallo el tratamiento paso a paso?'. ")
            append("No hagas preguntas abiertas genéricas; cada pregunta debe referirse a algo específico que puedas desarrollar en la siguiente respuesta.")
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

        val env = environmentRepository?.getEnvironment(location.coordinates)?.getOrNull()
            ?: return null

        if (env.weather == null && env.soil == null) return null

        return buildString {
            append("\n\nContexto del campo:\n")
            append("- Ubicación: ${location.display.primary}\n")
            env.weather?.let {
                append("- Clima: ${it.temperatureCelsius}, ${it.humidity} humedad, ${it.description}\n")
            }
            env.soil?.let {
                append("- Suelo: textura ${it.dominantTexture}, pH ${it.summary.topHorizonPh}")
                if (it.interpretation.isNotBlank()) {
                    append(" — ${it.interpretation}")
                }
                append("\n")
            }
        }
    }

    private fun buildImageDiagnosisSystemPrompt(): String {
        return buildString {
            append("<|think|>\n")
            append(ANTI_CHAIN_OF_THOUGHT_RULE)
            append("Eres AgroGem, un experto en diagnóstico visual de salud de cultivos. ")
            append("Analiza la imagen adjunta buscando enfermedades, hongos, bacterias, virus, insectos, ácaros, nematodos, plagas, deficiencias o estrés ambiental. ")
            append("Responde en texto natural, en español, describiendo: qué problema detectas, la severidad estimada, la causa probable y los pasos de tratamiento recomendados. ")
            append("Sé claro y práctico; no incluyas JSON ni formato estructurado. ")
            append("Escribí máximo 2 o 3 párrafos. Al final planteá 1 o 2 preguntas breves de seguimiento como ofertas concretas: p. ej. '¿Te explico el tratamiento paso a paso?' o '¿Querés que identifique si hay más plantas afectadas?'.")
        }
    }

    private fun buildAnalysisSystemPrompt(
        diagnosis: DiagnosisResult,
        pestRiskContext: String? = null,
        diseaseRiskContext: String? = null,
    ): String {
        return buildString {
            append("<|think|>\n")
            append(ANTI_CHAIN_OF_THOUGHT_RULE)
            append("Eres AgroGem, un asistente agronómico experto especializado en salud de cultivos. ")
            append("El usuario está consultando sobre un diagnóstico previo de enfermedad, plaga o daño en cultivo. ")
            append("Responde de forma clara, práctica y contextualizada. Explica qué significa el diagnóstico, por qué ocurre, qué señales debe revisar y qué acciones agrícolas puede tomar. ")
            append("Separá diagnóstico probable, factores de riesgo y manejo recomendado; no afirmes certeza absoluta si la evidencia no alcanza. ")
            append("Usa la siguiente información:\n\n")
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
            append("\nResponde en español y mantén un tono profesional pero accesible. ")
            append("Escribí máximo 2 o 3 párrafos cortos. Al final planteá 1 o 2 preguntas breves de seguimiento formuladas como ofertas concretas: p. ej. '¿Te detallo el plan de tratamiento?' o '¿Querés que revise si las condiciones climáticas actuales favorecen la enfermedad?'.")
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


    private fun computeContextWarning(messages: List<ChatMessage>): ContextWarningLevel {
        val conversationChars = messages.sumOf { it.text.length + (it.thought?.length ?: 0) }
        val estimatedTokens = conversationChars / CHARS_PER_TOKEN + SYSTEM_PROMPT_TOKEN_ESTIMATE
        return when {
            estimatedTokens >= CONTEXT_WINDOW_TOKENS * 0.90 -> ContextWarningLevel.Critical
            estimatedTokens >= CONTEXT_WINDOW_TOKENS * 0.72 -> ContextWarningLevel.Strong
            estimatedTokens >= CONTEXT_WINDOW_TOKENS * 0.50 -> ContextWarningLevel.Mild
            else -> ContextWarningLevel.None
        }
    }

    /**
     * Strips thinking-process tokens from the raw model output so they never
     * appear in the visible chat bubble.
     *
     * Gemma thinking models emit their reasoning wrapped in <|think|> delimiters:
     *   <|think|>[thought content]<|think|>[actual response]
     * Some variants use different open tokens (e.g. <|chanel |>, <|channel|>),
     * so the fallback also handles any <|...|>-prefixed block separated from the
     * response by a blank line.
     *
     * Returns (cleanResponse, thought?) — thought is null when nothing was stripped.
     */
    private fun stripThinkingTokens(raw: String): Pair<String, String?> {
        val thinkToken = "<|think|>"
        val firstIdx = raw.indexOf(thinkToken)
        if (firstIdx != -1) {
            val contentStart = firstIdx + thinkToken.length
            val closeIdx = raw.indexOf(thinkToken, contentStart)
            if (closeIdx != -1) {
                val thought = raw.substring(contentStart, closeIdx).trim()
                val response = raw.substring(closeIdx + thinkToken.length).trim()
                if (response.isNotBlank()) return response to thought.ifBlank { null }
            }
            // Only one delimiter found: strip everything after it.
            val response = (raw.take(firstIdx) + raw.drop(firstIdx + thinkToken.length)).trim()
            return response to null
        }

        // Handle <|channel>thought\n[reasoning]<channel|>[response] format (Gemma 4 E2B,
        // per the official model card on huggingface.co/google/gemma-4-E2B-it).
        val channelThought = "<|channel>thought"
        val channelClose = "<channel|>"
        if (raw.startsWith(channelThought)) {
            val afterToken = raw.substring(channelThought.length).trimStart('\n', ' ')
            val closeIdx = afterToken.indexOf(channelClose)
            if (closeIdx != -1) {
                val thought = afterToken.substring(0, closeIdx).trim()
                val response = afterToken.substring(closeIdx + channelClose.length).trim()
                if (response.isNotBlank()) return response to thought.ifBlank { null }
                return "" to thought.ifBlank { null }
            }
            // Fallback: some E2B outputs separate thought from response with a blank line
            // instead of the explicit close token. Honor that legacy shape too.
            val blankLine = afterToken.indexOf("\n\n")
            if (blankLine != -1) {
                val thought = afterToken.substring(0, blankLine).trim()
                val response = afterToken.substring(blankLine).trim()
                if (response.isNotBlank()) return response to thought.ifBlank { null }
            }
            // Still accumulating the thinking block — hide from main text.
            return "" to afterToken.ifBlank { null }
        }

        // Fallback: handle <|…|> tokens at the very start of the output.
        val specialToken = Regex("""<\|[^\n|]*\|>""")
        if (specialToken.containsMatchIn(raw.take(80))) {
            // Separate thinking block from response at the first blank line.
            val blankLine = raw.indexOf("\n\n")
            if (blankLine != -1) {
                val possibleThought = raw.substring(0, blankLine).trim()
                val possibleResponse = raw.substring(blankLine).trim()
                if (possibleResponse.isNotBlank()) {
                    return possibleResponse to possibleThought.ifBlank { null }
                }
            }
            // Still accumulating — hide token markers, don't show as main text.
            return "" to specialToken.replace(raw, "").trim().ifBlank { null }
        }

        // Last-resort fallback: Gemma 4 E2B sometimes verbalizes its reasoning as a
        // numbered list with English bold headers ("1. **Analyze the User Input:** …")
        // without ever emitting <|think|> markers. Detect that pattern and route the
        // whole leading block to `thought` instead of the chat bubble.
        val markerHits = COT_LEAK_MARKERS.count { raw.contains(it, ignoreCase = true) }
        val startsWithNumberedHeader = raw.trimStart()
            .take(160)
            .matches(Regex("""^\d+\.\s+\*?\*?[A-Z][\s\S]*"""))
        if (markerHits >= 2 && startsWithNumberedHeader) {
            val lines = raw.lines()
            var lastReasoningIdx = -1
            for ((idx, line) in lines.withIndex()) {
                val trimmed = line.trim()
                val isNumbered = trimmed.matches(Regex("""^\d+\.\s+.*"""))
                val isBullet = trimmed.startsWith("•") ||
                    trimmed.startsWith("- ") ||
                    trimmed.startsWith("* ") ||
                    trimmed.startsWith("◦ ")
                val isIndentedContinuation = (line.startsWith("   ") || line.startsWith("\t")) && trimmed.isNotEmpty()
                val containsMarker = COT_LEAK_MARKERS.any { line.contains(it, ignoreCase = true) }
                if (isNumbered || isBullet || isIndentedContinuation || containsMarker) {
                    lastReasoningIdx = idx
                }
            }
            if (lastReasoningIdx >= 0) {
                val thought = lines.take(lastReasoningIdx + 1).joinToString("\n").trim()
                val response = lines.drop(lastReasoningIdx + 1).joinToString("\n").trim()
                return response to thought.ifBlank { null }
            }
        }

        return raw to null
    }

    companion object {
        // Process-wide latch so the orphan sweep runs at most once per app launch,
        // not on every VM recreation (config changes mid-stream would otherwise
        // nuke the active message). The sweep is idempotent so a benign double-run
        // from a race wouldn't cause harm — we don't need volatile semantics.
        private var hasSweptOrphans: Boolean = false

        private const val CONTEXT_WINDOW_TOKENS = 8192
        private const val SYSTEM_PROMPT_TOKEN_ESTIMATE = 800
        private const val CHARS_PER_TOKEN = 4

        private const val ANTI_CHAIN_OF_THOUGHT_RULE =
            "REGLA ABSOLUTA: Respondé siempre directamente al usuario en español, sin razonamiento visible. " +
                "NUNCA escribas pasos numerados de pensamiento ni encabezados como 'Analyze the User Input', " +
                "'Determine the Goal', 'Check Tool Availability' o 'Formulate the Response Strategy'. " +
                "NUNCA expliques tu proceso interno ni tus consideraciones meta. Si necesitás pensar, hacelo " +
                "internamente y entregá solo la respuesta final.\n\n"

        private val COT_LEAK_MARKERS = listOf(
            "Analyze the User Input",
            "Determine the Goal",
            "Check Tool Availability",
            "Formulate the Response Strategy",
            "Response Strategy",
        )
    }

    private fun updateAssistantMessage(
        id: String,
        text: String,
        thought: String?,
        isDone: Boolean,
        toolsUsed: List<String> = emptyList(),
        thinkingPhase: ThinkingPhase? = null,
        resetStreamStart: Boolean = false,
    ) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == id }
        if (index != -1) {
            if (toolsUsed.isNotEmpty()) {
                platformLog("AgroGemTools", "ChatVM updateMsg id=$id toolsUsed=$toolsUsed isDone=$isDone")
            }
            val current = currentMessages[index]
            // Clear the timer when the stream completes; mint a fresh start when the
            // supervisor decides to retry (so the counter restarts from 0:00 instead
            // of accumulating from the prior attempt).
            val newStreamStartedAt = when {
                isDone -> null
                resetStreamStart -> Clock.System.now().toEpochMilliseconds()
                else -> current.streamStartedAt
            }
            currentMessages[index] = current.copy(
                text = text,
                thought = thought,
                isStreaming = !isDone,
                toolsUsed = toolsUsed,
                thinkingPhase = thinkingPhase ?: current.thinkingPhase,
                streamStartedAt = newStreamStartedAt,
            )
            _uiState.value = _uiState.value.copy(messages = currentMessages)
        } else {
            platformLog("AgroGemTools", "ChatVM updateMsg MISS — id=$id not found")
        }
    }

    private fun updateAssistantPhase(id: String, phase: ThinkingPhase) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == id }
        if (index != -1) {
            currentMessages[index] = currentMessages[index].copy(thinkingPhase = phase)
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
        audioRecorder?.cancel()
        audioRecorder?.start()
        voiceRecordingStartedAtMs = Clock.System.now().toEpochMilliseconds()
        speechRecognizer?.start(
            onPartialResult = { text ->
                _uiState.value = _uiState.value.copy(inputText = text)
            },
            onFinalResult = { text ->
                _uiState.value = _uiState.value.copy(inputText = text)
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.Error(message))
            },
            onAmplitudeUpdate = { amplitude ->
                val current = _uiState.value.voiceState
                if (current is VoiceState.Listening) {
                    _uiState.value = _uiState.value.copy(
                        voiceState = current.copy(amplitude = amplitude),
                    )
                }
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
        audioRecorder?.cancel()
        voiceRecordingStartedAtMs = null
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

    /** Removes a pending attachment by index. */
    private fun handleRemoveAttachment(index: Int) {
        val current = _uiState.value
        if (index < 0 || index >= current.attachments.size) return
        _uiState.value = current.copy(
            attachments = current.attachments.filterIndexed { i, _ -> i != index },
        )
    }

    /** Stops recording, commits the user voice message, and sends through the real chat pipeline. */
    private fun handleStopVoiceInput() {
        speechRecognizer?.stop()
        val audioUri = audioRecorder?.stop()
        val durationMs = voiceRecordingStartedAtMs
            ?.let { startedAt -> Clock.System.now().toEpochMilliseconds() - startedAt }
            ?.coerceAtLeast(0)
            ?: 0
        voiceRecordingStartedAtMs = null
        val captured = _uiState.value
        val text = captured.inputText.trim()

        if (text.isEmpty() && audioUri == null) {
            _uiState.value = captured.copy(voiceState = VoiceState.Idle)
            return
        }

        val voiceAttachment = ChatAttachment.Audio(uri = audioUri.orEmpty(), durationMs = durationMs)

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

        if (text.isEmpty()) {
            val voiceErrorMsg = (_uiState.value.voiceState as? VoiceState.Error)?.message
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                voiceState = VoiceState.Idle,
                error = voiceErrorMsg ?: "No se pudo transcribir el audio. Escribe tu pregunta.",
            )
            return
        }

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
        currentConversationId = null
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

        currentConversationId = localChatRepository
            ?.getOrCreateAnalysisConversation(analysisId, diagnosis)
            ?.id
    }

    private fun resolveConversationId(mode: ChatMode): String? {
        val repo = localChatRepository ?: return null
        currentConversationId?.let { return it }

        val conversation = when (mode) {
            is ChatMode.AnalysisSeeded -> repo.getOrCreateAnalysisConversation(mode.analysisId, mode.diagnosis)
            ChatMode.Blank -> repo.createBlankConversation()
        }
        return conversation.id
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

    /** Start a fresh chat session, discarding the current conversation. */
    data object NewSession : ChatEvent

    /** Remove a pending attachment by its index in the attachments list. */
    data class RemoveAttachment(val index: Int) : ChatEvent

    /** Cancel an in-progress Gemma inference. */
    data object StopGeneration : ChatEvent

    /** User picked a model option in the dropdown — opens the confirm-download dialog if needed. */
    data class SelectModel(val option: GemmaModelOption) : ChatEvent

    /** User confirmed the model switch/download in the dialog. */
    data object ConfirmModelDownload : ChatEvent

    /** User dismissed the model switch/download dialog. */
    data object CancelModelDownload : ChatEvent
}
