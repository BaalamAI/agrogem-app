package com.agrogem.app.data.climate.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClimateHistoryResponse(
    val lat: Double? = null,
    val lon: Double? = null,
    val granularity: String? = null,
    val series: List<ClimateDataPointDto>? = null,
)

@Serializable
data class ClimateDataPointDto(
    val date: String? = null,
    val t2m: Double? = null,
    @SerialName("t2m_max")
    val t2mMax: Double? = null,
    @SerialName("t2m_min")
    val t2mMin: Double? = null,
    @SerialName("precipitation_mm")
    val precipitationMm: Double? = null,
    @SerialName("rh_pct")
    val rhPct: Double? = null,
    @SerialName("solar_mj_m2")
    val solarMjM2: Double? = null,
)
