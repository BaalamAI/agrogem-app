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
import com.google.ai.edge.litertlm.ExperimentalFlags
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

// Hard cap on total context length (input + output combined, NOT just generated tokens —
// the engine throws "Input token ids are too long" if input alone exceeds this).
// Gemma 4 E2B's native context is 32k; bounding it to 4096 shrinks the KV-cache ~8×,
// which is what keeps Android's Low-Memory Killer from SIGKILL'ing the process during
// long thinking phases on vision turns. 4096 leaves room for an image (~256 tokens) +
// system prompt (~500) + thinking (~2k) + final answer (~1k).
private const val MAX_CONTEXT_TOKENS = 4096
private const val MAX_IMAGES_PER_TURN = 1

/**
 * Pulls visible content and channeled thinking from a LiteRT-LM Message.
 *
 * Gemma 4 thinking models route their reasoning into the `channels` map
 * (e.g. `{thought=...}`) while leaving `contents`/`toString()` empty until
 * the response phase begins. Reading `toString()` alone — which earlier
 * code did — produced ~50s of empty chunks and a blank chat bubble, and
 * for the non-streaming path it leaked the full `Message(...)` wrapper
 * (including `channels={thought=...}`) into downstream parsers and the UI.
 *
 * Reflection is used so this keeps compiling if LiteRT-LM renames a field
 * across versions; fallback is the previous `toString()` behavior.
 */
private data class ParsedMessage(val text: String, val thought: String?)

private fun extractMessage(msg: Any): ParsedMessage {
    val rawString = runCatching { msg.toString() }.getOrDefault("")
    val contents = runCatching {
        msg.javaClass.getMethod("getContents").invoke(msg)?.toString().orEmpty()
    }.getOrDefault("")
    val channels = runCatching {
        @Suppress("UNCHECKED_CAST")
        msg.javaClass.getMethod("getChannels").invoke(msg) as? Map<String, Any?>
    }.getOrNull()
    val thought = channels?.entries
        ?.firstOrNull { (key, _) -> key.equals("thought", ignoreCase = true) }
        ?.value
        ?.toString()
        ?.takeIf { it.isNotBlank() }
    val text = when {
        contents.isNotEmpty() -> contents
        rawString.isNotEmpty() -> rawString
        else -> ""
    }
    return ParsedMessage(text = text, thought = thought)
}

class AndroidGemmaManager(private val context: Context) : GemmaManager {
    private var engine: Engine? = null
    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized = _isInitialized.asStateFlow()
    override var supportsVision: Boolean = false
        private set

    private val initializationMutex = kotlinx.coroutines.sync.Mutex()

    private data class BackendVariant(val name: String, val backend: Backend, val withVision: Boolean)

    private val textOnlyVariants = listOf(
        BackendVariant("GPU", Backend.GPU(), false),
        BackendVariant("CPU", Backend.CPU(), false),
    )
    private val visionVariants = listOf(
        BackendVariant("GPU+Vision", Backend.GPU(), true),
        BackendVariant("CPU+Vision", Backend.CPU(), true),
    )

    @OptIn(ExperimentalApi::class)
    private fun buildEngineConfig(modelPath: String, variant: BackendVariant): EngineConfig =
        if (variant.withVision) {
            EngineConfig(
                modelPath = modelPath,
                backend = variant.backend,
                visionBackend = Backend.GPU(),
                audioBackend = Backend.CPU(),
                maxNumTokens = MAX_CONTEXT_TOKENS,
                maxNumImages = MAX_IMAGES_PER_TURN,
                cacheDir = context.cacheDir.path,
            )
        } else {
            EngineConfig(
                modelPath = modelPath,
                backend = variant.backend,
                maxNumTokens = MAX_CONTEXT_TOKENS,
                cacheDir = context.cacheDir.path,
            )
        }

    /** Attempts the given variants in order, returning the first that succeeds. */
    private fun tryInitVariants(modelPath: String, variants: List<BackendVariant>): Pair<Engine, BackendVariant>? {
        for (variant in variants) {
            try {
                Log.d("GemmaManager", "Intentando inicializar con Backend: ${variant.name}...")
                val newEngine = Engine(buildEngineConfig(modelPath, variant))
                Log.d("GemmaManager", "Llamando a engine.initialize() para ${variant.name}...")
                newEngine.initialize()
                Log.i("GemmaManager", "¡ÉXITO! Gemma 4 inicializado con ${variant.name} (vision=${variant.withVision})")
                return newEngine to variant
            } catch (e: Throwable) {
                Log.w("GemmaManager", "Fallo al inicializar con ${variant.name}: ${e.message}")
            }
        }
        return null
    }

    @OptIn(ExperimentalApi::class)
    override suspend fun initialize(modelPath: String, preferVision: Boolean) {
        // Double-check without lock for performance
        if (_isInitialized.value) return

        initializationMutex.withLock {
            // Check again inside lock
            if (_isInitialized.value) return@withLock

            withContext(Dispatchers.IO) {
                Log.i("GemmaManager", "Iniciando proceso de inicialización única en: $modelPath (preferVision=$preferVision)")
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

                    // Speculative decoding (MTP) was previously enabled per the docs' "universal"
                    // recommendation, but on Gemma 4 E2B + vision it appears to push memory over
                    // the edge — the LMK kills the process mid-thinking. Disabled to free memory;
                    // costs ~10-20% inference speed but the alternative is a SIGKILL'd process.
                    runCatching { ExperimentalFlags.enableSpeculativeDecoding = false }
                        .onFailure { Log.w("GemmaManager", "No se pudo configurar speculative decoding: ${it.message}") }

                    // For vision-capable models, boot directly into vision mode. Trying to
                    // upgrade later via enableVision() loads a second engine copy in memory
                    // before closing the first one — Low-Memory Killer SIGKILLs the process.
                    // For text-only models, start text-only (saves ~10-20s on cold start).
                    val (firstChoice, secondChoice) = if (preferVision) {
                        visionVariants to textOnlyVariants
                    } else {
                        textOnlyVariants to visionVariants
                    }
                    val result = tryInitVariants(modelPath, firstChoice)
                        ?: tryInitVariants(modelPath, secondChoice)

                    if (result == null) {
                        Log.e("GemmaManager", "Ningún backend pudo inicializar el modelo.")
                        _isInitialized.value = false
                        return@withContext
                    }

                    val (newEngine, variant) = result
                    engine = newEngine
                    supportsVision = variant.withVision
                    _isInitialized.value = true
                } catch (e: Throwable) {
                    Log.e("GemmaManager", "ERROR CRITICO inesperado", e)
                    _isInitialized.value = false
                }
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    override suspend fun enableVision(modelPath: String): Boolean {
        if (supportsVision) return true

        return initializationMutex.withLock {
            if (supportsVision) return@withLock true

            withContext(Dispatchers.IO) {
                Log.i("GemmaManager", "Activando modo visión para modelo en: $modelPath")
                // Close the text-only engine BEFORE creating the vision one. Holding two
                // engine copies (~1 GB each) simultaneously OOM-kills the process on most
                // phones. If vision init fails afterwards, the caller falls back to backend
                // — acceptable, since the user attached an image expecting vision support.
                runCatching { engine?.close() }
                    .onFailure { Log.w("GemmaManager", "No se pudo cerrar engine previo: ${it.message}") }
                engine = null
                supportsVision = false
                _isInitialized.value = false

                val result = tryInitVariants(modelPath, visionVariants)
                if (result == null) {
                    Log.w("GemmaManager", "El modelo cargado no soporta visión; engine text-only fue cerrado.")
                    return@withContext false
                }
                val (visionEngine, variant) = result
                engine = visionEngine
                supportsVision = variant.withVision
                _isInitialized.value = true
                true
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

            val tempFiles = mutableListOf<File>()
            try {
                currentEngine.createConversation(conversationConfig).use { conversation ->
                    val contentList = mutableListOf<Content>()

                    images.forEach { uri ->
                        val localPath = materializeAttachment(uri, tempFiles)
                        contentList.add(Content.ImageFile(localPath))
                    }
                    contentList.add(Content.Text(userPrompt))

                    val response = conversation.sendMessage(Contents.of(*contentList.toTypedArray()))
                    // Same parser the streaming path uses: strips the Message(...) wrapper
                    // and the `channels={thought=...}` reasoning, returning only the visible
                    // contents. Without this, plant-analysis parsing breaks and onboarding
                    // chat shows the model's thought process to the user.
                    extractMessage(response).text
                }
            } finally {
                tempFiles.forEach { file ->
                    runCatching { if (file.exists()) file.delete() }
                        .onFailure { Log.w("GemmaManager", "No se pudo borrar temp ${file.name}: ${it.message}") }
                }
            }
        }
    }

    override fun startChatSession(
        systemPrompt: String,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): GemmaChatSession {
        val currentEngine = engine ?: throw IllegalStateException("Gemma not initialized")
        val config = createConversationConfig(systemPrompt, temperature, toolBundle)
        return AndroidGemmaChatSession(currentEngine, config, ::materializeAttachment)
    }

    private class AndroidGemmaChatSession(
        private val engine: Engine,
        private val baseConfig: ConversationConfig,
        private val materializeAttachment: (uri: String, tempFiles: MutableList<File>) -> String,
    ) : GemmaChatSession {
        // Serializes sendMessage() calls. LiteRT-LM's native Conversation cannot be
        // re-entered concurrently — overlapping calls null-deref in liblitertlm_jni.
        private val mutex = kotlinx.coroutines.sync.Mutex()
        private var conversation: Conversation? = engine.createConversation(baseConfig)

        override fun sendMessage(text: String, images: List<String>): Flow<GemmaResponse> {
            val tempFiles = mutableListOf<File>()
            return channelFlow {
                mutex.withLock {
                    val conv = ensureConversation()
                    val contentList = mutableListOf<Content>()
                    images.forEach { uri -> contentList.add(Content.ImageFile(materializeAttachment(uri, tempFiles))) }
                    contentList.add(Content.Text(text))

                    var chunkIndex = 0
                    try {
                        conv.sendMessageAsync(Contents.of(*contentList.toTypedArray()))
                            .collect { msg ->
                                val parsed = extractMessage(msg)
                                if (chunkIndex < 3 || (chunkIndex % 20 == 0)) {
                                    Log.d(
                                        "GemmaChunk",
                                        "i=$chunkIndex text=${parsed.text.length} thought=${parsed.thought?.length ?: 0}",
                                    )
                                }
                                chunkIndex++
                                send(GemmaResponse(text = parsed.text, thought = parsed.thought, isDone = false))
                            }
                        send(GemmaResponse(text = "", thought = null, isDone = true))
                    } catch (ce: kotlinx.coroutines.CancellationException) {
                        // The native engine/ thread keeps writing into Conversation after
                        // Kotlin cancellation. Closing it here forces the native side to
                        // wind down and prevents the next sendMessage from null-derefing.
                        discardConversation()
                        throw ce
                    } catch (t: Throwable) {
                        Log.e("GemmaManager", "Inference stream failed", t)
                        // Engine state is suspect after a native throw; recreate next turn.
                        discardConversation()
                        val msg = t.message ?: t::class.simpleName ?: "unknown"
                        send(GemmaResponse(text = "", thought = null, isDone = true, error = msg))
                    }
                }
            }.onCompletion {
                tempFiles.forEach { file ->
                    runCatching { if (file.exists()) file.delete() }
                        .onFailure { Log.w("GemmaManager", "No se pudo borrar temp ${file.name}: ${it.message}") }
                }
            }
        }

        private fun ensureConversation(): Conversation =
            conversation ?: engine.createConversation(baseConfig).also { conversation = it }

        private fun discardConversation() {
            runCatching { conversation?.close() }
                .onFailure { Log.w("GemmaManager", "No se pudo cerrar Conversation: ${it.message}") }
            conversation = null
        }

        override fun close() {
            discardConversation()
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

    private fun materializeAttachment(uriString: String, tempFiles: MutableList<File>): String {
        // Resolve to a content:// or file:// Uri the downscaler can read.
        val sourceUri: android.net.Uri = when {
            uriString.startsWith("content://") -> android.net.Uri.parse(uriString)
            uriString.startsWith("file://") -> {
                val candidate = File(android.net.Uri.parse(uriString).path ?: uriString.removePrefix("file://"))
                if (!candidate.exists() || candidate.length() == 0L) {
                    throw java.io.IOException("La imagen no existe o está vacía: $uriString")
                }
                android.net.Uri.fromFile(candidate)
            }
            else -> {
                val candidate = File(uriString)
                if (!candidate.exists() || candidate.length() == 0L) {
                    throw java.io.IOException("La imagen no existe o está vacía: $uriString")
                }
                android.net.Uri.fromFile(candidate)
            }
        }

        // Always downscale: 12 MP phone photos waste tokens and bloat vision-prefill memory.
        // Helper falls back to a raw copy on any decode failure, so we never leave the
        // user without an image to send.
        val downscaledFile = ImageDownscaler.downscaleToCache(context, sourceUri, context.cacheDir)
        if (downscaledFile.length() == 0L) {
            runCatching { downscaledFile.delete() }
            throw java.io.IOException("La imagen procesada quedó vacía: $uriString")
        }
        tempFiles.add(downscaledFile)
        return downscaledFile.absolutePath
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
