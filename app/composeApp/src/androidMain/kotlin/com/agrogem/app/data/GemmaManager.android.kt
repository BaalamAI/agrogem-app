package com.agrogem.app.data

import android.content.Context
import android.util.Log
import com.agrogem.app.AndroidAppContext
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

actual typealias GemmaToolSet = ToolSet

class AndroidGemmaManager(private val context: Context) : GemmaManager {
    private var engine: Engine? = null
    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized = _isInitialized.asStateFlow()

    private val initializationMutex = kotlinx.coroutines.sync.Mutex()

    @OptIn(ExperimentalApi::class)
    override suspend fun initialize(modelPath: String) {
        // Double-check without lock for performance
        if (_isInitialized.value) return

        initializationMutex.withLock {
            // Check again inside lock
            if (_isInitialized.value) return@withLock

            withContext(Dispatchers.IO) {
                Log.i("GemmaManager", "Iniciando proceso de inicialización única en: $modelPath")
                try {
                    val modelFile = File(modelPath)
                    if (!modelFile.exists()) {
                        Log.e("GemmaManager", "ERROR: El archivo del modelo no existe en la ruta: $modelPath")
                        return@withContext
                    }

                    // Log available memory
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    val memInfo = android.app.ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memInfo)
                    Log.i("GemmaManager", "Memoria disponible: ${memInfo.availMem / (1024 * 1024)} MB de ${memInfo.totalMem / (1024 * 1024)} MB")

                    // Define initialization steps for each backend.
                    // Priorizamos GPU para rendimiento, CPU como fallback seguro.
                    val backendsToTry = listOf(
                        "GPU" to { Backend.GPU() },
                        "CPU" to { Backend.CPU() }
                    )

                    var initializationSuccessful = false
                    
                    for ((name, backendProvider) in backendsToTry) {
                        try {
                            Log.d("GemmaManager", "Intentando inicializar con Backend: $name...")
                            val currentBackend = backendProvider()
                            
                            // Configuración completa incluyendo Visión y Audio para Gemma 4
                            val config = EngineConfig(
                                modelPath = modelPath,
                                backend = currentBackend,
                                visionBackend = Backend.GPU(), // Forzamos GPU para visión como recomienda Gallery
                                audioBackend = Backend.CPU(),  // Audio en CPU para estabilidad
                                cacheDir = context.cacheDir.path
                            )
                            
                            val newEngine = Engine(config)
                            Log.d("GemmaManager", "Llamando a engine.initialize() para $name...")
                            newEngine.initialize()
                            
                            engine = newEngine
                            initializationSuccessful = true
                            Log.i("GemmaManager", "¡ÉXITO! Gemma 4 inicializado correctamente con $name")
                            break 
                        } catch (e: Throwable) {
                            Log.w("GemmaManager", "Fallo al inicializar con $name: ${e.message}")
                        }
                    }

                    if (!initializationSuccessful) {
                        Log.e("GemmaManager", "Ningún backend pudo inicializar el modelo.")
                        _isInitialized.value = false
                        return@withContext
                    }

                    _isInitialized.value = true
                } catch (e: Throwable) {
                    Log.e("GemmaManager", "ERROR CRITICO inesperado", e)
                    _isInitialized.value = false
                }
            }
        }
    }

    override suspend fun sendMessage(
        systemPrompt: String,
        userPrompt: String,
        images: List<String>,
        audioPath: String?,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): String {
        val currentEngine = engine ?: throw IllegalStateException("Gemma not initialized")
        
        return withContext(Dispatchers.IO) {
            val conversationConfig = createConversationConfig(
                systemPrompt = systemPrompt,
                temperature = temperature,
                toolBundle = toolBundle,
            )
            
            currentEngine.createConversation(conversationConfig).use { conversation ->
                val contentList = mutableListOf<Content>()
                
                images.forEach { uri ->
                    val localPath = getLocalPath(uri)
                    contentList.add(Content.ImageFile(localPath))
                }
                contentList.add(Content.Text(userPrompt))
                
                val response = conversation.sendMessage(Contents.of(*contentList.toTypedArray()))
                response.toString()
            }
        }
    }

    override fun sendMessageStream(
        systemPrompt: String,
        userPrompt: String,
        images: List<String>,
        audioPath: String?,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): Flow<GemmaResponse> {
        val currentEngine = engine ?: throw IllegalStateException("Gemma not initialized")
        
        val conversationConfig = createConversationConfig(
            systemPrompt = systemPrompt,
            temperature = temperature,
            toolBundle = toolBundle,
        )

        val conversation = currentEngine.createConversation(conversationConfig)

        return channelFlow {
            val contentList = mutableListOf<Content>()
            images.forEach { uri ->
                val localPath = getLocalPath(uri)
                contentList.add(Content.ImageFile(localPath))
            }
            contentList.add(Content.Text(userPrompt))

            conversation.sendMessageAsync(Contents.of(*contentList.toTypedArray()))
                .collect { message ->
                    send(
                        GemmaResponse(
                            text = message.toString(),
                            thought = null,
                            isDone = false
                        )
                    )
                }
            send(GemmaResponse(text = "", thought = null, isDone = true))
        }.onCompletion {
            conversation.close()
        }
    }

    override fun startChatSession(
        systemPrompt: String,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): GemmaChatSession {
        val currentEngine = engine ?: throw IllegalStateException("Gemma not initialized")
        val config = createConversationConfig(systemPrompt, temperature, toolBundle)
        val conversation = currentEngine.createConversation(config)
        return AndroidGemmaChatSession(conversation, ::getLocalPath)
    }

    private class AndroidGemmaChatSession(
        private val conversation: Conversation,
        private val resolveLocalPath: (String) -> String,
    ) : GemmaChatSession {
        override fun sendMessage(text: String, images: List<String>): Flow<GemmaResponse> = channelFlow {
            val contentList = mutableListOf<Content>()
            images.forEach { uri -> contentList.add(Content.ImageFile(resolveLocalPath(uri))) }
            contentList.add(Content.Text(text))

            conversation.sendMessageAsync(Contents.of(*contentList.toTypedArray()))
                .collect { msg ->
                    send(GemmaResponse(text = msg.toString(), thought = null, isDone = false))
                }
            send(GemmaResponse(text = "", thought = null, isDone = true))
        }

        override fun close() {
            conversation.close()
        }
    }

    private fun createConversationConfig(
        systemPrompt: String,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): ConversationConfig {
        val samplerConfig = SamplerConfig(temperature = temperature.toDouble(), topK = 64, topP = 0.95)
        return if (toolBundle == null) {
            ConversationConfig(
                systemInstruction = Contents.of(Content.Text(systemPrompt)),
                samplerConfig = samplerConfig,
            )
        } else {
            ConversationConfig(
                systemInstruction = Contents.of(Content.Text(systemPrompt)),
                samplerConfig = samplerConfig,
                tools = toolBundle.tools.map { tool(it) },
                automaticToolCalling = toolBundle.automaticToolCalling,
            )
        }
    }

    private fun getLocalPath(uriString: String): String {
        if (!uriString.startsWith("content://")) return uriString

        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "gemma_input_${System.currentTimeMillis()}.jpg")
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e("GemmaManager", "Error al convertir URI a path local: ${e.message}")
            uriString
        }
    }

    override fun close() {
        engine?.close()
        engine = null
        _isInitialized.value = false
    }
}

// Singleton instance management
private var instance: GemmaManager? = null

actual fun createGemmaManager(): GemmaManager {
    if (instance == null) {
        if (!AndroidAppContext.isInitialized) {
             throw IllegalStateException("AndroidAppContext not initialized")
        }
        initializeGemmaManager(AndroidAppContext.context)
    }
    return instance!!
}

fun initializeGemmaManager(context: Context) {
    if (instance == null) {
        instance = AndroidGemmaManager(context.applicationContext)
    }
}
