package com.agrogem.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.agrogem.app.AndroidAppContext
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createDeviceLocationProvider(): DeviceLocationProvider {
    check(AndroidAppContext.isInitialized) { "AndroidAppContext not initialized" }
    return AndroidDeviceLocationProvider(AndroidAppContext.context)
}

private class AndroidDeviceLocationProvider(
    private val context: Context,
) : DeviceLocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLatLng(): Result<LatLng> {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.failure(IllegalStateException("LocationManager unavailable"))
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return Result.failure(IllegalStateException("No location provider enabled"))
        }

        manager.getLastKnownLocation(provider)?.let { last ->
            return Result.success(last.toLatLng())
        }

        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(location.toLatLng()))
                    }
                    manager.removeUpdates(this)
                }

                override fun onProviderDisabled(disabledProvider: String) {
                    if (disabledProvider == provider && continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException("Location provider disabled")))
                    }
                    manager.removeUpdates(this)
                }
            }

            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        }
    }
}

private fun Location.toLatLng(): LatLng = LatLng(latitude = latitude, longitude = longitude)
