package com.agrogem.app.ui.screens.figma.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.figma.BadgeTone
import com.agrogem.app.ui.screens.figma.FigmaColors

@Composable
internal fun StatusBadge(
    tone: BadgeTone,
    labelOverride: String? = null,
) {
    val (label, background, foreground) = when (tone) {
        BadgeTone.Healthy -> Triple(labelOverride ?: "ÓPTIMO", Color(0x330D631B), Color(0xFF438600))
        BadgeTone.Warning -> Triple(labelOverride ?: "ATENCIÓN", Color(0x4CFF7248), Color(0xFFDB0000))
        BadgeTone.Critical -> Triple(labelOverride ?: "CRÍTICO", FigmaColors.DangerSoft, FigmaColors.Danger)
    }

    Pill(
        text = label,
        background = background,
        foreground = foreground,
        horizontal = 10.dp,
        vertical = 4.dp,
        textSize = 10.sp,
    )
}
