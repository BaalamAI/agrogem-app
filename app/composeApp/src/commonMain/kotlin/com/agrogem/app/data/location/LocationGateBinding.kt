package com.agrogem.app.data.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.agrogem.app.data.rememberLocationPermissionRequester

@Composable
fun BindLocationGate() {
    val requester = rememberLocationPermissionRequester { granted ->
        LocationGate.onPermissionResult(granted)
    }
    LaunchedEffect(requester) {
        LocationGate.requests.collect {
            requester.request()
        }
    }
}
