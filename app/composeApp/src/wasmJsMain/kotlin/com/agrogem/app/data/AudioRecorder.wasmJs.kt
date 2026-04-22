package com.agrogem.app.data

import androidx.compose.runtime.Composable

/**
 * Web/WASM implementation of [AudioRecorder].
 * Audio recording is not supported in WASM — stub returns no-op recorder.
 */
actual @Composable
fun rememberAudioRecorder(
    onAmplitudeUpdate: (Float) -> Unit,
): AudioRecorder = object : AudioRecorder {
    override fun startRecording() {
        // Not supported in WASM
    }

    override fun stopRecording(): String? {
        return null
    }

    override fun cancelRecording() {
        // No-op
    }
}
