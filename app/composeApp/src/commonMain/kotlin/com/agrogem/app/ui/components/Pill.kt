package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun Pill(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconColor: Color = foreground,
    horizontal: Dp = 12.dp,
    vertical: Dp = 5.dp,
    textSize: TextUnit = 10.sp,
) {
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = horizontal, vertical = vertical),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Text(text = it, color = iconColor, fontSize = textSize)
        }
        Text(text = text, color = foreground, fontSize = textSize)
    }
}
