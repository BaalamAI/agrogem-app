package com.agrogem.app.data

import androidx.compose.runtime.Immutable

/**
 * Represents an image captured from the camera or selected from the gallery.
 * [uri] is a platform-specific URI string (e.g. content:// on Android).
 */
@Immutable
data class ImageResult(
    val uri: String,
    val timestamp: Long = 0L,
    val bytes: ByteArray? = null,
)
