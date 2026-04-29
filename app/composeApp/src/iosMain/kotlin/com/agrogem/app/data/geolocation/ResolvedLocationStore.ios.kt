package com.agrogem.app.data.geolocation

import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

private const val KEY_RESOLVED = "resolved_location"

actual class ResolvedLocationStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val state = MutableStateFlow<ResolvedLocation?>(readCurrent())

    actual fun observe(): Flow<ResolvedLocation?> = state

    actual suspend fun write(location: ResolvedLocation) {
        defaults.setObject(Json.encodeToString(location), forKey = KEY_RESOLVED)
        state.value = location
    }

    actual suspend fun clear() {
        defaults.removeObjectForKey(KEY_RESOLVED)
        state.value = null
    }

    private fun readCurrent(): ResolvedLocation? {
        val json = defaults.stringForKey(KEY_RESOLVED)
        return json?.let { Json.decodeFromString<ResolvedLocation>(it) }
    }
}
