package com.agrogem.app.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val DEFAULT_GEMMA_MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/7fa1d78473894f7e736a21d920c3aa80f950c0db/gemma-4-E2B-it.litertlm?download=true"

sealed interface GemmaPreparationStatus {
    data object NotPrepared : GemmaPreparationStatus
    data object Downloading : GemmaPreparationStatus
    data object Preparing : GemmaPreparationStatus
    data object Ready : GemmaPreparationStatus
    data class Unavailable(val reason: String? = null) : GemmaPreparationStatus
}

class GemmaPreparation(
    private val gemmaManager: GemmaManager,
    private val modelDownloader: GemmaModelDownloader,
    private val defaultModelUrl: String = DEFAULT_GEMMA_MODEL_URL,
    private val downloadPollIntervalMs: Long = 1_000,
    private val maxDownloadWaitMs: Long = 30 * 60 * 1_000,
) {
    private val mutex = Mutex()

    private val _status = MutableStateFlow<GemmaPreparationStatus>(GemmaPreparationStatus.NotPrepared)
    val status: StateFlow<GemmaPreparationStatus> = _status.asStateFlow()

    fun hasLocalModel(): Boolean = modelDownloader.isModelDownloaded()

    suspend fun ensureReady(): Boolean {
        if (_status.value is GemmaPreparationStatus.Ready) return true

        return mutex.withLock {
            if (_status.value is GemmaPreparationStatus.Ready) return@withLock true

            val modelReady = runCatching {
                if (!modelDownloader.isModelDownloaded()) {
                    _status.value = GemmaPreparationStatus.Downloading
                    if (modelDownloader.downloadModel(defaultModelUrl).isSuccess) {
                        waitUntilModelDownloaded()
                    } else {
                        false
                    }
                } else {
                    true
                }
            }.getOrDefault(false)

            if (!modelReady || !modelDownloader.isModelDownloaded()) {
                _status.value = if (!modelReady) {
                    GemmaPreparationStatus.Unavailable("Model download failed")
                } else {
                    GemmaPreparationStatus.Downloading
                }
                return@withLock false
            }

            _status.value = GemmaPreparationStatus.Preparing

            val initialized = runCatching {
                gemmaManager.initialize(modelDownloader.getModelPath())
                true
            }.getOrDefault(false)

            val ready = initialized && runCatching { gemmaManager.isInitialized.firstOrFalse() }.getOrDefault(false)
            _status.value = if (ready) GemmaPreparationStatus.Ready else GemmaPreparationStatus.Unavailable("Gemma init failed")
            ready
        }
    }

    private suspend fun waitUntilModelDownloaded(): Boolean {
        val attempts = (maxDownloadWaitMs / downloadPollIntervalMs).toInt().coerceAtLeast(1)
        repeat(attempts) {
            if (modelDownloader.isModelDownloaded()) return true
            delay(downloadPollIntervalMs)
        }
        return modelDownloader.isModelDownloaded()
    }
}

private suspend fun kotlinx.coroutines.flow.Flow<Boolean>.firstOrFalse(): Boolean =
    runCatching { first() }.getOrDefault(false)
