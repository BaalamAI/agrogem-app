package com.agrogem.app.data.location

import com.agrogem.app.data.shared.domain.LatLng

interface DeviceLocationProvider {
    suspend fun getCurrentLatLng(): Result<LatLng>
}

expect fun createDeviceLocationProvider(): DeviceLocationProvider
