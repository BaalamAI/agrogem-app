package com.agrogem.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual @Composable
fun rememberAudioPlayer(): AudioPlayer {
    return remember {
        object : AudioPlayer {
            override fun play(uri: String): Boolean = false
            override fun stop() = Unit
        }
    }
}
