package com.agrogem.app.data

import androidx.compose.runtime.Composable

/**
 * iOS stub implementation of [AudioRecorder] for MVP.
 * TODO: Full implementation (Phase 4)
 */
actual @Composable
fun rememberAudioRecorder(
    onAmplitudeUpdate: (Float) -> Unit,
): AudioRecorder = object : AudioRecorder {
    override fun start() {
        // Stub: iOS audio recording not implemented yet
    }

    override fun stop(): String? {
        // Stub: returns null for MVP
        return null
    }

    override fun cancel() {
        // Stub: iOS audio recording not implemented yet
    }
}
