package com.agrogem.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// STUB iOS: la POC no necesita geolocalización. Implementación real
// (CoreLocation) pendiente — ver historial de la rama si se necesita.
@Composable
actual fun rememberLocationPermissionRequester(
    onResult: (Boolean) -> Unit,
): LocationPermissionRequester = remember(onResult) {
    object : LocationPermissionRequester {
        override fun request() {
            onResult(false)
        }
    }
}
