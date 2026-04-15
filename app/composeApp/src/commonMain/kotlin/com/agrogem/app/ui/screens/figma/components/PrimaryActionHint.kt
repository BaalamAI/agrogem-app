package com.agrogem.app.ui.screens.figma.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.figma.FigmaColors

@Composable
internal fun PrimaryActionHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(FigmaColors.OverlayDark, RoundedCornerShape(999.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(FigmaColors.PrimarySoft, CircleShape),
        )
        Text(text = text, color = Color.White, fontSize = 12.sp, letterSpacing = 1.2.sp)
    }
}
