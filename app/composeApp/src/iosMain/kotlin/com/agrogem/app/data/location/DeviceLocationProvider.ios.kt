package com.agrogem.app.data.location

import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSError
import platform.Foundation.NSObject
import kotlin.coroutines.resume

actual fun createDeviceLocationProvider(): DeviceLocationProvider = IosDeviceLocationProvider()

private class IosDeviceLocationProvider : DeviceLocationProvider {
    override suspend fun getCurrentLatLng(): Result<LatLng> = suspendCancellableCoroutine { continuation ->
        val manager = CLLocationManager()
        val delegate = OneShotLocationDelegate(
            onSuccess = { location ->
                if (continuation.isActive) {
                    continuation.resume(Result.success(LatLng(location.coordinate.latitude, location.coordinate.longitude)))
                }
            },
            onError = { error ->
                if (continuation.isActive) {
                    continuation.resume(Result.failure(Exception(error.localizedDescription ?: "Location unavailable")))
                }
            },
        )
        manager.delegate = delegate
        manager.requestLocation()
        continuation.invokeOnCancellation {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }
}

private class OneShotLocationDelegate(
    private val onSuccess: (CLLocation) -> Unit,
    private val onError: (NSError) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        onSuccess(location)
        manager.stopUpdatingLocation()
        manager.delegate = null
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onError(didFailWithError)
        manager.stopUpdatingLocation()
        manager.delegate = null
    }
}
