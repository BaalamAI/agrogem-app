package com.agrogem.app.data

import androidx.compose.runtime.Composable

/**
 * Android implementation of [AudioRecorder] using MediaRecorder.
 * TODO: Full implementation with amplitude tracking (Phase 4)
 */
actual @Composable
fun rememberAudioRecorder(
    onAmplitudeUpdate: (Float) -> Unit,
): AudioRecorder = object : AudioRecorder {
    override fun startRecording() {
        // Stub: Phase 4 implementation
    }

    override fun stopRecording(): String? {
        // Stub: Phase 4 implementation
        return null
    }

    override fun cancelRecording() {
        // Stub: Phase 4 implementation
    }
}
