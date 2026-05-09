package com.agrogem.app.data.environment.domain

import com.agrogem.app.data.environment.api.EnvironmentApi
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.domain.SoilProfile
import com.agrogem.app.data.soil.domain.mapSoilResponse
import com.agrogem.app.data.weather.domain.CurrentWeather
import com.agrogem.app.data.weather.domain.mapWeatherDto

data class EnvironmentData(
    val weather: CurrentWeather? = null,
    val soil: SoilProfile? = null,
)

interface EnvironmentRepository {
    suspend fun getEnvironment(latLng: LatLng): Result<EnvironmentData>
}

class EnvironmentRepositoryImpl(
    private val api: EnvironmentApi,
) : EnvironmentRepository {

    override suspend fun getEnvironment(latLng: LatLng): Result<EnvironmentData> {
        return try {
            val dto = api.getEnvironment(latLng.latitude, latLng.longitude)
            Result.success(
                EnvironmentData(
                    weather = dto.weather?.let { mapWeatherDto(it) },
                    soil = dto.soil?.let { mapSoilResponse(it) },
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
