package com.agrogem.app.data

import android.content.Context
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.agrogem.app.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "GemmaDownloader"
private const val DOWNLOAD_WORK_NAME = "GemmaDownload"

class AndroidGemmaModelDownloader(private val context: Context) : GemmaModelDownloader {
    
    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(modelDir, "model.litertlm")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _downloadProgress = MutableStateFlow<Int?>(null)
    private var progressObserverJob: Job? = null
    override val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    override suspend fun downloadModel(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (modelFile.exists()) {
                return@withContext Result.success(modelFile.absolutePath)
            }
            
            if (!modelDir.exists()) modelDir.mkdirs()

            _downloadProgress.value = null
            val workManager = WorkManager.getInstance(context)
            val downloadRequest = androidx.work.OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        "url" to url,
                        "destinationPath" to modelFile.absolutePath
                    )
                )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .build()

            workManager.enqueueUniqueWork(
                DOWNLOAD_WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                downloadRequest
            )
            observeDownloadProgress()
            
            // We return success here as the task is enqueued. 
            // In a real app, you might want to observe the WorkInfo.
            Result.success(modelFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isModelDownloaded(): Boolean {
        val exists = modelFile.exists()
        Log.d(TAG, "Checking if model exists: $exists at ${modelFile.absolutePath}")
        return exists
    }

    override fun getModelPath(): String = modelFile.absolutePath

    private fun observeDownloadProgress() {
        if (progressObserverJob?.isActive == true) return

        progressObserverJob = scope.launch {
            val workManager = WorkManager.getInstance(context)
            while (true) {
                val workInfos = runCatching {
                    workManager.getWorkInfosForUniqueWork(DOWNLOAD_WORK_NAME).get()
                }.getOrDefault(emptyList())
                val current = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    ?: workInfos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
                    ?: workInfos.firstOrNull()

                _downloadProgress.value = when (current?.state) {
                    WorkInfo.State.SUCCEEDED -> 100
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    -> current.progress.getInt("progress", 0).coerceIn(0, 100)
                    else -> null
                }

                if (current == null || current.state.isFinished) return@launch
                delay(1_000)
            }
        }
    }
}

private var downloaderInstance: GemmaModelDownloader? = null

actual fun createGemmaModelDownloader(): GemmaModelDownloader {
    if (downloaderInstance == null) {
        if (!AndroidAppContext.isInitialized) {
             throw IllegalStateException("AndroidAppContext not initialized")
        }
        initializeGemmaModelDownloader(AndroidAppContext.context)
    }
    return downloaderInstance!!
}

fun initializeGemmaModelDownloader(context: Context) {
    if (downloaderInstance == null) {
        downloaderInstance = AndroidGemmaModelDownloader(context.applicationContext)
    }
}
