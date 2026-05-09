package com.agrogem.app.data

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
actual fun rememberLocationPermissionRequester(
    onResult: (Boolean) -> Unit,
): LocationPermissionRequester {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        onResult(results.values.any { it })
    }

    return remember(launcher) {
        object : LocationPermissionRequester {
            override fun request() {
                launcher.launch(LOCATION_PERMISSIONS)
            }
        }
    }
}
