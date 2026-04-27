package com.agrogem.app.data.geolocation.api

interface GeolocationApi {
    suspend fun geocode(query: String): List<GeocodeHit>
    suspend fun reverseGeocode(lat: Double, lng: Double): ReverseGeocodeResponse
    suspend fun elevation(lat: Double, lng: Double): ElevationResponse
}
