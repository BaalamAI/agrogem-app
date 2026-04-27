package com.agrogem.app.data.climate.domain

import androidx.compose.runtime.Immutable

@Immutable
data class ClimateHistory(
    val lat: Double,
    val lon: Double,
    val granularity: String,
    val domainSeries: List<ClimateDataPoint>,
)

@Immutable
data class ClimateDataPoint(
    val date: String,
    val t2m: Double,
    val t2mMax: Double,
    val t2mMin: Double,
    val precipitationMm: Double,
    val rhPct: Double,
    val solarMjM2: Double,
)
