package com.agrogem.app.ui.screens.figma.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun LeafThumb(
    seed: Int,
    rounded: Dp = 32.dp,
    size: Dp = 96.dp,
) {
    val brush = when (seed % 4) {
        0 -> Brush.linearGradient(listOf(Color(0xFF9AC55A), Color(0xFF27491E)))
        1 -> Brush.linearGradient(listOf(Color(0xFF2A5838), Color(0xFF16291A)))
        2 -> Brush.linearGradient(listOf(Color(0xFF789C4C), Color(0xFF1F311B)))
        else -> Brush.linearGradient(listOf(Color(0xFF6D8E5A), Color(0xFF22372B)))
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(brush, RoundedCornerShape(rounded)),
    )
}
