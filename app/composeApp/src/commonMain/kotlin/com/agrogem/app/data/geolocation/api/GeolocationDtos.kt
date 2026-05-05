package com.agrogem.app.data.geolocation.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodeHit(
    @SerialName("display_name")
    val name: String,
    val lat: Double,
    @SerialName("lon")
    val lng: Double,
    val municipality: String? = null,
    val state: String? = null,
    @SerialName("country_code")
    val country: String? = null,
    val interpretation: String? = null,
)

@Serializable
data class ReverseGeocodeResponse(
    @SerialName("display_name")
    val displayName: String? = null,
    val lat: Double,
    @SerialName("lon")
    val lng: Double,
    val municipality: String? = null,
    val state: String? = null,
    @SerialName("country_code")
    val country: String? = null,
)

@Serializable
data class ElevationResponse(
    @SerialName("elevation_m")
    val elevationMeters: Double? = null,
)
