package com.agrogem.app.data

import androidx.compose.runtime.Composable

interface MicrophonePermissionRequester {
    fun request()
}

@Composable
expect fun rememberMicrophonePermissionRequester(
    onResult: (Boolean) -> Unit,
): MicrophonePermissionRequester
