package com.agrogem.app.data

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    val context = LocalContext.current
    return remember(context) { AndroidSpeechSynthesizer(context) }
}

private class AndroidSpeechSynthesizer(context: Context) : SpeechSynthesizer {
    private var isReady = false
    private var textToSpeech: TextToSpeech? = null

    private val tts = TextToSpeech(context) { status ->
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            textToSpeech?.language = Locale("es", "ES")
        }
    }.also { textToSpeech = it }

    override fun speak(text: String): Boolean {
        if (!isReady || text.isBlank()) return false
        tts.stop()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant_reply")
        return true
    }

    override fun stop() {
        tts.stop()
    }
}
