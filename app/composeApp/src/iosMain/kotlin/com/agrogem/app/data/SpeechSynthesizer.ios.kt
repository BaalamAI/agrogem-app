package com.agrogem.app.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    return object : SpeechSynthesizer {
        override fun speak(text: String): Boolean = false

        override fun stop() {}
    }
}
