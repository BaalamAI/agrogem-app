package com.agrogem.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.CoreLocation.CLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.CLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.CLAuthorizationStatusDenied
import platform.CoreLocation.CLAuthorizationStatusNotDetermined
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSObject

@Composable
actual fun rememberLocationPermissionRequester(
    onResult: (Boolean) -> Unit,
): LocationPermissionRequester {
    return remember(onResult) {
        object : LocationPermissionRequester {
            private var authorizationDelegate: NSObject? = null
            private var locationManager: CLLocationManager? = null

            override fun request() {
                val manager = CLLocationManager()
                locationManager = manager
                val status = CLLocationManager.authorizationStatus()
                when (status) {
                    CLAuthorizationStatusAuthorizedAlways,
                    CLAuthorizationStatusAuthorizedWhenInUse,
                    -> onResult(true)

                    CLAuthorizationStatusDenied -> onResult(false)

                    CLAuthorizationStatusNotDetermined -> {
                        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                                val updatedStatus = CLLocationManager.authorizationStatus()
                                val granted = updatedStatus == CLAuthorizationStatusAuthorizedAlways ||
                                    updatedStatus == CLAuthorizationStatusAuthorizedWhenInUse
                                if (updatedStatus != CLAuthorizationStatusNotDetermined) {
                                    onResult(granted)
                                    manager.delegate = null
                                    authorizationDelegate = null
                                    locationManager = null
                                }
                            }
                        }
                        authorizationDelegate = delegate
                        manager.delegate = delegate
                        manager.requestWhenInUseAuthorization()
                    }

                    else -> onResult(false)
                }
            }
        }
    }
}
