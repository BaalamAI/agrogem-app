package com.agrogem.app.data.weather.domain

import android.content.Context.MODE_PRIVATE
import com.agrogem.app.AndroidAppContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "agrogem_weather"
private const val KEY_WEATHER = "last_weather"

private class AndroidWeatherCacheStore : WeatherCacheStore {
    private var fallback: WeatherCacheEntry? = null

    private fun prefsOrNull() =
        if (AndroidAppContext.isInitialized) {
            AndroidAppContext.context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        } else {
            null
        }

    override suspend fun read(): WeatherCacheEntry? {
        val prefs = prefsOrNull()
        return if (prefs != null) {
            prefs.getString(KEY_WEATHER, null)?.let { Json.decodeFromString<WeatherCacheEntry>(it) }
        } else {
            fallback
        }
    }

    override suspend fun write(entry: WeatherCacheEntry) {
        val prefs = prefsOrNull()
        if (prefs != null) {
            prefs.edit().putString(KEY_WEATHER, Json.encodeToString(entry)).apply()
        } else {
            fallback = entry
        }
    }
}

actual fun createWeatherCacheStore(): WeatherCacheStore = AndroidWeatherCacheStore()
