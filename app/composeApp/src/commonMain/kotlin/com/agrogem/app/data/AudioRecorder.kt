package com.agrogem.app.data

import androidx.compose.runtime.Composable

/**
 * Platform abstraction for audio recording.
 * Each platform provides an actual implementation via [rememberAudioRecorder].
 */
interface AudioRecorder {
    fun start()

    /**
     * Stops capturing audio and returns the URI of the recorded file,
     * or null if recording failed or was cancelled.
     */
    fun stop(): String?

    fun cancel()
}

/**
 * Creates and remembers a platform-specific [AudioRecorder].
 * [onAmplitudeUpdate] is called periodically with the current audio amplitude (0.0 to 1.0).
 */
@Composable
expect fun rememberAudioRecorder(
    onAmplitudeUpdate: (Float) -> Unit,
): AudioRecorder
