package com.agrogem.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

// =============================================================================
// STUB iOS — el motor real de Gemma todavía NO está conectado en iOS.
// Esta implementación existe SOLO para que el proyecto compile en iOS y
// puedas ver la UI. Cualquier llamada al chat devolverá un mensaje fijo
// indicando que iOS aún no tiene Gemma.
//
// Las firmas DEBEN coincidir con la interfaz definida en commonMain/GemmaManager.kt
// =============================================================================

private const val NOT_IMPLEMENTED = "Gemma aún no está implementado en iOS. " +
    "Para activarlo hay que integrar MediaPipe LLM Inference o LiteRT-LM Swift."

actual interface GemmaToolSet

actual fun createGemmaManager(): GemmaManager = object : GemmaManager {
    override val isInitialized: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun initialize(modelPath: String) {
        // No-op: no hay engine que inicializar todavía.
    }

    override suspend fun sendMessage(
        systemPrompt: String,
        userPrompt: String,
        images: List<String>,
        audioPath: String?,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): String = NOT_IMPLEMENTED

    override fun sendMessageStream(
        systemPrompt: String,
        userPrompt: String,
        images: List<String>,
        audioPath: String?,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): Flow<GemmaResponse> = flowOf(
        GemmaResponse(text = NOT_IMPLEMENTED, thought = null, isDone = true),
    )

    override fun startChatSession(
        systemPrompt: String,
        temperature: Float,
        toolBundle: GemmaToolBundle?,
    ): GemmaChatSession = object : GemmaChatSession {
        override fun sendMessage(text: String, images: List<String>): Flow<GemmaResponse> =
            flowOf(GemmaResponse(text = NOT_IMPLEMENTED, thought = null, isDone = true))

        override fun close() {}
    }

    override fun close() {}
}

actual fun createGemmaModelDownloader(): GemmaModelDownloader = object : GemmaModelDownloader {
    override suspend fun downloadModel(url: String): Result<String> =
        Result.failure(Exception("Gemma downloader no implementado en iOS"))
    override fun isModelDownloaded(): Boolean = false
    override fun getModelPath(): String = ""
}
