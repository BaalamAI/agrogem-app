package com.agrogem.app.data

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val DEFAULT_GEMMA_MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/7fa1d78473894f7e736a21d920c3aa80f950c0db/gemma-4-E2B-it.litertlm?download=true"
const val FINETUNED_GEMMA_MODEL_URL = "https://huggingface.co/alvarog1318/gemma4-vision-crop-litertlm/resolve/main/model.litertlm?download=true"

/**
 * Available Gemma model variants the user can pick from in the model selector.
 *
 * `Default` is the stock Gemma 4 E2B IT released by litert-community — multimodal,
 * supports vision out of the box.
 * `Finetuned` is a custom text-only finetune; it is opt-in so it only downloads
 * after the user explicitly confirms it.
 */
sealed class GemmaModelOption(
    val id: String,
    val displayName: String,
    val shortName: String,
    val url: String,
    /**
     * Whether this model file is known to support vision inference. Triggering a vision-mode
     * engine init against a text-only model can SIGABRT inside LiteRT-LM (uncatchable from
     * Kotlin), so callers should gate vision attempts behind this flag.
     */
    val visionCapable: Boolean,
    /**
     * Marks the option as experimental in the UI. Used for variants whose bundle still has
     * known on-device issues (slow CPU-only inference, broken stop tokens) so the user
     * understands what they are picking. The option remains selectable.
     */
    val isExperimental: Boolean = false,
) {
    data object Default : GemmaModelOption(
        id = "default-it",
        displayName = "Gemma 4 (default)",
        shortName = "Gemma 4",
        url = DEFAULT_GEMMA_MODEL_URL,
        visionCapable = true,
    )

    data object Finetuned : GemmaModelOption(
        id = "finetuned-vision-crop",
        displayName = "Gemma 4 finetuneado",
        shortName = "Finetuneado",
        url = FINETUNED_GEMMA_MODEL_URL,
        visionCapable = false,
        isExperimental = true,
    )

    companion object {
        val all: List<GemmaModelOption> = listOf(Default, Finetuned)

        fun fromId(id: String?): GemmaModelOption =
            all.firstOrNull { it.id == id } ?: Default
    }
}

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
    private val modelPreference: GemmaModelPreference? = null,
) {
    private val mutex = Mutex()

    private val _status = MutableStateFlow<GemmaPreparationStatus>(GemmaPreparationStatus.NotPrepared)
    val status: StateFlow<GemmaPreparationStatus> = _status.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        modelPreference?.read() ?: GemmaModelOption.Default
    )
    val selectedModel: StateFlow<GemmaModelOption> = _selectedModel.asStateFlow()

    fun hasLocalModel(): Boolean = modelDownloader.isModelDownloaded()

    suspend fun ensureReady(): Boolean {
        if (_status.value is GemmaPreparationStatus.Ready) return true
        return prepareInternal(force = false)
    }

    /**
     * Asks the engine to enable vision support (re-initializing if needed).
     * Returns true if vision is available after the call.
     *
     * Caller (e.g. ChatViewModel) is responsible for invalidating any open
     * chat session — the underlying engine may have been swapped, so existing
     * Conversation handles are no longer valid.
     */
    suspend fun ensureVisionReady(): Boolean = mutex.withLock {
        if (gemmaManager.supportsVision) return@withLock true
        // Hard gate: only attempt vision activation for models we know are multimodal.
        // Calling enableVision() on a text-only model can SIGABRT inside LiteRT-LM
        // during init — uncatchable from Kotlin.
        if (!_selectedModel.value.visionCapable) return@withLock false
        if (!modelDownloader.isModelDownloaded()) return@withLock false
        gemmaManager.enableVision(modelDownloader.getModelPath())
    }

    /**
     * Switches to the given model option, downloads it if missing, and
     * re-initializes the engine. Persists the selection so it survives restarts.
     * Caller is responsible for asking the user before triggering a large download.
     */
    suspend fun switchModel(option: GemmaModelOption): Boolean {
        if (_selectedModel.value == option && _status.value is GemmaPreparationStatus.Ready) return true
        _selectedModel.value = option
        modelPreference?.write(option.id)
        // Force re-prep so the new URL is downloaded and engine reinitialized.
        return prepareInternal(force = true)
    }

    private suspend fun prepareInternal(force: Boolean): Boolean = mutex.withLock {
        if (!force && _status.value is GemmaPreparationStatus.Ready) return@withLock true

        // Reclaim disk from model files saved by older downloader versions before
        // deciding whether to (re-)download. Cheap: a single listFiles() over a
        // tiny directory. Failure is non-fatal — the rest of prepare can still run.
        runCatching { modelDownloader.purgeOrphanFiles() }

        val targetUrl = _selectedModel.value.url

        val modelReady = runCatching {
            if (!modelDownloader.isModelDownloaded() || force) {
                _status.value = GemmaPreparationStatus.Downloading
                downloadWithProgress(targetUrl)
            } else {
                _downloadProgress.value = null
                true
            }
        }.getOrDefault(false)

        if (!modelReady || !modelDownloader.isModelDownloaded()) {
            _status.value = GemmaPreparationStatus.Unavailable(
                "Sin conexión a internet. Conectate a WiFi para descargar el modelo."
            )
            return@withLock false
        }

        _status.value = GemmaPreparationStatus.Preparing
        _downloadProgress.value = null

        val initialized = runCatching {
            if (force) gemmaManager.close()
            // Boot directly into vision mode for vision-capable models. The post-init
            // upgrade path in enableVision() loads a second engine copy in memory before
            // closing the first one, which OOM-kills the process on most phones.
            gemmaManager.initialize(
                modelPath = modelDownloader.getModelPath(),
                preferVision = _selectedModel.value.visionCapable,
            )
            true
        }.getOrDefault(false)

        val ready = initialized && runCatching { gemmaManager.isInitialized.firstOrFalse() }.getOrDefault(false)
        _status.value = if (ready) GemmaPreparationStatus.Ready else GemmaPreparationStatus.Unavailable("Gemma init failed")
        ready
    }

    // Downloads the model while forwarding real-time progress to _downloadProgress.
    private suspend fun downloadWithProgress(url: String): Boolean = coroutineScope {
        val progressForwarder = launch {
            modelDownloader.downloadProgress.collect { p ->
                _downloadProgress.value = p?.coerceIn(0, 100)
            }
        }
        val success = modelDownloader.downloadModel(url).isSuccess
        progressForwarder.cancel()
        _downloadProgress.value = if (success) 100 else null
        success && modelDownloader.isModelDownloaded()
    }
}

/**
 * Persists the user's chosen Gemma model id across launches.
 */
interface GemmaModelPreference {
    fun read(): GemmaModelOption
    fun write(id: String)
}

private suspend fun kotlinx.coroutines.flow.Flow<Boolean>.firstOrFalse(): Boolean =
    runCatching { first() }.getOrDefault(false)
