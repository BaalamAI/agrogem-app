package com.agrogem.app.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMicrophonePermissionRequester(
    onResult: (Boolean) -> Unit,
): MicrophonePermissionRequester {
    return object : MicrophonePermissionRequester {
        override fun request() {
            onResult(true)
        }
    }
}
