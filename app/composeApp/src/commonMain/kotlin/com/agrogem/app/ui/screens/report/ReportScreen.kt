package com.agrogem.app.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.agrogem.app.theme.AgroGemPalette

@Composable
fun ReportScreen(
    onScanAgain: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = remember { ReportViewModel() },
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
                    text = "${uiState.crop} • ${uiState.lot}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        shape = AgroGemPalette.shapes.largeCard,
                    )
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = "Salud del cultivo",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${uiState.healthScore}%",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                ReportStatusPill(label = uiState.statusLabel)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = AgroGemPalette.shapes.card,
                    )
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = "Diagnóstico",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.diagnosis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                text = "Recomendaciones",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        items(uiState.recommendations) { recommendation ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = AgroGemPalette.shapes.card,
                    )
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
            ) {
                Text(
                    text = "• $recommendation",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Button(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm),
            ) {
                Text("Nuevo escaneo")
            }
        }

        item {
            OutlinedButton(
                onClick = onBackToDashboard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Volver al dashboard")
            }
        }

        item {
            Text(
                text = "Reporte de maquetado con datos simulados. Sin conexión a APIs reales.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.lg),
            )
        }
    }
}

@Composable
private fun ReportStatusPill(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = AgroGemPalette.severity.atencion,
        modifier = Modifier
            .background(
                color = AgroGemPalette.severity.atencion.copy(alpha = 0.22f),
                shape = AgroGemPalette.shapes.pill,
            )
            .padding(horizontal = AgroGemPalette.spacing.sm, vertical = AgroGemPalette.spacing.xs),
    )
}
