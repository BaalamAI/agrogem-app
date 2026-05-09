package com.agrogem.app.data

import androidx.compose.runtime.Composable

interface SpeechRecognizer {
    fun start(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit,
        onAmplitudeUpdate: (Float) -> Unit = {},
    )

    fun stop()

    fun cancel()
}

@Composable
expect fun rememberSpeechRecognizer(): SpeechRecognizer
