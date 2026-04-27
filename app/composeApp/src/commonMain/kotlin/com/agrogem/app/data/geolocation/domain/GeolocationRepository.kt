package com.agrogem.app.data.geolocation.domain

import com.agrogem.app.data.geolocation.ResolvedLocationStore
import com.agrogem.app.data.geolocation.api.GeolocationApi
import com.agrogem.app.data.geolocation.api.ReverseGeocodeResponse
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.flow.Flow

interface GeolocationRepository {
    suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation>
    suspend fun saveResolvedLocation(location: ResolvedLocation)
    fun observeResolvedLocation(): Flow<ResolvedLocation?>
}

class GeolocationRepositoryImpl(
    private val api: GeolocationApi,
    private val store: ResolvedLocationStore,
) : GeolocationRepository {

    override suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation> {
        return try {
            val dto = api.reverseGeocode(latLng.latitude, latLng.longitude)
            val elevation = try {
                api.elevation(latLng.latitude, latLng.longitude).elevationMeters
            } catch (e: Exception) {
                null
            }
            val location = mapReverseGeocodeDto(dto, elevation)
            store.write(location)
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveResolvedLocation(location: ResolvedLocation) {
        store.write(location)
    }

    override fun observeResolvedLocation(): Flow<ResolvedLocation?> = store.observe()
}

internal fun mapReverseGeocodeDto(
    dto: ReverseGeocodeResponse,
    elevationMeters: Double? = null,
): ResolvedLocation {
    val primary = buildDisplayString(dto)
    return ResolvedLocation(
        coordinates = LatLng(dto.lat, dto.lng),
        display = LocationDisplay(
            primary = primary,
            municipality = dto.municipality,
            state = dto.state,
            country = dto.country,
        ),
        elevationMeters = elevationMeters,
    )
}

private fun buildDisplayString(dto: ReverseGeocodeResponse): String {
    val raw = dto.displayName
    if (!raw.isNullOrBlank()) return raw
    return "Ubicación desconocida"
}
