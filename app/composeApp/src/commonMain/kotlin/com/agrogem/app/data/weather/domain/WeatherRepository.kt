package com.agrogem.app.data.weather.domain

import androidx.compose.runtime.Immutable
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.weather.api.WeatherApi
import com.agrogem.app.data.weather.api.WeatherResponse
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MATERIAL_LOCATION_DISTANCE_KM = 1.0

@Immutable
@Serializable
data class CurrentWeather(
    val temperatureCelsius: String,
    val humidity: String,
    val precipitation: String,
    val weatherCode: Int?,
    val windSpeed: String,
    val maxMin: String,
    val uvIndex: String,
    val description: String,
    val dateLabel: String,
)

@Serializable
data class WeatherCacheEntry(
    val lat: Double,
    val lon: Double,
    val weather: CurrentWeather,
)

interface WeatherRepository {
    suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather>
    suspend fun getCachedWeather(latLng: LatLng): CurrentWeather?
    suspend fun shouldRefresh(latLng: LatLng): Boolean
}

class WeatherRepositoryImpl(
    private val api: WeatherApi,
    private val cacheStore: WeatherCacheStore,
) : WeatherRepository {

    override suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather> {
        return try {
            val dto = api.getCurrentWeather(latLng.latitude, latLng.longitude)
            val mapped = mapWeatherDto(dto)
            cacheStore.write(
                WeatherCacheEntry(
                    lat = latLng.latitude,
                    lon = latLng.longitude,
                    weather = mapped,
                )
            )
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCachedWeather(latLng: LatLng): CurrentWeather? {
        val cached = cacheStore.read() ?: return null
        return if (distanceKm(cached.lat, cached.lon, latLng.latitude, latLng.longitude) <= MATERIAL_LOCATION_DISTANCE_KM) {
            cached.weather
        } else {
            null
        }
    }

    override suspend fun shouldRefresh(latLng: LatLng): Boolean {
        val cached = cacheStore.read() ?: return true
        return distanceKm(cached.lat, cached.lon, latLng.latitude, latLng.longitude) > MATERIAL_LOCATION_DISTANCE_KM
    }
}

internal fun mapWeatherDto(dto: WeatherResponse): CurrentWeather {
    val current = dto.current
    val daily = dto.daily
    val max = daily?.temperature2mMax?.firstOrNull()?.toInt()
    val min = daily?.temperature2mMin?.firstOrNull()?.toInt()
    val weatherCode = current?.weatherCode
    val interpretation = dto.interpretation?.trim().orEmpty()
    return CurrentWeather(
        temperatureCelsius = current?.temperature2m?.let { "${it.toInt()}°C" } ?: "--",
        humidity = current?.relativeHumidity2m?.let { "${it}%" } ?: "--",
        precipitation = current?.precipitation?.let { "${it} mm" } ?: "--",
        weatherCode = weatherCode,
        windSpeed = current?.windSpeed10m?.let { "${it.toInt()} km/h" } ?: "--",
        maxMin = if (max != null && min != null) "$max°/$min°" else "--",
        uvIndex = daily?.uvIndexMax?.firstOrNull()?.let { formatOneDecimal(it) } ?: "--",
        description = weatherCodeToText(weatherCode).ifBlank {
            if (interpretation.length > 42) interpretation.take(42) + "…" else interpretation.ifBlank { "--" }
        },
        dateLabel = current?.time ?: "--",
    )
}

private fun weatherCodeToText(code: Int?): String = when (code) {
    0 -> "Despejado"
    1, 2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    51, 53, 55, 56, 57 -> "Llovizna"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Lluvia"
    71, 73, 75, 77, 85, 86 -> "Nieve"
    95, 96, 99 -> "Tormenta"
    else -> ""
}

private fun formatOneDecimal(value: Double): String {
    val scaled = (value * 10).toInt()
    return "${scaled / 10}.${scaled % 10}"
}

private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(toRadians(lat1)) * cos(toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

private fun toRadians(degrees: Double): Double = degrees * PI / 180.0
