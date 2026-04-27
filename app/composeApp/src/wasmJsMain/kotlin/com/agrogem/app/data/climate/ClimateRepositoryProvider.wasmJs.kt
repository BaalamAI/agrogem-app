package com.agrogem.app.data.climate

import com.agrogem.app.data.climate.domain.ClimateHistory
import com.agrogem.app.data.climate.domain.ClimateRepository
import com.agrogem.app.data.shared.domain.LatLng

actual fun createClimateRepository(): ClimateRepository = NoOpClimateRepository()

private class NoOpClimateRepository : ClimateRepository {
    override suspend fun getClimateHistory(
        latLng: LatLng,
        start: String,
        end: String,
        granularity: String,
    ): Result<ClimateHistory> =
        Result.failure(UnsupportedOperationException("Climate not supported on this platform"))
}
