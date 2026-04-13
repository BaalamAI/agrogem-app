package com.agrogem.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.agrogem.app.theme.AgroGemPalette
import com.agrogem.app.ui.components.AnalysisCard
import com.agrogem.app.ui.components.SectionHeader
import com.agrogem.app.ui.components.StatCard

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = remember { DashboardViewModel() },
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
                    text = uiState.greeting,
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
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                uiState.stats.forEach { stat ->
                    StatCard(
                        stat = stat,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Análisis recientes",
                actionLabel = "Ver todo",
                onActionClick = { viewModel.onEvent(DashboardEvent.OnSeeAllRequested) },
                modifier = Modifier.padding(top = spacing.sm),
            )
        }

        items(uiState.recentAnalyses, key = { it.id }) { analysis ->
            AnalysisCard(
                analysis = analysis,
                onClick = {
                    viewModel.onEvent(DashboardEvent.OnRecentAnalysisSelected(it.id))
                },
            )
        }

        item {
            Column(modifier = Modifier.padding(bottom = spacing.lg)) {
                Text(
                    text = "Panel en modo maquetado: datos simulados.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (uiState.isHistoryVisible) {
        HistorialModal(
            analyses = uiState.historyAnalyses,
            onDismiss = { viewModel.onEvent(DashboardEvent.OnHistoryDismissRequested) },
            onSelect = { analysisId ->
                viewModel.onEvent(DashboardEvent.OnHistoryAnalysisSelected(analysisId))
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HistorialModal(
    analyses: List<RecentAnalysis>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val spacing = AgroGemPalette.spacing

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = "Historial de análisis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Datos simulados para maquetado. Seleccioná un registro para cerrarlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            analyses.forEach { analysis ->
                AnalysisCard(
                    analysis = analysis,
                    onClick = { onSelect(it.id) },
                )
            }
            Spacer(modifier = Modifier.height(spacing.lg))
        }
    }
}
