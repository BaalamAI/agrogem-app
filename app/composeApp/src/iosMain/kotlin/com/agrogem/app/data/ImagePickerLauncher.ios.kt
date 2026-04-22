package com.agrogem.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (ImageResult?) -> Unit,
): ImagePickerLauncher = remember {
    object : ImagePickerLauncher {
        override fun launchCamera() {
            // iOS stub — real implementation pending
        }

        override fun launchGallery() {
            // iOS stub — real implementation pending
        }
    }
}
