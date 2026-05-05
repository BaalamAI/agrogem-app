package com.agrogem.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.agrogem.app.AndroidAppContext
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createDeviceLocationProvider(): DeviceLocationProvider {
    check(AndroidAppContext.isInitialized) { "AndroidAppContext not initialized" }
    return AndroidDeviceLocationProvider(AndroidAppContext.context)
}

private const val LAST_KNOWN_MAX_AGE_NANOS = 5L * 60L * 1_000_000_000L // 5 minutos

private class AndroidDeviceLocationProvider(
    private val context: Context,
) : DeviceLocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLatLng(): Result<LatLng> {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.failure(IllegalStateException("LocationManager unavailable"))
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        val candidateProviders = buildList {
            if (hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
            if (hasFine && manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) add(LocationManager.PASSIVE_PROVIDER)
        }
        if (candidateProviders.isEmpty()) {
            return Result.failure(IllegalStateException("No location provider enabled"))
        }

        val now = SystemClock.elapsedRealtimeNanos()
        val freshLastKnown = candidateProviders
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.elapsedRealtimeNanos <= LAST_KNOWN_MAX_AGE_NANOS }
            .minByOrNull { it.accuracy.takeIf { acc -> acc > 0f } ?: Float.MAX_VALUE }
        if (freshLastKnown != null) {
            return Result.success(freshLastKnown.toLatLng())
        }

        val liveProviders = candidateProviders.filter { it != LocationManager.PASSIVE_PROVIDER }
        if (liveProviders.isEmpty()) {
            return Result.failure(IllegalStateException("No active location provider"))
        }

        return suspendCancellableCoroutine { continuation ->
            val listeners = mutableListOf<LocationListener>()
            val resolve: (Result<LatLng>) -> Unit = { result ->
                if (continuation.isActive) {
                    listeners.forEach { manager.removeUpdates(it) }
                    listeners.clear()
                    continuation.resume(result)
                }
            }

            liveProviders.forEach { providerName ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        resolve(Result.success(location.toLatLng()))
                    }

                    override fun onProviderDisabled(disabledProvider: String) {
                        // Ignoramos: si un provider se cae, esperamos al otro.
                    }
                }
                listeners += listener
                manager.requestLocationUpdates(providerName, 0L, 0f, listener, Looper.getMainLooper())
            }

            continuation.invokeOnCancellation {
                listeners.forEach { manager.removeUpdates(it) }
                listeners.clear()
            }
        }
    }
}

private fun Location.toLatLng(): LatLng = LatLng(latitude = latitude, longitude = longitude)
