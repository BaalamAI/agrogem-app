package com.agrogem.app.data

import android.content.Context
import android.util.Log
import com.agrogem.app.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private const val TAG = "GemmaDownloader"

class AndroidGemmaModelDownloader(private val context: Context) : GemmaModelDownloader {
    
    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(modelDir, "gemma-4-E2B-it.litertlm")
    private val tempFile = File(modelDir, "gemma-4-E2B-it.litertlm.tmp")

    override suspend fun downloadModel(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (modelFile.exists()) {
                return@withContext Result.success(modelFile.absolutePath)
            }
            
            if (!modelDir.exists()) modelDir.mkdirs()

            val workManager = androidx.work.WorkManager.getInstance(context)
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
                "GemmaDownload",
                androidx.work.ExistingWorkPolicy.KEEP,
                downloadRequest
            )
            
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
