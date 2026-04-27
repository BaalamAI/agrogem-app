package com.agrogem.app.data.weather

import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.weather.domain.CurrentWeather
import com.agrogem.app.data.weather.domain.WeatherRepository

actual fun createWeatherRepository(): WeatherRepository = NoOpWeatherRepository()

private class NoOpWeatherRepository : WeatherRepository {
    override suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather> =
        Result.failure(UnsupportedOperationException("Weather not supported on this platform"))
}
