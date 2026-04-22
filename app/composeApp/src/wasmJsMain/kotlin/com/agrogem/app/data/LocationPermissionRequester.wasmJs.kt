package com.agrogem.app.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberLocationPermissionRequester(
    onResult: (Boolean) -> Unit,
): LocationPermissionRequester {
    return object : LocationPermissionRequester {
        override fun request() {
            onResult(true)
        }
    }
}
