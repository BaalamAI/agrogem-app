package com.agrogem.app.ui.screens.gemma_demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.agent.createAgroGemToolBundle
import com.agrogem.app.agent.toolCallTracker
import com.agrogem.app.data.GemmaChatSession
import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaPreparation
import com.agrogem.app.data.GemmaPreparationStatus
import com.agrogem.app.data.SpeechRecognizer
import com.agrogem.app.data.SpeechSynthesizer
import com.agrogem.app.data.createGemmaManager
import com.agrogem.app.data.createGemmaModelDownloader
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.weather.domain.WeatherRepository
import com.agrogem.app.ui.screens.chat.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

class GemmaDemoViewModel(
    private val gemmaManager: GemmaManager = createGemmaManager(),
    private val gemmaPreparation: GemmaPreparation = GemmaPreparation(
        gemmaManager = gemmaManager,
        modelDownloader = createGemmaModelDownloader(),
    ),
    private val geolocationRepository: GeolocationRepository? = null,
    private val weatherRepository: WeatherRepository? = null,
    private val soilRepository: SoilRepository? = null,
    private val speechRecognizer: SpeechRecognizer? = null,
    private val speechSynthesizer: SpeechSynthesizer? = null,
) : ViewModel() {

    private val toolBundle = createAgroGemToolBundle()
    private var chatSession: GemmaChatSession? = null

    private val _uiState = MutableStateFlow(ChatUiState(mode = ChatMode.Blank))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _preparationStatus = MutableStateFlow<GemmaPreparationStatus>(GemmaPreparationStatus.NotPrepared)
    val preparationStatus: StateFlow<GemmaPreparationStatus> = _preparationStatus.asStateFlow()

    init {
        println("[GemmaDemo] ViewModel inicializado. Verificando modelo...")
        prepareGemma()
    }

    private fun prepareGemma() {
        viewModelScope.launch {
            gemmaPreparation.status.collect { status ->
                _preparationStatus.value = status
            }
        }
        viewModelScope.launch {
            gemmaPreparation.ensureReady()
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InputChanged -> _uiState.value = _uiState.value.copy(inputText = event.text)
            is ChatEvent.SendMessage -> {
                println("[GemmaDemo] Evento: Enviar Mensaje")
                handleSendMessage()
            }
            is ChatEvent.ToggleThinking -> {
                println("[GemmaDemo] Evento: Toggle Thinking -> ${event.enabled}")
                _uiState.value = _uiState.value.copy(useThinking = event.enabled)
            }
            is ChatEvent.ToggleAttachmentMenu -> _uiState.value = _uiState.value.copy(showAttachmentMenu = event.show)
            is ChatEvent.ImageSelected -> {
                println("[GemmaDemo] Imagen seleccionada: ${event.result}")
                handleImageSelected(event.result)
            }
            is ChatEvent.RequestCamera, is ChatEvent.RequestGallery -> {
                _uiState.value = _uiState.value.copy(showAttachmentMenu = false)
            }
            is ChatEvent.NewSession -> {
                println("[GemmaDemo] Evento: Nueva sesión")
                speechSynthesizer?.stop()
                chatSession?.close()
                chatSession = null
                _uiState.value = ChatUiState(mode = ChatMode.Blank)
            }
            is ChatEvent.StartVoiceInput -> handleStartVoiceInput()
            is ChatEvent.StopVoiceInput -> handleStopVoiceInput()
            is ChatEvent.DismissVoice -> handleDismissVoice()
            is ChatEvent.VoicePermissionDenied -> handleVoicePermissionDenied()
            is ChatEvent.PlayAssistantMessage -> handlePlayAssistantMessage(event.messageId)
            is ChatEvent.RemoveAttachment -> handleRemoveAttachment(event.index)
            else -> {}
        }
    }

    private fun handleStartVoiceInput() {
        val recognizer = speechRecognizer ?: return
        recognizer.cancel()
        recognizer.start(
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
        )
    }

    private fun handleStopVoiceInput() {
        speechRecognizer?.stop()
        val captured = _uiState.value
        val text = captured.inputText.trim()
        if (text.isEmpty()) {
            _uiState.value = captured.copy(voiceState = VoiceState.Idle)
            return
        }
        _uiState.value = captured.copy(voiceState = VoiceState.Idle)
        handleSendMessage()
    }

    private fun handleDismissVoice() {
        speechRecognizer?.cancel()
        _uiState.value = _uiState.value.copy(voiceState = VoiceState.Idle, inputText = "")
    }

    private fun handleVoicePermissionDenied() {
        _uiState.value = _uiState.value.copy(
            voiceState = VoiceState.Idle,
        )
    }

    private fun handlePlayAssistantMessage(messageId: String) {
        val synth = speechSynthesizer ?: return
        val currentState = _uiState.value
        val message = currentState.messages.firstOrNull { it.id == messageId } ?: return
        if (message.sender != MessageSender.Assistant || message.isStreaming) return
        val text = message.text.trim()
        if (text.isEmpty()) return

        if (currentState.speakingMessageId == messageId) {
            synth.stop()
            _uiState.value = currentState.copy(speakingMessageId = null)
            return
        }

        val started = synth.speak(text)
        _uiState.value = currentState.copy(
            speakingMessageId = if (started) messageId else null,
        )
    }

    private fun handleRemoveAttachment(index: Int) {
        val current = _uiState.value
        if (index < 0 || index >= current.attachments.size) return
        _uiState.value = current.copy(
            attachments = current.attachments.filterIndexed { i, _ -> i != index },
        )
    }

    private fun handleImageSelected(uri: String?) {
        if (uri == null) return
        val currentAttachments = _uiState.value.attachments
        _uiState.value = _uiState.value.copy(
            attachments = currentAttachments + ChatAttachment.Image(uri)
        )
    }

    private fun handleSendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()
        val attachments = currentState.attachments

        if (text.isEmpty() && attachments.isEmpty()) {
            println("[GemmaDemo] Intento de envío vacío ignorado.")
            return
        }

        val userMessage = ChatMessage(
            id = "msg_${Random.nextLong()}",
            text = text,
            sender = MessageSender.User,
            attachments = attachments,
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )

        val assistantMessageId = "assistant_${Random.nextLong()}"
        val assistantPlaceholder = ChatMessage(
            id = assistantMessageId,
            text = "",
            sender = MessageSender.Assistant,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            isStreaming = true
        )

        _uiState.value = currentState.copy(
            messages = currentState.messages + userMessage + assistantPlaceholder,
            inputText = "",
            attachments = emptyList(),
            isLoading = true
        )

        viewModelScope.launch {
            try {
                if (!gemmaPreparation.ensureReady()) {
                    updateAssistantMessage(assistantMessageId, "Gemma no está disponible en este dispositivo.", null, true)
                    return@launch
                }

                val hasImage = userMessage.attachments.any { it is ChatAttachment.Image }
                val imageUris = userMessage.attachments
                    .filterIsInstance<ChatAttachment.Image>()
                    .map { it.uri }

                val session = chatSession ?: run {
                    val systemPrompt = buildSystemPrompt(currentState, hasImage)
                    println("[GemmaDemo] --- INICIANDO SESIÓN ---")
                    println("[GemmaDemo] SYSTEM PROMPT: $systemPrompt")
                    gemmaManager.startChatSession(
                        systemPrompt = systemPrompt,
                        temperature = 0.4f,
                        toolBundle = toolBundle,
                    ).also { chatSession = it }
                }

                toolCallTracker.reset()
                println("[GemmaDemo] USER PROMPT: $text")

                var accumulatedText = ""
                var latestThought: String? = null
                var lastEmitMs = 0L
                val throttleMs = 60L
                val toolObserverJob: Job = viewModelScope.launch {
                    toolCallTracker.calledTools.collect { tools ->
                        if (tools.isNotEmpty()) {
                            updateAssistantMessage(
                                id = assistantMessageId,
                                text = accumulatedText,
                                thought = latestThought,
                                isDone = false,
                                toolsUsed = tools.toList(),
                            )
                        }
                    }
                }

                try {
                    session.sendMessage(text = text, images = imageUris).collect { response ->
                        if (response.text.isNotEmpty()) {
                            accumulatedText += response.text
                        }
                        if (response.thought != null) {
                            latestThought = response.thought
                        }
                        val nowMs = Clock.System.now().toEpochMilliseconds()
                        val shouldEmit = response.isDone || (nowMs - lastEmitMs) >= throttleMs
                        if (shouldEmit) {
                            lastEmitMs = nowMs
                            updateAssistantMessage(
                                id = assistantMessageId,
                                text = accumulatedText,
                                thought = latestThought,
                                isDone = response.isDone,
                                toolsUsed = toolCallTracker.calledTools.value.toList(),
                            )
                        }
                        if (response.isDone) {
                            println("[GemmaDemo] --- INFERENCIA COMPLETADA ---")
                            println("[GemmaDemo] RESPUESTA FINAL: $accumulatedText")
                        }
                    }
                } finally {
                    toolObserverJob.cancel()
                }
                updateAssistantMessage(
                    id = assistantMessageId,
                    text = accumulatedText,
                    thought = latestThought,
                    isDone = true,
                    toolsUsed = toolCallTracker.calledTools.value.toList(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[GemmaDemo] ERROR DURANTE INFERENCIA: ${e.message}")
                chatSession?.close()
                chatSession = null
                updateAssistantMessage(assistantMessageId, "Error: ${e.message}", null, true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun buildSystemPrompt(state: ChatUiState, hasImage: Boolean): String {
        val envContext = fetchGeneralEnvironmentContext()
        return buildString {
            if (hasImage) {
                append("Eres AgroGem, un experto en diagnóstico visual de salud de cultivos. ")
                append("Analiza la imagen adjunta buscando enfermedades, hongos, bacterias, virus, insectos, ácaros, nematodos, plagas, deficiencias o estrés ambiental. ")
                append("Responde a la pregunta del usuario con el siguiente formato JSON: ")
                append("{nx: \"\", dx: \"\", est: \"\", trx: [\"\", \"\", \"\"]}")
            } else {
                append("Eres AgroGem, un asistente agronómico experto especializado en salud de cultivos. ")
                append("Toda respuesta debe orientarse al campo, el cultivo y la toma de decisiones agrícolas. ")
                append("Prioriza detección y explicación de enfermedades, hongos, bacterias, virus, insectos, ácaros, nematodos, plagas, deficiencias y estrés ambiental. ")
                append("Responde con diagnóstico diferencial cuando aplique: síntomas observables, causa probable, factores que lo favorecen y acciones prácticas de manejo. ")
                append("Si la pregunta no es agrícola, redirígela brevemente hacia un enfoque útil para agricultura o explica que está fuera del alcance del agente. ")
                append("Si el usuario adjunta una imagen de una planta, podrás analizar enfermedades y plagas en detalle.")
                append("\n\nTenés acceso a herramientas que consultan datos reales del campo del usuario: ")
                append("`get_current_weather` (clima actual), `get_soil_profile` (perfil de suelo), ")
                append("`get_pest_risks` (riesgos de plagas), `request_user_location` (pedirle GPS al usuario), ")
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
                append("NUNCA te rindas en silencio: si una herramienta falla, siempre respondé al usuario explicando qué pasó y, cuando aplique, pedile los datos que necesitás para reintentar.")
                if (envContext != null) {
                    append(envContext)
                }
            }

            if (!state.useThinking) {
                // Al añadir <|think|> al final, el modelo cree que ya terminó de pensar
                append("\n<|think|>")
            }
        }
    }

    private suspend fun fetchGeneralEnvironmentContext(): String? {
        val geo = geolocationRepository ?: return null
        val location = geo.observeResolvedLocation().first() ?: return null
        val weather = weatherRepository?.getCurrentWeather(location.coordinates)?.getOrNull()
        val soil = soilRepository?.getSoil(location.coordinates)?.getOrNull()
        return buildString {
            append("\n\nContexto del campo:\n")
            append("- Ubicación: ${location.display.primary}\n")
            weather?.let {
                append("- Clima: ${it.temperatureCelsius}, ${it.humidity} humedad, ${it.description}\n")
            }
            soil?.let {
                append("- Suelo: textura ${it.dominantTexture}")
                val ph = it.domainHorizons.firstOrNull()?.ph
                if (ph != null) append(", pH $ph")
                if (it.interpretation.isNotBlank()) append(" — ${it.interpretation}")
                append("\n")
            }
        }
    }

    private fun updateAssistantMessage(
        id: String,
        text: String,
        thought: String?,
        isDone: Boolean,
        toolsUsed: List<String>? = null,
    ) {
        val currentState = _uiState.value
        val currentMessages = currentState.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == id }

        if (index != -1) {
            val existingMessage = currentMessages[index]
            currentMessages[index] = existingMessage.copy(
                text = if (text.isNotEmpty()) text else existingMessage.text,
                thought = thought ?: existingMessage.thought,
                isStreaming = !isDone,
                toolsUsed = toolsUsed ?: existingMessage.toolsUsed,
            )
            _uiState.value = currentState.copy(messages = currentMessages)
        }
    }

    override fun onCleared() {
        speechSynthesizer?.stop()
        chatSession?.close()
        chatSession = null
        super.onCleared()
    }
}
