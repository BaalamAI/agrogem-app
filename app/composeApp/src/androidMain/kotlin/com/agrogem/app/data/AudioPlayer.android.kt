package com.agrogem.app.data

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual @Composable
fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { AndroidAudioPlayer(context) }

    DisposableEffect(player) {
        onDispose { player.stop() }
    }

    return player
}

private class AndroidAudioPlayer(
    private val context: Context,
) : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    override fun play(uri: String): Boolean {
        if (uri.isBlank()) return false
        stop()

        return runCatching {
            mediaPlayer = MediaPlayer().apply {
                if (uri.startsWith("content://") || uri.startsWith("file://")) {
                    setDataSource(context, Uri.parse(uri))
                } else {
                    setDataSource(uri)
                }
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
        }.isSuccess
    }

    override fun stop() {
        mediaPlayer?.runCatchingStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun MediaPlayer.runCatchingStop() {
        runCatching {
            if (isPlaying) stop()
        }
    }
}
