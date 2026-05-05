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
        temperature: Float = 0.4f,
        toolBundle: GemmaToolBundle? = null,
    ): String

    /**
     * Streams the model's response.
     */
    fun sendMessageStream(
        systemPrompt: String,
        userPrompt: String,
        images: List<String> = emptyList(),
        audioPath: String? = null,
        temperature: Float = 0.4f,
        toolBundle: GemmaToolBundle? = null,
    ): Flow<GemmaResponse>

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
    val isDone: Boolean = false
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
    suspend fun downloadModel(url: String): Result<String>
    fun isModelDownloaded(): Boolean
    fun getModelPath(): String
}

expect fun createGemmaModelDownloader(): GemmaModelDownloader
