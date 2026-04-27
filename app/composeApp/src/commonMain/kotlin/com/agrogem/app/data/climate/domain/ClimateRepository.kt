package com.agrogem.app.data.climate.domain

import com.agrogem.app.data.climate.api.ClimateApi
import com.agrogem.app.data.climate.api.ClimateDataPointDto
import com.agrogem.app.data.climate.api.ClimateHistoryResponse
import com.agrogem.app.data.shared.domain.LatLng

interface ClimateRepository {
    suspend fun getClimateHistory(
        latLng: LatLng,
        start: String,
        end: String,
        granularity: String = "monthly",
    ): Result<ClimateHistory>

    suspend fun getClimateHistory(latLng: LatLng): Result<ClimateHistory> {
        val query = createDefaultClimateQuery()
        return getClimateHistory(latLng, query.start, query.end, query.granularity)
    }
}

class ClimateRepositoryImpl(
    private val api: ClimateApi,
) : ClimateRepository {

    override suspend fun getClimateHistory(
        latLng: LatLng,
        start: String,
        end: String,
        granularity: String,
    ): Result<ClimateHistory> {
        return try {
            val effectiveGranularity = if (granularity == "daily" && exceedsDailyLimit(start, end)) {
                "monthly"
            } else {
                granularity
            }
            val history = api.getClimateHistory(
                lat = latLng.latitude,
                lon = latLng.longitude,
                start = start,
                end = end,
                granularity = effectiveGranularity,
            )
            Result.success(mapClimateHistoryResponse(history))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

internal fun exceedsDailyLimit(start: String, end: String): Boolean {
    // Simplified: assume YYYY-MM-DD format; coerce if parsing fails or range > 366 days
    return try {
        val startDays = parseDateToDays(start)
        val endDays = parseDateToDays(end)
        (endDays - startDays) > 366
    } catch (_: Exception) {
        false
    }
}

private fun parseDateToDays(date: String): Int {
    val parts = date.split("-").map { it.toInt() }
    val year = parts[0]
    val month = parts[1]
    val day = parts[2]
    var days = 0
    for (y in 1 until year) {
        days += if (isLeap(y)) 366 else 365
    }
    val monthDays = intArrayOf(31, if (isLeap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    for (m in 1 until month) {
        days += monthDays[m - 1]
    }
    days += day
    return days
}

private fun isLeap(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

internal fun mapClimateHistoryResponse(dto: ClimateHistoryResponse): ClimateHistory {
    val domainSeries = dto.series?.map { mapClimateDataPoint(it) } ?: emptyList()
    return ClimateHistory(
        lat = dto.lat ?: 0.0,
        lon = dto.lon ?: 0.0,
        granularity = dto.granularity ?: "",
        domainSeries = domainSeries,
    )
}

internal fun mapClimateDataPoint(dto: ClimateDataPointDto): ClimateDataPoint {
    return ClimateDataPoint(
        date = dto.date ?: "",
        t2m = dto.t2m ?: 0.0,
        t2mMax = dto.t2mMax ?: 0.0,
        t2mMin = dto.t2mMin ?: 0.0,
        precipitationMm = dto.precipitationMm ?: 0.0,
        rhPct = dto.rhPct ?: 0.0,
        solarMjM2 = dto.solarMjM2 ?: 0.0,
    )
}
