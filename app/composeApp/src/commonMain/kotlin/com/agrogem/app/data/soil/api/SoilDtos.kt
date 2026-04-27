package com.agrogem.app.data.soil.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SoilResponse(
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("dominant_texture")
    val dominantTexture: String? = null,
    val horizons: List<HorizonDto>? = null,
    val interpretation: String? = null,
)

@Serializable
data class HorizonDto(
    val depth: String? = null,
    val ph: Double? = null,
    @SerialName("texture_class")
    val textureClass: String? = null,
    @SerialName("soc_g_per_kg")
    val socGPerKg: Double? = null,
    @SerialName("nitrogen_g_per_kg")
    val nitrogenGPerKg: Double? = null,
    @SerialName("clay_pct")
    val clayPct: Double? = null,
    @SerialName("sand_pct")
    val sandPct: Double? = null,
    @SerialName("silt_pct")
    val siltPct: Double? = null,
    @SerialName("cec_mmol_per_kg")
    val cecMmolPerKg: Double? = null,
)
