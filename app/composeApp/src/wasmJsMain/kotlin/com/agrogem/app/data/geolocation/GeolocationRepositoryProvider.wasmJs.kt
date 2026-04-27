package com.agrogem.app.data.geolocation

import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createGeolocationRepository(): GeolocationRepository = NoOpGeolocationRepository()

private class NoOpGeolocationRepository : GeolocationRepository {
    override suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation> =
        Result.failure(UnsupportedOperationException("Geolocation not supported on this platform"))

    override suspend fun saveResolvedLocation(location: ResolvedLocation) {}

    override fun observeResolvedLocation(): Flow<ResolvedLocation?> = flowOf(null)
}
