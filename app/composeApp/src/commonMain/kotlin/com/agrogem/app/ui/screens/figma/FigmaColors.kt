package com.agrogem.app.ui.screens.figma

import androidx.compose.ui.graphics.Color

internal object FigmaColors {
    val Screen = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF7F7F7)
    val SurfaceSoft = Color(0xFFEFF4EE)
    val Border = Color(0xFFE3E3E3)
    val Text = Color(0xFF181D1A)
    val TextSecondary = Color(0xFF40493D)
    val TextHint = Color(0xFFB4BDB1)
    val Primary = Color(0xFF0D631B)
    val PrimarySoft = Color(0xFFA3F69C)
    val Alert = Color(0xFF824600)
    val AlertSoft = Color(0xFFFFECDB)
    val Danger = Color(0xFFBA1A1A)
    val DangerSoft = Color(0xFFFFE6E4)
    val ConfidenceBg = Color(0xFFE5F6E9)
    val ConfidenceText = Color(0xFF0D4926)
    val PillTrack = Color(0xFFE5E5E5)
    val OverlayDark = Color(0xCC181D1A)
    val CameraDarkTop = Color(0xFF2A4A5D)
    val CameraDarkBottom = Color(0xFF0D1310)

    /**
     * Deferred token migration: This object is intentionally retained in Phase 1-2.
     * Phase 3 (token alignment) will migrate these tokens to AgroGemPalette/Color.kt.
     * See: SDD spec `sdd/refactor-figma-flow-screens/spec` — Theme and Token Alignment requirement.
     */
}
