package com.agrogem.app.data

import androidx.compose.runtime.Composable

interface LocationPermissionRequester {
    fun request()
}

@Composable
expect fun rememberLocationPermissionRequester(
    onResult: (Boolean) -> Unit,
): LocationPermissionRequester
