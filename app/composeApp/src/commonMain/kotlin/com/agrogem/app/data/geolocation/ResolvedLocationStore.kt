package com.agrogem.app.data.geolocation

import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import kotlinx.coroutines.flow.Flow

expect class ResolvedLocationStore() {
    fun observe(): Flow<ResolvedLocation?>
    suspend fun write(location: ResolvedLocation)
    suspend fun clear()
}
