package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agrogem.app.theme.AgroGemColors

@Composable
internal fun LeafThumb(
    seed: Int,
    rounded: Dp = 32.dp,
    size: Dp = 96.dp,
) {
    val brush = when (seed % 4) {
        0 -> Brush.linearGradient(AgroGemColors.LeafGradient0)
        1 -> Brush.linearGradient(AgroGemColors.LeafGradient1)
        2 -> Brush.linearGradient(AgroGemColors.LeafGradient2)
        else -> Brush.linearGradient(AgroGemColors.LeafGradient3)
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(brush, RoundedCornerShape(rounded)),
    )
}
