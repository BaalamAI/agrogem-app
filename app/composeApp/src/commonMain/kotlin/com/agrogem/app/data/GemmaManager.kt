package com.agrogem.app.data

import kotlinx.coroutines.flow.Flow

interface GemmaManager {
    val isInitialized: Flow<Boolean>

    /**
     * Initializes the engine with the model at the given path.
     * @param modelPath Absolute path to the .litertlm file
     */
    suspend fun initialize(modelPath: String)

    /**
     * Sends a multimodal message to the model and returns the response.
     */
    suspend fun sendMessage(
        systemPrompt: String,
        userPrompt: String,
        images: List<String> = emptyList(),
        audioPath: String? = null,
        temperature: Float = 0.4f
    ): String

    /**
     * Streams the model's response.
     */
    fun sendMessageStream(
        systemPrompt: String,
        userPrompt: String,
        images: List<String> = emptyList(),
        audioPath: String? = null,
        temperature: Float = 0.4f
    ): Flow<GemmaResponse>

    fun close()
}

data class GemmaResponse(
    val text: String,
    val thought: String? = null,
    val isDone: Boolean = false
)

expect fun getGemmaManager(): GemmaManager

/**
 * Utility to manage model downloading and paths
 */
interface GemmaModelDownloader {
    suspend fun downloadModel(url: String): Result<String>
    fun isModelDownloaded(): Boolean
    fun getModelPath(): String
}

expect fun getGemmaModelDownloader(): GemmaModelDownloader
