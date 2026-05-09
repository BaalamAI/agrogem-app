package com.agrogem.app.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

actual @Composable
fun rememberAudioRecorder(
    onAmplitudeUpdate: (Float) -> Unit,
): AudioRecorder {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val currentOnAmplitudeUpdate by rememberUpdatedState(onAmplitudeUpdate)
    val recorder = remember(context, scope) {
        AndroidAudioRecorder(
            context = context,
            scope = scope,
            onAmplitudeUpdate = { currentOnAmplitudeUpdate(it) },
        )
    }

    DisposableEffect(recorder) {
        onDispose { recorder.cancel() }
    }

    return recorder
}

private class AndroidAudioRecorder(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onAmplitudeUpdate: (Float) -> Unit,
) : AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var amplitudeJob: Job? = null

    override fun start() {
        cancel()

        val audioDir = File(context.cacheDir, "audio").apply { mkdirs() }
        val file = File.createTempFile("agrogem_voice_", ".m4a", audioDir)
        val recorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
        }

        try {
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            outputFile = file
            startAmplitudeUpdates()
        } catch (_: Exception) {
            recorder.release()
            file.delete()
            mediaRecorder = null
            outputFile = null
        }
    }

    override fun stop(): String? {
        val recorder = mediaRecorder ?: return null
        val file = outputFile
        stopAmplitudeUpdates()

        val stopped = runCatching { recorder.stop() }.isSuccess
        recorder.release()
        mediaRecorder = null
        outputFile = null

        if (!stopped || file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            return null
        }

        return file.toURI().toString()
    }

    override fun cancel() {
        stopAmplitudeUpdates()
        mediaRecorder?.runCatchingStop()
        mediaRecorder?.release()
        mediaRecorder = null
        outputFile?.delete()
        outputFile = null
        onAmplitudeUpdate(0f)
    }

    private fun startAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive) {
                val amplitude = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                onAmplitudeUpdate((amplitude / Short.MAX_VALUE.toFloat()).coerceIn(0f, 1f))
                delay(100)
            }
        }
    }

    private fun stopAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        onAmplitudeUpdate(0f)
    }

    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun MediaRecorder.runCatchingStop() {
        runCatching { stop() }
    }
}
