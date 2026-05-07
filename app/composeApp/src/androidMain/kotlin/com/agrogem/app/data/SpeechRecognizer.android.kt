package com.agrogem.app.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun rememberSpeechRecognizer(): SpeechRecognizer {
    val context = LocalContext.current
    return remember(context) { AndroidSpeechRecognizerImpl(context) }
}

private class AndroidSpeechRecognizerImpl(context: Context) : SpeechRecognizer {
    private val recognizer: AndroidSpeechRecognizer? =
        if (AndroidSpeechRecognizer.isRecognitionAvailable(context)) {
            AndroidSpeechRecognizer.createSpeechRecognizer(context)
        } else null

    private var onPartialResult: ((String) -> Unit)? = null
    private var onFinalResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onAmplitudeUpdate: ((Float) -> Unit)? = null

    // Keeps the last captured partial so stop() can return it even before onResults fires
    private var lastCapturedText: String = ""

    override fun start(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit,
        onAmplitudeUpdate: (Float) -> Unit,
    ) {
        lastCapturedText = ""
        this.onPartialResult = onPartialResult
        this.onFinalResult = onFinalResult
        this.onError = onError
        this.onAmplitudeUpdate = onAmplitudeUpdate

        val r = recognizer ?: run {
            onError("El reconocimiento de voz no está disponible en este dispositivo")
            return
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                this@AndroidSpeechRecognizerImpl.onAmplitudeUpdate?.invoke(normalized)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // If we already have partial text, deliver it as final instead of failing
                val partial = lastCapturedText
                if (partial.isNotBlank()) {
                    this@AndroidSpeechRecognizerImpl.onFinalResult?.invoke(partial)
                    return
                }
                val message = when (error) {
                    AndroidSpeechRecognizer.ERROR_AUDIO -> "Error de audio al grabar"
                    AndroidSpeechRecognizer.ERROR_CLIENT -> "Error del reconocedor"
                    AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                    AndroidSpeechRecognizer.ERROR_NETWORK,
                    AndroidSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin conexión para transcribir. Escribe tu pregunta."
                    AndroidSpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció el habla. Escribe tu pregunta."
                    AndroidSpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado. Intenta de nuevo."
                    AndroidSpeechRecognizer.ERROR_SERVER -> "Error del servidor de voz. Escribe tu pregunta."
                    AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó habla. Escribe tu pregunta."
                    else -> "No se pudo transcribir. Escribe tu pregunta."
                }
                this@AndroidSpeechRecognizerImpl.onError?.invoke(message)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotBlank()) lastCapturedText = text
                this@AndroidSpeechRecognizerImpl.onFinalResult?.invoke(lastCapturedText)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                if (text.isNotBlank()) lastCapturedText = text
                this@AndroidSpeechRecognizerImpl.onPartialResult?.invoke(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val deviceLanguage = Locale.getDefault().toLanguageTag()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, deviceLanguage)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        try {
            r.startListening(intent)
        } catch (e: Exception) {
            onError("No se pudo iniciar el reconocimiento: ${e.message}")
        }
    }

    override fun stop() {
        try {
            recognizer?.stopListening()
            // Deliver the last captured partial immediately so the ViewModel
            // doesn't read an empty inputText before onResults fires.
            val partial = lastCapturedText
            if (partial.isNotBlank()) {
                onFinalResult?.invoke(partial)
            }
        } catch (_: Exception) {}
    }

    override fun cancel() {
        lastCapturedText = ""
        try {
            recognizer?.cancel()
        } catch (_: Exception) {}
    }
}
