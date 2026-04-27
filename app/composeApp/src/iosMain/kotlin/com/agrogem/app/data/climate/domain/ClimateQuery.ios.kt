package com.agrogem.app.data.climate.domain

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents

actual fun createDefaultClimateQuery(): ClimateQuery {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"

    val now = NSDate()
    val end = formatter.stringFromDate(now)

    val calendar = NSCalendar.currentCalendar
    val components = NSDateComponents()
    components.month = -12
    val startDate = calendar.dateByAddingComponents(components, now, 0u) ?: now
    val start = formatter.stringFromDate(startDate)

    return ClimateQuery(start = start, end = end, granularity = "monthly")
}
