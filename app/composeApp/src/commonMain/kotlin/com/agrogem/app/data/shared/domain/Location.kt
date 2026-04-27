package com.agrogem.app.data.shared.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class LatLng(val latitude: Double, val longitude: Double)

@Immutable
data class LocationInfo(
    val name: String,
    val latLng: LatLng,
    val elevationMeters: Double?
)

sealed interface LocationResult {
    data class Success(val info: LocationInfo) : LocationResult
    data object NotFound : LocationResult
    data class Failure(val cause: Throwable) : LocationResult
}
