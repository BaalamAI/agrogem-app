package com.agrogem.app.data.environment.api

interface EnvironmentApi {
    suspend fun getEnvironment(lat: Double, lon: Double): EnvironmentResponseDto
}
