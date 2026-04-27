package com.agrogem.app.data.climate.api

interface ClimateApi {
    suspend fun getClimateHistory(
        lat: Double,
        lon: Double,
        start: String,
        end: String,
        granularity: String,
    ): ClimateHistoryResponse
}
