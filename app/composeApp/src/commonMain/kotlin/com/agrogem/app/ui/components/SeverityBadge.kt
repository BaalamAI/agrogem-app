package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SeverityOptimo = Color(0xFF0D631B)
private val SeverityAtencion = Color(0xFFFE5E2F)
private val SeverityCritica = Color(0xFFB12D00)

@Composable
fun SeverityBadge(
    severity: Severity,
    modifier: Modifier = Modifier,
    labelOverride: String? = null,
    compact: Boolean = false,
) {
    val color = when (severity) {
        Severity.Optimo -> SeverityOptimo
        Severity.Atencion -> SeverityAtencion
        Severity.Critica -> SeverityCritica
    }
    val label = labelOverride ?: severity.label
    val textStyle = if (compact) {
        MaterialTheme.typography.labelMedium.copy(
            fontSize = 10.sp,
            lineHeight = 15.sp,
        )
    } else {
        MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.6).sp,
        )
    }

    Text(
        text = label,
        style = textStyle,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

private val Severity.label: String
    get() = when (this) {
        Severity.Optimo -> "ÓPTIMO"
        Severity.Atencion -> "ATENCIÓN"
        Severity.Critica -> "CRÍTICA"
    }
