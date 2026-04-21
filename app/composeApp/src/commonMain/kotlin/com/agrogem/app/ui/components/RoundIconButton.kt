package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.theme.AgroGemColors
import org.jetbrains.compose.resources.DrawableResource

/**
 * Resolves the content description for an icon button.
 * Returns [explicit] when provided, otherwise falls back to [label].
 */
fun resolveIconContentDescription(label: String, explicit: String?): String =
    explicit ?: label

@Composable
internal fun RoundIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.4f),
    foreground: Color = AgroGemColors.IconDefaultTint,
    size: Dp = 32.dp,
    icon: DrawableResource? = null,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            AgroGemIcon(
                icon = icon,
                contentDescription = resolveIconContentDescription(label, contentDescription),
                tint = foreground,
                size = size * 0.5f,
            )
        } else {
            Text(text = label, color = foreground, fontSize = (size.value * 0.42f).sp)
        }
    }
}
