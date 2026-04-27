package com.agrogem.app.data.geolocation

import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_RESOLVED = "resolved_location"

actual class ResolvedLocationStore actual constructor() {
    private var memoryFallback: ResolvedLocation? = null
    private val storageAvailable = try {
        localStorage
        true
    } catch (_: Throwable) {
        false
    }

    actual fun observe(): Flow<ResolvedLocation?> = flow {
        emit(readCurrent())
    }

    actual suspend fun write(location: ResolvedLocation) {
        if (!storageAvailable) {
            memoryFallback = location
            return
        }
        try {
            localStorage.setItem(KEY_RESOLVED, Json.encodeToString(location))
        } catch (_: Throwable) {
            memoryFallback = location
        }
    }

    actual suspend fun clear() {
        if (!storageAvailable) {
            memoryFallback = null
            return
        }
        try {
            localStorage.removeItem(KEY_RESOLVED)
        } catch (_: Throwable) {
            memoryFallback = null
        }
    }

    private fun readCurrent(): ResolvedLocation? {
        if (!storageAvailable) return memoryFallback
        return try {
            val json = localStorage.getItem(KEY_RESOLVED)
            json?.let { Json.decodeFromString<ResolvedLocation>(it) }
        } catch (_: Throwable) {
            memoryFallback
        }
    }
}
