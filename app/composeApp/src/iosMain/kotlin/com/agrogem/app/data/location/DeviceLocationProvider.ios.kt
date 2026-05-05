package com.agrogem.app.data.location

import com.agrogem.app.data.shared.domain.LatLng

// STUB iOS: la POC no necesita geolocalización. Implementación real
// (CoreLocation) pendiente — ver historial de la rama si se necesita.
actual fun createDeviceLocationProvider(): DeviceLocationProvider = object : DeviceLocationProvider {
    override suspend fun getCurrentLatLng(): Result<LatLng> =
        Result.failure(Exception("Location no implementado en iOS (POC)"))
}
