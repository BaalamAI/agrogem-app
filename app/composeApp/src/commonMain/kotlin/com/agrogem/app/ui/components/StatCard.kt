package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.agrogem.app.theme.AgroGemPalette
import com.agrogem.app.ui.screens.dashboard.DashboardStat

@Composable
fun StatCard(
    stat: DashboardStat,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = AgroGemPalette.shapes.largeCard,
            )
            .padding(AgroGemPalette.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AgroGemPalette.spacing.sm),
    ) {
        SeverityBadge(severity = stat.severity)
        Text(
            text = stat.value,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stat.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
