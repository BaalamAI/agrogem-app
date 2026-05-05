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
    override fun start() {
        // Stub: Phase 4 implementation
    }

    override fun stop(): String? {
        // Stub: Phase 4 implementation
        return null
    }

    override fun cancel() {
        // Stub: Phase 4 implementation
    }
}
