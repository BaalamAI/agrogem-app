package com.agrogem.app.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSpeechRecognizer(): SpeechRecognizer {
    return object : SpeechRecognizer {
        override fun startListening(
            onPartialResult: (String) -> Unit,
            onFinalResult: (String) -> Unit,
            onError: (String) -> Unit,
        ) {
            onError("El reconocimiento de voz no está disponible en iOS")
        }

        override fun stopListening() {}

        override fun cancel() {}
    }
}
