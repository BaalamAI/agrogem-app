package com.agrogem.app.ui.screens.gemma_demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.getGemmaManager
import com.agrogem.app.data.getGemmaModelDownloader
import com.agrogem.app.ui.screens.chat.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

class GemmaDemoViewModel : ViewModel() {

    private val gemmaManager = getGemmaManager()
    private val modelDownloader = getGemmaModelDownloader()

    private val _uiState = MutableStateFlow(ChatUiState(mode = ChatMode.Blank))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    init {
        println("[GemmaDemo] ViewModel inicializado. Verificando modelo...")
        checkModelAndInit()
    }

    private fun checkModelAndInit() {
        viewModelScope.launch {
            if (!modelDownloader.isModelDownloaded()) {
                println("[GemmaDemo] Modelo no encontrado. Iniciando descarga...")
                _isDownloading.value = true
                val url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/7fa1d78473894f7e736a21d920c3aa80f950c0db/gemma-4-E2B-it.litertlm?download=true"
                modelDownloader.downloadModel(url)
            } else {
                println("[GemmaDemo] Modelo encontrado en local. Inicializando motor...")
                gemmaManager.initialize(modelDownloader.getModelPath())
                println("[GemmaDemo] Motor inicializado correctamente.")
            }
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
            else -> {}
        }
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
            text = "...",
            sender = MessageSender.Assistant,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            isStreaming = true
        )

        _uiState.value = currentState.copy(
            messages = currentState.messages + userMessage + assistantPlaceholder,
            inputText = "",
            attachments = emptyList()
        )

        viewModelScope.launch {
            // Lógica de Thinking: si NO está habilitado, cerramos el tag de pensamiento en el system prompt
            val systemPrompt = buildString {
                append("Eres un experto Fitopatólogo Especialista en enfermedades de cultivos. ")
                append("Tu funciones es analizar la imagen y responder a la pregunta del usuario con el siguiente formato JSON: ")
                append("{nx: \"\", dx: \"\", est: \"\", trx: [\"\", \"\", \"\"]}")
                
                if (!currentState.useThinking) {
                    // Al añadir <|think|> al final, el modelo cree que ya terminó de pensar
                    append("\n<|think|>") 
                }
            }

            println("[GemmaDemo] --- INICIANDO INFERENCIA ---")
            println("[GemmaDemo] SYSTEM PROMPT: $systemPrompt")
            println("[GemmaDemo] USER PROMPT: $text")
            println("[GemmaDemo] TEMPERATURA: 0.4")

            try {
                val imageUris = userMessage.attachments
                    .filterIsInstance<ChatAttachment.Image>()
                    .map { it.uri }

                var accumulatedText = ""
                
                gemmaManager.sendMessageStream(
                    systemPrompt = systemPrompt,
                    userPrompt = text,
                    images = imageUris,
                    temperature = 0.4f
                ).collect { response ->
                    if (response.text.isNotEmpty()) {
                        accumulatedText += response.text
                        updateAssistantMessage(assistantMessageId, accumulatedText, response.thought, response.isDone)
                    } else if (response.isDone) {
                        // Si recibimos señal de fin sin texto nuevo, aseguramos el estado final
                        updateAssistantMessage(assistantMessageId, accumulatedText, null, true)
                    }
                    
                    if (response.isDone) {
                        println("[GemmaDemo] --- INFERENCIA COMPLETADA ---")
                        println("[GemmaDemo] RESPUESTA FINAL: $accumulatedText")
                    }
                }
            } catch (e: Exception) {
                println("[GemmaDemo] ERROR DURANTE INFERENCIA: ${e.message}")
                updateAssistantMessage(assistantMessageId, "Error: ${e.message}", null, true)
            }
        }
    }

    private fun updateAssistantMessage(id: String, text: String, thought: String?, isDone: Boolean) {
        val currentState = _uiState.value
        val currentMessages = currentState.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == id }
        
        if (index != -1) {
            val existingMessage = currentMessages[index]
            // Solo actualizamos si hay texto nuevo o si estamos marcando el fin
            currentMessages[index] = existingMessage.copy(
                text = if (text.isNotEmpty()) text else existingMessage.text,
                thought = thought ?: existingMessage.thought,
                isStreaming = !isDone
            )
            _uiState.value = currentState.copy(messages = currentMessages)
        }
    }
}
