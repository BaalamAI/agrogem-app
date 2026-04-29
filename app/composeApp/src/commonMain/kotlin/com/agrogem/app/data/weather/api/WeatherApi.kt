package com.agrogem.app.data.weather.api

interface WeatherApi {
    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse
}
