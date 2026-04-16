package com.agrogem.app.ui.screens.figma

internal enum class BadgeTone {
    Healthy,
    Warning,
    Critical,
}

internal data class RecentAnalysisItem(
    val name: String,
    val subtitle: String,
    val health: String,
    val tone: BadgeTone,
)

internal data class HistoryEntry(
    val crop: String,
    val meta: String,
    val status: String,
    val tone: BadgeTone,
    val seed: Int,
)

internal data class ProductItem(
    val name: String,
    val price: String,
)

internal val dashboardRecentItems = listOf(
    RecentAnalysisItem(
        name = "ALBAHACA",
        subtitle = "Sin plagas detectadas",
        health = "Salud: 98%",
        tone = BadgeTone.Healthy,
    ),
    RecentAnalysisItem(
        name = "TOMATE",
        subtitle = "Estrés hídrico leve",
        health = "Salud: 72%",
        tone = BadgeTone.Warning,
    ),
)

internal val historyToday = listOf(
    HistoryEntry("Tomate Roma", "10:45 AM • Invernadero A", "SALUDABLE", BadgeTone.Healthy, 1),
    HistoryEntry("Maíz Dulce", "08:20 AM • Parcela Norte", "ALERTA", BadgeTone.Warning, 2),
)

internal val historyYesterday = listOf(
    HistoryEntry("Papa Blanca", "04:15 PM • Sección C-4", "CRÍTICO", BadgeTone.Critical, 3),
    HistoryEntry("Papa Blanca", "04:15 PM • Sección C-4", "CRÍTICO", BadgeTone.Critical, 4),
)

internal val products = listOf(
    ProductItem("Caldo Bordelés XL", "$24.50"),
    ProductItem("Caldo Bordelés XL", "$24.50"),
)
