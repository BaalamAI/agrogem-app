package com.agrogem.app.data

import android.content.Context
import android.util.Log
import com.agrogem.app.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "GemmaDownloader"
private const val BUFFER_SIZE = 64 * 1024 // 64 KB
private const val MAX_ATTEMPTS = 6
private const val INITIAL_BACKOFF_MS = 1_500L
private const val MAX_BACKOFF_MS = 30_000L

class AndroidGemmaModelDownloader(private val context: Context) : GemmaModelDownloader {

    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(modelDir, "model.litertlm")
    private val urlStampFile = File(modelDir, "model.url")
    private val tempFile = File(modelDir, "model.litertlm.tmp")
    private val tempUrlStampFile = File(modelDir, "model.litertlm.tmp.url")

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    override val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    override suspend fun downloadModel(url: String): Result<String> = withContext(Dispatchers.IO) {
        val savedUrl = if (urlStampFile.exists()) urlStampFile.readText().trim() else ""
        if (modelFile.exists() && savedUrl == url) return@withContext Result.success(modelFile.absolutePath)
        if (modelFile.exists() && savedUrl != url) {
            Log.i(TAG, "Model URL changed ($savedUrl -> $url), deleting old model")
            modelFile.delete()
            urlStampFile.delete()
        }

        if (!modelDir.exists()) modelDir.mkdirs()

        // If a partial exists for a different URL, scrap it.
        val partialUrl = if (tempUrlStampFile.exists()) tempUrlStampFile.readText().trim() else ""
        if (tempFile.exists() && partialUrl != url) {
            Log.i(TAG, "Partial download is for a different URL — discarding (${tempFile.length()} bytes)")
            tempFile.delete()
            tempUrlStampFile.delete()
        }
        tempUrlStampFile.writeText(url)

        _downloadProgress.value = 0

        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < MAX_ATTEMPTS) {
            attempt++
            val startBytes = if (tempFile.exists()) tempFile.length() else 0L
            try {
                Log.i(
                    TAG,
                    if (startBytes > 0) "Attempt $attempt: resuming from ${startBytes / (1024 * 1024)} MB"
                    else "Attempt $attempt: starting download from $url",
                )
                runDownloadAttempt(url, startBytes)
                // Success — move temp to final.
                if (tempFile.renameTo(modelFile)) {
                    urlStampFile.writeText(url)
                    tempUrlStampFile.delete()
                    _downloadProgress.value = 100
                    Log.i(TAG, "Download complete: ${modelFile.absolutePath}")
                    return@withContext Result.success(modelFile.absolutePath)
                } else {
                    tempFile.delete()
                    tempUrlStampFile.delete()
                    Log.e(TAG, "Failed to rename temp file")
                    return@withContext Result.failure(IOException("Failed to rename downloaded file"))
                }
            } catch (e: NonRecoverableDownloadException) {
                Log.e(TAG, "Non-recoverable download error: ${e.message}")
                tempFile.delete()
                tempUrlStampFile.delete()
                _downloadProgress.value = null
                return@withContext Result.failure(e)
            } catch (e: IOException) {
                lastError = e
                Log.w(TAG, "Attempt $attempt failed (${e.javaClass.simpleName}: ${e.message}). Partial: ${tempFile.length()} bytes")
                if (attempt >= MAX_ATTEMPTS) break
                val backoff = (INITIAL_BACKOFF_MS shl (attempt - 1)).coerceAtMost(MAX_BACKOFF_MS)
                Log.i(TAG, "Retrying in ${backoff}ms (attempt ${attempt + 1}/$MAX_ATTEMPTS)")
                delay(backoff)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected download failure: ${e.message}", e)
                tempFile.delete()
                tempUrlStampFile.delete()
                _downloadProgress.value = null
                return@withContext Result.failure(e)
            }
        }

        // Exhausted retries — keep the partial on disk so a future call can resume.
        _downloadProgress.value = null
        Log.e(TAG, "Download failed after $MAX_ATTEMPTS attempts. Partial preserved at ${tempFile.absolutePath}")
        Result.failure(lastError ?: IOException("Download failed after $MAX_ATTEMPTS attempts"))
    }

    private fun runDownloadAttempt(url: String, startBytes: Long) {
        val rangeHeader = if (startBytes > 0) "bytes=$startBytes-" else null
        val connection = openWithRedirects(url, rangeHeader)
        try {
            val code = connection.responseCode
            val resumed = code == HttpURLConnection.HTTP_PARTIAL
            val totalBytes: Long = when {
                resumed -> {
                    // Content-Range: bytes start-end/total
                    val cr = connection.getHeaderField("Content-Range")
                    val parsed = parseContentRangeTotal(cr)
                    val cl = connection.contentLengthLong
                    parsed ?: if (cl > 0) startBytes + cl else -1L
                }
                code == HttpURLConnection.HTTP_OK -> {
                    if (startBytes > 0) {
                        // Server ignored Range — must restart from zero.
                        Log.w(TAG, "Server returned 200 ignoring Range header — restarting from 0")
                        tempFile.delete()
                    }
                    connection.contentLengthLong
                }
                code == 416 -> {
                    // Requested range not satisfiable — likely partial >= full size; restart fresh.
                    Log.w(TAG, "Server returned 416 — discarding partial and restarting")
                    tempFile.delete()
                    throw IOException("Range not satisfiable; will retry from 0")
                }
                code in 400..499 -> throw NonRecoverableDownloadException("HTTP $code: ${connection.responseMessage}")
                else -> throw IOException("Unexpected HTTP $code: ${connection.responseMessage}")
            }

            val effectiveStart = if (code == HttpURLConnection.HTTP_OK) 0L else startBytes
            Log.i(
                TAG,
                "Stream open. resumed=$resumed start=${effectiveStart / (1024 * 1024)} MB total=${totalBytes / (1024 * 1024)} MB",
            )

            connection.inputStream.use { input ->
                FileOutputStream(tempFile, /* append = */ effectiveStart > 0).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalSoFar = effectiveStart
                    var lastLoggedPct = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalSoFar += bytesRead

                        if (totalBytes > 0) {
                            val pct = (totalSoFar * 100 / totalBytes).toInt()
                            if (pct != _downloadProgress.value) {
                                _downloadProgress.value = pct
                                if (pct % 5 == 0 && pct != lastLoggedPct) {
                                    Log.d(TAG, "Download: $pct% (${totalSoFar / (1024 * 1024)} MB / ${totalBytes / (1024 * 1024)} MB)")
                                    lastLoggedPct = pct
                                }
                            }
                        }
                    }
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    override fun isModelDownloaded(): Boolean {
        val exists = modelFile.exists() && urlStampFile.exists()
        Log.d(TAG, "Checking if model exists: $exists at ${modelFile.absolutePath}")
        return exists
    }

    override fun getModelPath(): String = modelFile.absolutePath

    override suspend fun purgeOrphanFiles() = withContext(Dispatchers.IO) {
        if (!modelDir.exists()) return@withContext
        val expected = setOf(
            modelFile.name,
            urlStampFile.name,
            tempFile.name,
            tempUrlStampFile.name,
        )
        modelDir.listFiles()?.forEach { file ->
            if (file.name !in expected) {
                val sizeMb = file.length() / (1024 * 1024)
                runCatching {
                    if (file.delete()) {
                        Log.i(TAG, "Cleaned orphan file ${file.name} (${sizeMb} MB)")
                    } else {
                        Log.w(TAG, "Failed to delete orphan file ${file.name}")
                    }
                }.onFailure { Log.w(TAG, "Error deleting orphan ${file.name}: ${it.message}") }
            }
        }
    }

    private fun openWithRedirects(
        urlString: String,
        rangeHeader: String?,
        maxRedirects: Int = 10,
    ): HttpURLConnection {
        var currentUrl = urlString
        repeat(maxRedirects) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            if (rangeHeader != null) conn.setRequestProperty("Range", rangeHeader)
            conn.connect()
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                currentUrl = if (location.startsWith("http")) location
                             else URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IOException("Too many redirects for $urlString")
    }

    private fun parseContentRangeTotal(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        // Format: "bytes 1234-5678/9999" or "bytes 1234-5678/*"
        val slash = header.lastIndexOf('/')
        if (slash < 0) return null
        val tail = header.substring(slash + 1).trim()
        if (tail == "*") return null
        return tail.toLongOrNull()
    }
}

private class NonRecoverableDownloadException(message: String) : IOException(message)

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
