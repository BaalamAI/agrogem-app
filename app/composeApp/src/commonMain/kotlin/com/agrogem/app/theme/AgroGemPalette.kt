package com.agrogem.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AgroGemSeverityPalette(
    val optimo: Color,
    val atencion: Color,
    val critica: Color,
)

val LocalAgroGemSeverityPalette = compositionLocalOf {
    AgroGemSeverityPalette(
        optimo = Color(0xFF4CAF50),
        atencion = Color(0xFFFFB300),
        critica = Color(0xFFEF5350),
    )
}

val LocalAgroGemShapeTokens = staticCompositionLocalOf { DefaultAgroGemShapeTokens }

object AgroGemPalette {
    val severity: AgroGemSeverityPalette
        @Composable
        get() = LocalAgroGemSeverityPalette.current

    val shapes: AgroGemShapeTokens
        @Composable
        get() = LocalAgroGemShapeTokens.current

    val spacing: AgroGemSpacing
        @Composable
        get() = LocalAgroGemSpacing.current
}
