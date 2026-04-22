package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
            .height(51.dp)
            .background(AgroGemColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, AgroGemColors.BorderLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, color = AgroGemColors.TextMuted, fontSize = 8.sp)
        if (italicTail) {
            Text(
                text = value,
                color = AgroGemColors.TextPrimary,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Text(text = value, color = AgroGemColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
