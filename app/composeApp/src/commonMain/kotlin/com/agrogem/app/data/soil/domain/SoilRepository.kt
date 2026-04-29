package com.agrogem.app.data.soil.domain

import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.api.SoilApi
import com.agrogem.app.data.soil.api.SoilResponse

interface SoilRepository {
    suspend fun getSoil(latLng: LatLng): Result<SoilProfile>
}

class SoilRepositoryImpl(
    private val api: SoilApi,
) : SoilRepository {

    override suspend fun getSoil(latLng: LatLng): Result<SoilProfile> {
        return try {
            val dto = api.getSoil(latLng.latitude, latLng.longitude)
            Result.success(mapSoilResponse(dto))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

internal fun mapSoilResponse(dto: SoilResponse): SoilProfile {
    val domainHorizons = dto.horizons?.map { h ->
        Horizon(
            depth = h.depth ?: "",
            ph = h.ph,
            textureClass = h.textureClass ?: "",
            socGPerKg = h.socGPerKg ?: 0.0,
            nitrogenGPerKg = h.nitrogenGPerKg ?: 0.0,
            clayPct = h.clayPct ?: 0.0,
            sandPct = h.sandPct ?: 0.0,
            siltPct = h.siltPct ?: 0.0,
            cecMmolPerKg = h.cecMmolPerKg ?: 0.0,
        )
    } ?: emptyList()

    val dominantTexture = dto.dominantTexture
        ?: domainHorizons.firstOrNull()?.textureClass
        ?: ""

    return SoilProfile(
        lat = dto.lat ?: 0.0,
        lon = dto.lon ?: 0.0,
        dominantTexture = dominantTexture,
        domainHorizons = domainHorizons,
        interpretation = dto.interpretation ?: "",
    )
}
