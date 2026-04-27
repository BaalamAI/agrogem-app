package com.agrogem.app.data.soil.domain

import androidx.compose.runtime.Immutable

@Immutable
data class SoilProfile(
    val lat: Double,
    val lon: Double,
    val dominantTexture: String,
    val domainHorizons: List<Horizon>,
    val interpretation: String = "",
) {
    val summary: SoilSummary
        get() = SoilSummary(
            dominantTexture = dominantTexture,
            topHorizonPh = domainHorizons.firstOrNull()?.ph ?: 0.0,
        )
}

@Immutable
data class Horizon(
    val depth: String,
    val ph: Double,
    val textureClass: String,
    val socGPerKg: Double,
    val nitrogenGPerKg: Double = 0.0,
    val clayPct: Double = 0.0,
    val sandPct: Double = 0.0,
    val siltPct: Double = 0.0,
    val cecMmolPerKg: Double = 0.0,
)

@Immutable
data class SoilSummary(
    val dominantTexture: String,
    val topHorizonPh: Double,
)
