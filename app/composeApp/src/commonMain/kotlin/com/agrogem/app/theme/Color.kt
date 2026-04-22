package com.agrogem.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Material3 color scheme ─────────────────────────────────────────────────

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

// ─── AgroGem semantic color tokens (from Figma) ─────────────────────────────

object AgroGemColors {
    // Surfaces
    val Screen = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF7F7F7)
    val SurfaceSoft = Color(0xFFEFF4EE)
    val Border = Color(0xFFE3E3E3)
    val BorderLight = Color(0xFFEDEDED)
    val BorderDivider = Color(0xFFF0F0F0)
    val DividerLine = Color(0xFFDDE2DF)
    val DividerThin = Color(0xFFF3F3F3)

    // Text
    val TextPrimary = Color(0xFF181D1A)
    val TextSecondary = Color(0xFF40493D)
    val TextHint = Color(0xFFB4BDB1)
    val TextMuted = Color(0xFF747474)
    val TextDark = Color(0xFF383838)
    val TextMedium = Color(0xFF4E4E4E)
    val TextBody = Color(0xFF242424)
    val TextLabel = Color(0xFF616161)
    val TextGray = Color(0xFF4C4C4C)
    val TextGrayMuted = Color(0xFF6C6C6C)
    val TextPlaceholder = Color(0xFFB6B6B6)
    val TextSearchIcon = Color(0xFFA5A5A5)
    val TextNavChevron = Color(0xFFB7C2B5)
    val TextChatTimestamp = Color(0xFFABABAB)
    val TextChatHint = Color(0xFF939393)
    val TextDateHeader = Color(0xFF707A6C)
    val TextLocation = Color(0xFF5B5B5B)
    val TextLocationSmall = Color(0xFF5A5A5A)
    val TextIconGray = Color(0xFF4D4D4D)
    val TextGraySecondary = Color(0xFF8A8A8A)

    // Brand / Primary
    val Primary = Color(0xFF0D631B)
    val PrimarySoft = Color(0xFFA3F69C)
    val PrimaryButton = Color(0xFF008026)
    val PrimaryButtonBorder = Color(0xFF008023)
    val PrimaryAction = Color(0xFF438A30)
    val PrimaryNavActive = Color(0xFF6C9E00)
    val PrimaryNavGlow = Color(0x66ABD557)
    val PrimaryNavGlowDim = Color(0x44ABD557)

    // Semantic states
    val Alert = Color(0xFF824600)
    val AlertSoft = Color(0xFFFFECDB)
    val Danger = Color(0xFFBA1A1A)
    val DangerSoft = Color(0xFFFFE6E4)
    val ConfidenceBg = Color(0xFFE5F6E9)
    val ConfidenceText = Color(0xFF0D4926)

    // Navigation
    val NavBackground = Color(0xF2FFFFFF)
    val NavInactive = Color(0x99747474)
    val ScanBackground = Color(0xFF0D631B)

    // Pill / Track
    val PillTrack = Color(0xFFE5E5E5)
    val PillTrackBorder = Color(0xFFB8B8B8)
    val PillTrackSemi = Color(0xB9E5E5E5)

    // Overlays
    val OverlayDark = Color(0xCC181D1A)
    val OverlayDim = Color(0x4D000000)

    // Camera / Analysis
    val CameraDarkTop = Color(0xFF2A4A5D)
    val CameraDarkBottom = Color(0xFF0D1310)
    val AnalysisBackdrop = Color(0xFF172013)
    val AnalysisStepDone = Color(0xFF2E7D32)
    val AnalysisStepPending = Color(0xFFDDE2DF)

    // Severity colors (used by SeverityBadge)
    val SeverityOptimo = Color(0xFF0D631B)
    val SeverityOptimoBg = Color(0x330D631B)
    val SeverityOptimoText = Color(0xFF438600)
    val SeverityAtencion = Color(0xFFFE5E2F)
    val SeverityAtencionBg = Color(0x4CFF7248)
    val SeverityAtencionText = Color(0xFFDB0000)
    val SeverityCritica = Color(0xFFB12D00)

    // Section headers
    val SectionHeader = Color(0xFF181D1A)
    val SectionHeaderAction = Color(0xFF0D631B)

    // Icon defaults
    val IconOnPrimary = Color.White
    val IconOnSurface = Color(0xFF141B34)
    val IconDefaultTint = Color(0xFF747474)
    val IconMenuTint = Color(0xFF929292)
    val IconBellTint = Color(0xFF3D7D20)

    // Drag handle
    val DragHandle = Color(0xFFE4E4E4)

    // Map-specific
    val MapBackground = Color(0xFFFFFFFF)
    val MapPrimary = Color(0xFF0D631B)
    val MapTextPrimary = Color(0xFF181D1A)
    val MapTextSecondary = Color(0xFF40493D)
    val MapMutedCard = Color(0xFFD9D9D9)
    val MapAlertColor = Color(0xFFB12D00)
    val MapLinePrimary = Color(0xFFC9CCCB)
    val MapLineSecondary = Color(0xFFD7D9D8)

    // LeafThumb gradients (seed-based)
    val LeafGradient0 = listOf(Color(0xFF9AC55A), Color(0xFF27491E))
    val LeafGradient1 = listOf(Color(0xFF2A5838), Color(0xFF16291A))
    val LeafGradient2 = listOf(Color(0xFF789C4C), Color(0xFF1F311B))
    val LeafGradient3 = listOf(Color(0xFF6D8E5A), Color(0xFF22372B))

    // PlantBackdrop gradients
    val BackdropGradient = listOf(
        Color(0xFF3D6278),
        Color(0xFF8AB0C3),
        Color(0xFF7E8F6A),
        Color(0xFF22301E),
    )
    val BackdropOverlay = Color(0xCC172013)

    // Diagnosis / Product
    val ProductCardGradient = listOf(Color(0xFF6D5030), Color(0xFF121212))
    val ProductAlertBorder = Color(0xFFFF8600)
    val CaptureOverlayBg = Color.White.copy(alpha = 0.2f)

    // Separator / metric divider
    val MetricDivider = Color(0x264D4D4D)
    val MetricTextAlpha = Color(0x994C4C4C)

    // Voice
    val VoiceOrbInner = Color(0x29438A30)
    val VoiceOrbOuter = Color(0x0D438A30)
    val VoiceSendBg = Color(0xBB202020)
    val VoiceDismissBg = Color(0xFFE5E5E5)
    val VoiceDismissText = Color(0xFF787878)
    val VoiceWaveBg = Color(0xFF438A30)

    // Chat
    val ChatAttachBg = Color(0xFFE5E5E5)
    val ChatAttachText = Color(0xFF7A7A7A)
    val ChatAttachHint = Color(0xFFBDBDBD)
    val ChatBorder = Color(0xFFDCDCDC)

    // Dots indicator
    val DotsIndicator = Color(0xE6FFFFFF) // White.copy(alpha = 0.9f)
}
