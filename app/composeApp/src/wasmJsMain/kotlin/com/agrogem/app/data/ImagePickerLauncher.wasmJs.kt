package com.agrogem.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (ImageResult?) -> Unit,
): ImagePickerLauncher = remember {
    object : ImagePickerLauncher {
        override fun launchCamera() {
            // WASM stub — camera not available in browser
        }

        override fun launchGallery() {
            // WASM stub — gallery picker not available in browser
        }
    }
}
