package com.agrogem.app.data

import androidx.compose.runtime.Composable

/**
 * Platform abstraction for audio recording.
 * Each platform provides an actual implementation via [rememberAudioRecorder].
 */
interface AudioRecorder {
    /** Start capturing audio input. */
    fun startRecording()

    /**
     * Stop capturing audio and return the URI of the recorded file.
     * Returns null if recording failed or was cancelled.
     */
    fun stopRecording(): String?

    /** Cancel the current recording and clean up resources. */
    fun cancelRecording()
}

/**
 * Creates and remembers a platform-specific [AudioRecorder].
 * [onAmplitudeUpdate] is called periodically with the current audio amplitude (0.0 to 1.0).
 */
@Composable
expect fun rememberAudioRecorder(
    onAmplitudeUpdate: (Float) -> Unit,
): AudioRecorder
