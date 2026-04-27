package com.agrogem.app.data.geolocation

import android.content.Context.MODE_PRIVATE
import com.agrogem.app.AndroidAppContext
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "agrogem_geolocation"
private const val KEY_RESOLVED = "resolved_location"

actual class ResolvedLocationStore actual constructor() {
    private var fallback: ResolvedLocation? = null

    private fun prefsOrNull() =
        if (AndroidAppContext.isInitialized) {
            AndroidAppContext.context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        } else {
            null
        }

    actual fun observe(): Flow<ResolvedLocation?> = flow {
        emit(readCurrent())
    }

    actual suspend fun write(location: ResolvedLocation) {
        val prefs = prefsOrNull()
        if (prefs != null) {
            prefs.edit().putString(KEY_RESOLVED, Json.encodeToString(location)).apply()
        } else {
            fallback = location
        }
    }

    actual suspend fun clear() {
        val prefs = prefsOrNull()
        if (prefs != null) {
            prefs.edit().remove(KEY_RESOLVED).apply()
        } else {
            fallback = null
        }
    }

    private fun readCurrent(): ResolvedLocation? {
        val prefs = prefsOrNull()
        return if (prefs != null) {
            val json = prefs.getString(KEY_RESOLVED, null)
            json?.let { Json.decodeFromString<ResolvedLocation>(it) }
        } else {
            fallback
        }
    }
}
