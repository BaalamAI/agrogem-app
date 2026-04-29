package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.theme.AgroGemColors

/**
 * Shared info box used across diagnosis and chat screens.
 * Displays a label-value pair in a compact bordered card.
 */
@Composable
fun DiagnosisInfoBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    italicTail: Boolean = false,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 58.dp)
            .background(AgroGemColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, AgroGemColors.BorderLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, color = AgroGemColors.TextMuted, fontSize = 10.sp)
        if (italicTail) {
            Text(
                text = value,
                color = AgroGemColors.TextPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = value,
                color = AgroGemColors.TextPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
