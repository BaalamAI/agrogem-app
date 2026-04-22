package com.agrogem.app.ui.preview

import com.agrogem.app.ui.components.Severity

internal data class RecentAnalysisItem(
    val name: String,
    val subtitle: String,
    val health: String,
    val severity: Severity,
)

internal data class HistoryEntry(
    val crop: String,
    val meta: String,
    val status: String,
    val severity: Severity,
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
        severity = Severity.Optimo,
    ),
    RecentAnalysisItem(
        name = "TOMATE",
        subtitle = "Estrés hídrico leve",
        health = "Salud: 72%",
        severity = Severity.Atencion,
    ),
)

internal val historyToday = listOf(
    HistoryEntry("Tomate Roma", "10:45 AM • Invernadero A", "SALUDABLE", Severity.Optimo, 1),
    HistoryEntry("Maíz Dulce", "08:20 AM • Parcela Norte", "ALERTA", Severity.Atencion, 2),
)

internal val historyYesterday = listOf(
    HistoryEntry("Papa Blanca", "04:15 PM • Sección C-4", "CRÍTICO", Severity.Critica, 3),
    HistoryEntry("Papa Blanca", "04:15 PM • Sección C-4", "CRÍTICO", Severity.Critica, 4),
)

internal val products = listOf(
    ProductItem("Caldo Bordelés XL", "$24.50"),
    ProductItem("Caldo Bordelés XL", "$24.50"),
)
