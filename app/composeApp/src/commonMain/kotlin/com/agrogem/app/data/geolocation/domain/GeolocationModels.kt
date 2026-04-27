package com.agrogem.app.data.geolocation.domain

import androidx.compose.runtime.Immutable
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ResolvedLocation(
    val coordinates: LatLng,
    val display: LocationDisplay,
    val elevationMeters: Double?,
)

@Immutable
@Serializable
data class LocationDisplay(
    val primary: String,
    val municipality: String?,
    val state: String?,
    val country: String?,
)
