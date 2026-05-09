package com.agrogem.app.data

import androidx.compose.runtime.Composable

interface AudioPlayer {
    fun play(uri: String): Boolean
    fun stop()
}

@Composable
expect fun rememberAudioPlayer(): AudioPlayer
