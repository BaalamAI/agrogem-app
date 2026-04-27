package com.agrogem.app.data.weather.domain

import androidx.compose.runtime.Immutable
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.weather.api.WeatherApi
import com.agrogem.app.data.weather.api.WeatherResponse

@Immutable
data class CurrentWeather(
    val temperatureCelsius: String,
    val humidity: String,
    val cloudCover: String,
    val uvIndex: String,
    val description: String,
    val locationName: String,
    val dateLabel: String,
)

interface WeatherRepository {
    suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather>
}

class WeatherRepositoryImpl(
    private val api: WeatherApi,
) : WeatherRepository {

    override suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather> {
        return try {
            val dto = api.getCurrentWeather(latLng.latitude, latLng.longitude)
            Result.success(mapWeatherDto(dto))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

internal fun mapWeatherDto(dto: WeatherResponse): CurrentWeather {
    return CurrentWeather(
        temperatureCelsius = dto.temperatureCelsius?.let { "${it}°C" } ?: "--",
        humidity = dto.humidityPercentage?.let { "${it.toInt()}%" } ?: "--",
        cloudCover = dto.cloudCoverPercentage?.let { "${it.toInt()}%" } ?: "--",
        uvIndex = dto.uvIndex?.toString() ?: "--",
        description = dto.description ?: "--",
        locationName = "",
        dateLabel = dto.dateTime ?: "--",
    )
}
