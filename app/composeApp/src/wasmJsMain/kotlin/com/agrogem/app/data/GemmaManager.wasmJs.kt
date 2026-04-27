package com.agrogem.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

actual fun getGemmaManager(): GemmaManager = object : GemmaManager {
    override val isInitialized: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun initialize(modelPath: String) {}
    override suspend fun sendMessage(
        systemPrompt: String,
        userPrompt: String,
        images: List<String>,
        audioPath: String?
    ): String = "Not implemented on Wasm"
    
    override fun sendMessageStream(
        systemPrompt: String,
        userPrompt: String,
        images: List<String>,
        audioPath: String?
    ): Flow<String> = MutableStateFlow("Not implemented on Wasm")
    
    override fun close() {}
}

actual fun getGemmaModelDownloader(): GemmaModelDownloader = object : GemmaModelDownloader {
    override suspend fun downloadModel(url: String): Result<String> = Result.failure(Exception("Not implemented"))
    override fun isModelDownloaded(): Boolean = false
    override fun getModelPath(): String = ""
}
