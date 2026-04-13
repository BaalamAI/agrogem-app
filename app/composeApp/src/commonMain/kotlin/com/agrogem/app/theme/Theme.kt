package com.agrogem.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun AgroGemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) agroGemDarkColorScheme() else agroGemLightColorScheme()

    CompositionLocalProvider(
        LocalAgroGemSeverityPalette provides AgroGemSeverityPalette(
            optimo = colorScheme.primary,
            atencion = if (darkTheme) Color(0xFFFFB300) else Color(0xFF9A6700),
            critica = if (darkTheme) Color(0xFFEF5350) else Color(0xFFB3261E),
        ),
        LocalAgroGemSpacing provides AgroGemSpacing(),
        LocalAgroGemShapeTokens provides DefaultAgroGemShapeTokens,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AgroGemTypography,
            shapes = AgroGemShapes,
            content = content,
        )
    }
}
