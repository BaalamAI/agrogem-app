package com.agrogem.app.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.ListenableWorker
import java.io.File
import java.net.URL

class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        val urlString = inputData.getString("url") ?: return ListenableWorker.Result.failure()
        val destinationPath = inputData.getString("destinationPath") ?: return ListenableWorker.Result.failure()
        val tempPath = "$destinationPath.tmp"

        val destinationFile = File(destinationPath)
        val tempFile = File(tempPath)

        try {
            Log.i("DownloadWorker", "Starting download from $urlString")
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connect()

            val contentLength = connection.contentLength
            Log.i("DownloadWorker", "Conexión establecida. Tamaño esperado: ${contentLength / (1024 * 1024)} MB")
            
            val inputStream = connection.getInputStream()
            val outputStream = tempFile.outputStream()

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastLoggedMb = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        val currentMb = totalBytesRead / (1024 * 1024)
                        if (currentMb >= lastLoggedMb + 5) { // Log cada 5MB
                            lastLoggedMb = currentMb
                            val progressPercent = if (contentLength > 0) " (${totalBytesRead * 100 / contentLength}%)" else ""
                            Log.d("DownloadWorker", "Progreso: $currentMb MB descargados$progressPercent")
                            
                            if (contentLength > 0) {
                                setProgress(workDataOf("progress" to (totalBytesRead * 100 / contentLength).toInt()))
                            }
                        }
                    }
                }
            }

            if (tempFile.renameTo(destinationFile)) {
                Log.i("DownloadWorker", "Download completed successfully")
                return ListenableWorker.Result.success()
            } else {
                Log.e("DownloadWorker", "Failed to rename temp file")
                return ListenableWorker.Result.failure()
            }

        } catch (e: Exception) {
            Log.e("DownloadWorker", "Download failed", e)
            if (tempFile.exists()) tempFile.delete()
            return ListenableWorker.Result.retry()
        }
    }
}
