package com.agrogem.app.data

import androidx.compose.runtime.Composable

interface NotificationPermissionRequester {
    fun request()
}

@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit,
): NotificationPermissionRequester
