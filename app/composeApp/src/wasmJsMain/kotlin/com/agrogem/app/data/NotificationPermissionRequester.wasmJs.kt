package com.agrogem.app.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit,
): NotificationPermissionRequester {
    return object : NotificationPermissionRequester {
        override fun request() {
            onResult(true)
        }
    }
}
