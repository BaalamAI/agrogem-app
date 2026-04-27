package com.agrogem.app.data.soil

import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.domain.SoilProfile
import com.agrogem.app.data.soil.domain.SoilRepository

actual fun createSoilRepository(): SoilRepository = NoOpSoilRepository()

private class NoOpSoilRepository : SoilRepository {
    override suspend fun getSoil(latLng: LatLng): Result<SoilProfile> =
        Result.failure(UnsupportedOperationException("Soil not supported on this platform"))
}
