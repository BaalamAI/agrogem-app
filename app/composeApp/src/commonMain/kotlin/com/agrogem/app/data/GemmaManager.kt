package com.agrogem.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface GemmaManager {
    val isInitialized: Flow<Boolean>
    val supportsVision: Boolean

    /**
     * Initializes the engine with the model at the given path.
     *
     * Pass `preferVision = true` for vision-capable models so the engine boots
     * directly into vision mode. Booting text-only first and upgrading via
     * [enableVision] later requires holding two engine copies in memory at once,
     * which OOMs the process on most phones.
     *
     * @param modelPath Absolute path to the .litertlm file
     * @param preferVision If true, try vision-capable backends first.
     */
    suspend fun initialize(modelPath: String, preferVision: Boolean = false)

    /**
     * Re-initializes the engine with vision support enabled. Used when the user
     * attaches an image but the engine was started in text-only mode for fast
     * cold start. Closes the current engine and attempts vision-capable backends;
     * if none succeed, the previous engine is restored so chat keeps working.
     *
     * @return true if [supportsVision] is true after the call (either it already
     *   was, or the re-init succeeded). false if the loaded model can't do
     *   vision (e.g. Gemma 4 E2B) or no vision backend was available.
     */
    suspend fun enableVision(modelPath: String): Boolean

    /**
     * Sends a multimodal message to the model and returns the response.
     */
    suspend fun sendMessage(
        systemPrompt: String,
        userPrompt: String,
        images: List<String> = emptyList(),
        audioPath: String? = null,
        temperature: Float = 0.4f,
        toolBundle: GemmaToolBundle? = null,
    ): String

    /**
     * Starts a multi-turn chat session that preserves the model's KV-cache
     * across turns, so the model remembers previous messages without having
     * to resend the full history on every call.
     */
    fun startChatSession(
        systemPrompt: String,
        temperature: Float = 0.4f,
        toolBundle: GemmaToolBundle? = null,
    ): GemmaChatSession

    fun close()
}

interface GemmaChatSession {
    fun sendMessage(
        text: String,
        images: List<String> = emptyList(),
    ): Flow<GemmaResponse>

    fun close()
}

data class GemmaResponse(
    val text: String,
    val thought: String? = null,
    val isDone: Boolean = false,
    /**
     * Non-null when inference itself failed (e.g. native LiteRT-LM exception, OOM).
     * The flow will still emit one final response with isDone=true; cancellation does
     * NOT populate this field — it propagates as CancellationException as usual.
     */
    val error: String? = null,
)

expect interface GemmaToolSet

data class GemmaToolBundle(
    val tools: List<GemmaToolSet>,
    val automaticToolCalling: Boolean = false,
)

expect fun createGemmaManager(): GemmaManager

/**
 * Utility to manage model downloading and paths
 */
interface GemmaModelDownloader {
    val downloadProgress: Flow<Int?>
        get() = flowOf(null)

    suspend fun downloadModel(url: String): Result<String>
    fun isModelDownloaded(): Boolean
    fun getModelPath(): String

    /**
     * Deletes any file in the model directory that isn't part of the current
     * downloader's expected set. Older app versions used to save the model
     * under its real filename (e.g. `gemma-4-E2B-it.litertlm`) and a rename
     * left those files orphaned, eating multiple GB of disk forever.
     *
     * Default no-op for platforms that don't manage on-disk model storage.
     */
    suspend fun purgeOrphanFiles() {}
}

expect fun createGemmaModelDownloader(): GemmaModelDownloader
