package com.agrogem.app.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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

    override fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        this.onPartialResult = onPartialResult
        this.onFinalResult = onFinalResult
        this.onError = onError

        val r = recognizer ?: run {
            onError("El reconocimiento de voz no está disponible en este dispositivo")
            return
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = when (error) {
                    AndroidSpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    AndroidSpeechRecognizer.ERROR_CLIENT -> "Error del reconocedor"
                    AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                    AndroidSpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    AndroidSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red"
                    AndroidSpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció el habla"
                    AndroidSpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    AndroidSpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó habla"
                    else -> "Error desconocido en el reconocimiento"
                }
                this@AndroidSpeechRecognizerImpl.onError?.invoke(message)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                this@AndroidSpeechRecognizerImpl.onFinalResult?.invoke(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                this@AndroidSpeechRecognizerImpl.onPartialResult?.invoke(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            r.startListening(intent)
        } catch (e: Exception) {
            onError("No se pudo iniciar el reconocimiento: ${e.message}")
        }
    }

    override fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {}
    }

    override fun cancel() {
        try {
            recognizer?.cancel()
        } catch (_: Exception) {}
    }
}
