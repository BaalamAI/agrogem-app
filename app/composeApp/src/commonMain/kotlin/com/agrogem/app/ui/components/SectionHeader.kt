package com.agrogem.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SectionHeaderTextColor = Color(0xFF181D1A)
private val SectionHeaderActionColor = Color(0xFF0D631B)

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = SectionHeaderTextColor,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            color = SectionHeaderActionColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onActionClick),
        )
    }
}
