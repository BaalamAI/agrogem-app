package com.agrogem.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val DarkPrimary = Color(0xFF3DDC84)
private val DarkOnPrimary = Color(0xFF00391D)
private val DarkPrimaryContainer = Color(0xFF00522D)
private val DarkOnPrimaryContainer = Color(0xFF8DFFB0)
private val DarkSecondary = Color(0xFFB7CCBC)
private val DarkOnSecondary = Color(0xFF233427)
private val DarkBackground = Color(0xFF101714)
private val DarkOnBackground = Color(0xFFE0E4DE)
private val DarkSurface = Color(0xFF141C19)
private val DarkOnSurface = Color(0xFFE0E4DE)
private val DarkSurfaceVariant = Color(0xFF3F4943)
private val DarkOnSurfaceVariant = Color(0xFFBEC9C0)
private val DarkOutline = Color(0xFF89938B)

private val LightPrimary = Color(0xFF006D3B)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFF8DFFB0)
private val LightOnPrimaryContainer = Color(0xFF00210F)
private val LightSecondary = Color(0xFF506352)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightBackground = Color(0xFFF7FBF4)
private val LightOnBackground = Color(0xFF181D19)
private val LightSurface = Color(0xFFF7FBF4)
private val LightOnSurface = Color(0xFF181D19)
private val LightSurfaceVariant = Color(0xFFDBE5DC)
private val LightOnSurfaceVariant = Color(0xFF3F4943)
private val LightOutline = Color(0xFF6F7971)

fun agroGemDarkColorScheme() = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
)

fun agroGemLightColorScheme() = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
)
