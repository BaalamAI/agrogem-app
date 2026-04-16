package com.agrogem.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrogem.app.ui.components.AnalysisCard
import com.agrogem.app.ui.components.SectionHeader
import com.agrogem.app.ui.components.StatCard
import com.agrogem.app.ui.viewmodel.kmpViewModel

private val DashboardBackground = Color(0xFFFFFFFF)
private val DashboardPrimary = Color(0xFF0D631B)
private val DashboardTextPrimary = Color(0xFF181D1A)
private val DashboardTextSecondary = Color(0xFF40493D)

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = kmpViewModel { DashboardViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardBackground),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(
                top = 96.dp,
                bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            item {
                DashboardHero(
                    greeting = uiState.greeting,
                    subtitle = uiState.subtitle,
                )
            }

            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                    title = "Análisis Recientes",
                    actionLabel = "Ver todo",
                    onActionClick = { viewModel.onEvent(DashboardEvent.OnSeeAllRequested) },
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
        }

        DashboardBrandHeader(modifier = Modifier.align(Alignment.TopCenter))
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DashboardBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Historial de análisis",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = DashboardTextPrimary,
            )
            Text(
                text = "Seleccioná un registro para volver al dashboard.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                color = DashboardTextSecondary,
            )
            analyses.forEach { analysis ->
                AnalysisCard(
                    analysis = analysis,
                    showCapturedAt = true,
                    onClick = { onSelect(it.id) },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardHero(
    greeting: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 1.6.sp,
            ),
            color = DashboardPrimary,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = subtitle.highlightHealthyWord(),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                lineHeight = 45.sp,
                letterSpacing = (-0.9).sp,
            ),
            color = DashboardTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DashboardBrandHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DashboardBackground.copy(alpha = 0.86f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeafMarkIcon()
            Text(
                text = "Agrogemma",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-1.2).sp,
                ),
                color = DashboardPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun LeafMarkIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val stroke = size.minDimension * 0.13f

        drawOval(
            color = DashboardPrimary,
            topLeft = Offset(x = size.width * 0.08f, y = size.height * 0.2f),
            size = Size(width = size.width * 0.78f, height = size.height * 0.58f),
            style = Stroke(width = stroke),
        )

        drawLine(
            color = DashboardPrimary,
            start = Offset(x = size.width * 0.72f, y = size.height * 0.2f),
            end = Offset(x = size.width * 0.28f, y = size.height * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun String.highlightHealthyWord(): AnnotatedString {
    val target = "saludable"
    val startIndex = lowercase().indexOf(target)

    if (startIndex < 0) {
        return AnnotatedString(this)
    }

    val endIndex = startIndex + target.length

    return buildAnnotatedString {
        append(substring(0, startIndex))
        withStyle(style = SpanStyle(color = DashboardPrimary)) {
            append(substring(startIndex, endIndex))
        }
        append(substring(endIndex))
    }
}
