package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrogem.app.theme.AgroGemPalette
import com.agrogem.app.ui.screens.dashboard.DashboardSeverity

@Composable
fun SeverityBadge(
    severity: DashboardSeverity,
    modifier: Modifier = Modifier,
) {
    val color = when (severity) {
        DashboardSeverity.Optimo -> AgroGemPalette.severity.optimo
        DashboardSeverity.Atencion -> AgroGemPalette.severity.atencion
        DashboardSeverity.Critica -> AgroGemPalette.severity.critica
    }

    Text(
        text = severity.label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private val DashboardSeverity.label: String
    get() = when (this) {
        DashboardSeverity.Optimo -> "ÓPTIMO"
        DashboardSeverity.Atencion -> "ATENCIÓN"
        DashboardSeverity.Critica -> "CRÍTICA"
    }
