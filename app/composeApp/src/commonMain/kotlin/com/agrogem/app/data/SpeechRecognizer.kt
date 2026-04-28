package com.agrogem.app.data

import androidx.compose.runtime.Composable

interface SpeechRecognizer {
    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit,
    )

    fun stopListening()

    fun cancel()
}

@Composable
expect fun rememberSpeechRecognizer(): SpeechRecognizer
