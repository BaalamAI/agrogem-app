package com.agrogem.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.agrogem.app.theme.AgroGemIconSizes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Immutable
object AgroGemIconColors {
    val OnPrimary: Color = Color.White
    val OnSurface: Color = Color(0xFF141B34)
}

@Composable
fun AgroGemIcon(
    icon: DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp? = AgroGemIconSizes.Sm,
    tint: Color = Color.Unspecified,
) {
    val sizedModifier = if (size != null) modifier.size(size) else modifier

    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = tint,
        modifier = sizedModifier,
    )
}
