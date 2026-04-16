package com.agrogem.app.data

import androidx.compose.runtime.Composable

/**
 * Platform abstraction for launching the device camera or photo gallery.
 * Each platform provides an actual implementation via [rememberImagePickerLauncher].
 */
interface ImagePickerLauncher {
    fun launchCamera()
    fun launchGallery()
}

/**
 * Creates and remembers a platform-specific [ImagePickerLauncher].
 * [onResult] is called with the selected/captured image, or null if cancelled.
 */
@Composable
expect fun rememberImagePickerLauncher(
    onResult: (ImageResult?) -> Unit,
): ImagePickerLauncher
