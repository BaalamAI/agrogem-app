package com.agrogem.app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agrogem.app.theme.AgroGemPalette

@Composable
fun MapRiskScreen(
    modifier: Modifier = Modifier,
    viewModel: MapRiskViewModel = remember { MapRiskViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = AgroGemPalette.spacing

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.padding(top = spacing.lg),
            ) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = uiState.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                uiState.riskSummary.forEach { summary ->
                    SummaryCard(
                        label = summary.label,
                        value = summary.value,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            StaticRiskMap(markers = uiState.markers)
        }

        item {
            Text(
                text = "Alertas activas",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        items(uiState.alerts, key = { it.id }) { alert ->
            AlertCard(alert = alert)
        }

        item {
            Text(
                text = uiState.disclaimer,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.lg),
            )
        }
    }
}

@Composable
private fun StaticRiskMap(
    markers: List<RiskMarker>,
    modifier: Modifier = Modifier,
) {
    val spacing = AgroGemPalette.spacing
    val mapHeight = 260.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                shape = AgroGemPalette.shapes.largeCard,
            )
            .border(
                width = Dp.Hairline,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = AgroGemPalette.shapes.largeCard,
            ),
    ) {
        val mapWidth = maxWidth

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dp.Hairline)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                )
            }
        }

        markers.forEach { marker ->
            val markerColor = marker.severity.toColor()
            MarkerPin(
                marker = marker,
                color = markerColor,
                modifier = Modifier
                    .offset(
                        x = mapWidth * marker.xFraction,
                        y = mapHeight * marker.yFraction,
                    )
                    .padding(start = spacing.xs),
            )
        }
    }
}

@Composable
private fun MarkerPin(
    marker: RiskMarker,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = AgroGemPalette.spacing

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color = color, shape = CircleShape)
                .border(width = Dp.Hairline, color = Color.White.copy(alpha = 0.7f), shape = CircleShape),
        )
        Text(
            text = "${marker.lot} · ${marker.riskLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = AgroGemPalette.shapes.pill,
                )
                .padding(horizontal = spacing.xs, vertical = 2.dp),
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val spacing = AgroGemPalette.spacing
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = AgroGemPalette.shapes.card,
            )
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlertCard(
    alert: RiskAlert,
    modifier: Modifier = Modifier,
) {
    val spacing = AgroGemPalette.spacing
    val color = alert.severity.toColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = AgroGemPalette.shapes.card,
            )
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = alert.lot,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = alert.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Sugerencia: ${alert.recommendation}",
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun RiskSeverity.toColor(): Color = when (this) {
    RiskSeverity.Optimo -> AgroGemPalette.severity.optimo
    RiskSeverity.Atencion -> AgroGemPalette.severity.atencion
    RiskSeverity.Critica -> AgroGemPalette.severity.critica
}
