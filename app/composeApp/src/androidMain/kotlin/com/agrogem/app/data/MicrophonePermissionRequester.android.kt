package com.agrogem.app.data

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberMicrophonePermissionRequester(
    onResult: (Boolean) -> Unit,
): MicrophonePermissionRequester {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
    )

    return remember(launcher) {
        object : MicrophonePermissionRequester {
            override fun request() {
                when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                    PackageManager.PERMISSION_GRANTED -> onResult(true)
                    else -> launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
}
