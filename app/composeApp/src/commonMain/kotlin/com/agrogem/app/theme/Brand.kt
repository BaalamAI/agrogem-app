package com.agrogem.app.theme

import androidx.compose.ui.graphics.Color

// Paleta oficial de marca AgroGem.
// Fuente de verdad para nuevas pantallas / componentes rediseñados.
// Convive con AgroGemColors (legacy) durante la migración.
object AgroGemBrand {

    // ─── Colores principales ────────────────────────────────────────────────
    val Background = Color(0xFFF7F7F7)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF060E0F)
    val VerdeAgrogem = Color(0xFF59C27D)

    // ─── Escala de verde ────────────────────────────────────────────────────
    val Verde50 = Color(0xFFEDF8F2)
    val Verde200 = Color(0xFFB9E8CB)
    val Verde400 = Color(0xFF59C27D) // == VerdeAgrogem
    val Verde600 = Color(0xFF3A9B5E)
    val Verde900 = Color(0xFF1F5C37)

    // ─── Escala de grises ───────────────────────────────────────────────────
    val Gris50 = Color(0xFFF7F7F7) // == Background
    val Gris200 = Color(0xFFE0E0E0)
    val Gris400 = Color(0xFFA3A3A3)
    val Gris700 = Color(0xFF4D4D4D)
    val Gris900 = Color(0xFF060E0F) // == Black

    // ─── Roles semánticos (combinaciones recomendadas) ──────────────────────
    object Button {
        // Primario: bg Verde400 / texto verde oscuro
        val PrimaryBg = Verde400
        val PrimaryText = Color(0xFF0D3D20)
        // Secundario: bg negro / texto blanco
        val SecondaryBg = Black
        val SecondaryText = White
    }

    object Badge {
        val Bg = Verde50
        val Text = Verde900
    }

    object Text {
        val Primary = Gris900
        val Secondary = Gris700
        val Disabled = Gris400
        val OnBrand = Verde900
    }
}
