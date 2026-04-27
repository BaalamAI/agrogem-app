package com.agrogem.app.data.geolocation.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodeHit(
    val name: String,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class ReverseGeocodeResponse(
    @SerialName("display_name")
    val displayName: String? = null,
    val lat: Double,
    val lng: Double,
    val municipality: String? = null,
    val state: String? = null,
    val country: String? = null,
)

@Serializable
data class ElevationResponse(
    @SerialName("elevation_meters")
    val elevationMeters: Double? = null,
)
