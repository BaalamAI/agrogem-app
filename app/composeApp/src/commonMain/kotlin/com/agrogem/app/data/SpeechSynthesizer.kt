package com.agrogem.app.data

import androidx.compose.runtime.Composable

interface SpeechSynthesizer {
    fun speak(text: String): Boolean

    fun stop()
}

@Composable
expect fun rememberSpeechSynthesizer(): SpeechSynthesizer
