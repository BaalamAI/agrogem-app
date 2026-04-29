package com.agrogem.app.data.weather.domain

interface WeatherCacheStore {
    suspend fun read(): WeatherCacheEntry?
    suspend fun write(entry: WeatherCacheEntry)
}

expect fun createWeatherCacheStore(): WeatherCacheStore
