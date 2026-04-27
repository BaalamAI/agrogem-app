package com.agrogem.app.data.soil.api

interface SoilApi {
    suspend fun getSoil(lat: Double, lon: Double): SoilResponse
}
