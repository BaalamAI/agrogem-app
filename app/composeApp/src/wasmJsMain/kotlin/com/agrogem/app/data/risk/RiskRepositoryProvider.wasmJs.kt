package com.agrogem.app.data.risk

import com.agrogem.app.data.risk.domain.DiseaseRisk
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.shared.domain.LatLng

actual fun createRiskRepository(): RiskRepository = NoOpRiskRepository()

private class NoOpRiskRepository : RiskRepository {
    override suspend fun getDiseaseRisks(latLng: LatLng?): Result<List<DiseaseRisk>> =
        Result.failure(UnsupportedOperationException("Risk not supported on this platform"))

    override suspend fun getPestRisks(latLng: LatLng?): Result<List<DiseaseRisk>> =
        Result.failure(UnsupportedOperationException("Risk not supported on this platform"))
}
