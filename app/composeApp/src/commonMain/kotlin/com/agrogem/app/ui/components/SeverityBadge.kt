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
import com.agrogem.app.theme.AgroGemColors

@Composable
fun SeverityBadge(
    severity: Severity,
    modifier: Modifier = Modifier,
    labelOverride: String? = null,
    compact: Boolean = false,
) {
    val (bgColor, textColor) = when (severity) {
        Severity.Optimo -> AgroGemColors.SeverityOptimoBg to AgroGemColors.SeverityOptimoText
        Severity.Atencion -> AgroGemColors.SeverityAtencionBg to AgroGemColors.SeverityAtencionText
        Severity.Critica -> AgroGemColors.DangerSoft to AgroGemColors.Danger
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
        color = textColor,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(
                color = bgColor,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

private val Severity.label: String
    get() = when (this) {
        Severity.Optimo -> "ÓPTIMO"
        Severity.Atencion -> "ATENCIÓN"
        Severity.Critica -> "CRÍTICO"
    }
