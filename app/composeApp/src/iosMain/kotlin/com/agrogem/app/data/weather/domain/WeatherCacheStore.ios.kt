package com.agrogem.app.data.weather.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

private const val KEY_WEATHER = "last_weather"

private class IosWeatherCacheStore : WeatherCacheStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun read(): WeatherCacheEntry? {
        val json = defaults.stringForKey(KEY_WEATHER)
        return json?.let { Json.decodeFromString<WeatherCacheEntry>(it) }
    }

    override suspend fun write(entry: WeatherCacheEntry) {
        defaults.setObject(Json.encodeToString(entry), forKey = KEY_WEATHER)
    }
}

actual fun createWeatherCacheStore(): WeatherCacheStore = IosWeatherCacheStore()
