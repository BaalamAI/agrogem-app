package com.agrogem.app.data.climate.domain

import java.util.Calendar

actual fun createDefaultClimateQuery(): ClimateQuery {
    val endCal = Calendar.getInstance()
    val endYear = endCal.get(Calendar.YEAR)
    val endMonth = endCal.get(Calendar.MONTH) + 1
    val endDay = endCal.get(Calendar.DAY_OF_MONTH)
    val end = "${endYear}-${endMonth.toString().padStart(2, '0')}-${endDay.toString().padStart(2, '0')}"

    val startCal = Calendar.getInstance().apply { add(Calendar.MONTH, -12) }
    val startYear = startCal.get(Calendar.YEAR)
    val startMonth = startCal.get(Calendar.MONTH) + 1
    val startDay = startCal.get(Calendar.DAY_OF_MONTH)
    val start = "${startYear}-${startMonth.toString().padStart(2, '0')}-${startDay.toString().padStart(2, '0')}"

    return ClimateQuery(start = start, end = end, granularity = "monthly")
}
