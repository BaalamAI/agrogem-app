package com.agrogem.app.ui.screens.analysis

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
fun AnalysisScreen(
    onBackToCamera: () -> Unit,
    onViewReport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = remember { AnalysisViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = AgroGemPalette.spacing
    val pulse by rememberInfiniteTransition(label = "analysis-status").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "analysis-pulse",
    )

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
                LinearProgressIndicator(
                    progress = uiState.progress,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(uiState.progress * 100).toInt()}% completado",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = uiState.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulse),
                )
            }
        }

        items(uiState.steps, key = { it.id }) { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = AgroGemPalette.shapes.card,
                    )
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = if (step.done) "✓" else "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (step.done) {
                        AgroGemPalette.severity.optimo
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.onEvent(AnalysisEvent.OnFinishRequested)
                    onViewReport()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm),
            ) {
                Text("Ver reporte")
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    viewModel.onEvent(AnalysisEvent.OnCancelRequested)
                    onBackToCamera()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Volver a cámara")
            }
        }

        item {
            Text(
                text = "Simulación visual: el pipeline real de IA se integra en una etapa posterior.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.lg),
            )
        }
    }
}
