package com.agrogem.app.data.climate.domain

private fun todayJs(): String = js("new Date().toISOString().slice(0,10)")

@Suppress("UNUSED_PARAMETER")
private fun minusMonthsJs(dateIso: String, months: Int): String =
    js("(function(d,m){var x=new Date(d+'T00:00:00Z');x.setUTCMonth(x.getUTCMonth()-m);return x.toISOString().slice(0,10);})(dateIso,months)")

actual fun createDefaultClimateQuery(): ClimateQuery {
    val today = todayJs()
    val start = minusMonthsJs(today, 12)
    return ClimateQuery(start = start, end = today, granularity = "monthly")
}
