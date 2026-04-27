package com.agrogem.app.data.climate.domain

data class ClimateQuery(
    val start: String,
    val end: String,
    val granularity: String = "monthly",
)

expect fun createDefaultClimateQuery(): ClimateQuery
