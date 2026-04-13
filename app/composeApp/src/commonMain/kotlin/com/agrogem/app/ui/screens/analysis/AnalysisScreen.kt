package com.agrogem.app.ui.screens.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AnalysisBackground = Color(0xFFFFFFFF)
private val AnalysisPrimary = Color(0xFF0D631B)
private val AnalysisTextPrimary = Color(0xFF181D1A)
private val AnalysisTextSecondary = Color(0xFF40493D)
private val AnalysisMuted = Color(0xFFD9DDDB)
private val AnalysisSoftCard = Color(0xFFF7F7F7)
private val AnalysisSuccess = Color(0xFF2E7D32)
private val AnalysisNeon = Color(0xFFA3F69C)

@Suppress("UNUSED_PARAMETER")
@Composable
fun AnalysisScreen(
    onBackToCamera: () -> Unit,
    onViewReport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = remember { AnalysisViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress = uiState.progress.coerceIn(0f, 1f)
    val progressLabel = "${(progress * 100).toInt()}%"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AnalysisBackground)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(
            top = 42.dp,
            bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        item {
            AnalysisHero()
        }

        item {
            AnalysisProgressBlock(progress = progress, progressLabel = progressLabel)
        }

        item {
            Text(
                text = "Cancelar Análisis",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onEvent(AnalysisEvent.OnCancelRequested)
                        onBackToCamera()
                    }
                    .background(
                        color = AnalysisSoftCard,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    )
                    .padding(vertical = 20.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                ),
                color = AnalysisPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AnalysisHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(388.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF050806), Color(0xFF0F1511)),
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(48.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6FA06A), Color.Transparent),
                            radius = 360f,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🌿",
                    fontSize = 188.sp,
                )
            }

            AnalysisCornerBrackets()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(AnalysisPrimary),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    color = Color(0xCC181D1A),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                )
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = AnalysisNeon, shape = androidx.compose.foundation.shape.CircleShape),
            )
            Text(
                text = "IA ACTIVA",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 1.2.sp,
                ),
                color = Color.White,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AnalysisCornerBrackets() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 3f
        val pad = 24f
        val length = 32f

        drawLine(AnalysisNeon, Offset(pad, pad), Offset(pad + length, pad), stroke, cap = StrokeCap.Round)
        drawLine(AnalysisNeon, Offset(pad, pad), Offset(pad, pad + length), stroke, cap = StrokeCap.Round)

        drawLine(AnalysisNeon, Offset(size.width - pad, pad), Offset(size.width - pad - length, pad), stroke, cap = StrokeCap.Round)
        drawLine(AnalysisNeon, Offset(size.width - pad, pad), Offset(size.width - pad, pad + length), stroke, cap = StrokeCap.Round)

        drawLine(AnalysisNeon, Offset(pad, size.height - pad), Offset(pad + length, size.height - pad), stroke, cap = StrokeCap.Round)
        drawLine(AnalysisNeon, Offset(pad, size.height - pad), Offset(pad, size.height - pad - length), stroke, cap = StrokeCap.Round)

        drawLine(AnalysisNeon, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - length, size.height - pad), stroke, cap = StrokeCap.Round)
        drawLine(AnalysisNeon, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - length), stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun AnalysisProgressBlock(
    progress: Float,
    progressLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Analizando cultivo...",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.6).sp,
                    ),
                    color = AnalysisPrimary,
                )
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                    ),
                    color = AnalysisPrimary,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        color = Color(0xFFDDE2DF),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    ),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(12.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AnalysisPrimary, AnalysisSuccess),
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AnalysisSoftCard,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(48.dp),
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnalysisStatusRow(
                title = "Identificando patrones de hojas...",
                subtitle = "Analizando irregularidades celulares",
                iconKind = AnalysisIconKind.Detect,
                foreground = AnalysisPrimary,
                bubbleColor = AnalysisSuccess,
                alpha = 1f,
            )
            AnalysisStatusRow(
                title = "Consultando base de datos de\nplagas...",
                subtitle = "Sincronizando con AgroCloud Index",
                iconKind = AnalysisIconKind.Database,
                foreground = AnalysisTextPrimary,
                bubbleColor = AnalysisMuted,
                alpha = 0.6f,
            )
            AnalysisStatusRow(
                title = "Calculando severidad...",
                subtitle = "Estimación de impacto en cosecha",
                iconKind = AnalysisIconKind.Calculate,
                foreground = AnalysisTextPrimary,
                bubbleColor = AnalysisMuted,
                alpha = 0.4f,
            )
        }
    }
}

private enum class AnalysisIconKind {
    Detect,
    Database,
    Calculate,
}

@Composable
private fun AnalysisStatusRow(
    title: String,
    subtitle: String,
    iconKind: AnalysisIconKind,
    foreground: Color,
    bubbleColor: Color,
    alpha: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(bubbleColor, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnalysisStatusIcon(kind = iconKind, tint = Color.White.copy(alpha = alpha + 0.1f))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.35).sp,
                ),
                color = foreground.copy(alpha = alpha),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                color = AnalysisTextSecondary.copy(alpha = alpha),
            )
        }
    }
}

@Composable
private fun AnalysisStatusIcon(
    kind: AnalysisIconKind,
    tint: Color,
) {
    Canvas(modifier = Modifier.size(16.dp)) {
        when (kind) {
            AnalysisIconKind.Detect -> {
                drawLine(tint, Offset(size.width * 0.18f, size.height * 0.78f), Offset(size.width * 0.46f, size.height * 0.5f), 1.8f, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.46f, size.height * 0.5f), Offset(size.width * 0.82f, size.height * 0.2f), 1.8f, StrokeCap.Round)
                drawCircle(tint, radius = 1.5f, center = Offset(size.width * 0.18f, size.height * 0.78f))
                drawCircle(tint, radius = 1.5f, center = Offset(size.width * 0.46f, size.height * 0.5f))
                drawCircle(tint, radius = 1.5f, center = Offset(size.width * 0.82f, size.height * 0.2f))
            }

            AnalysisIconKind.Database -> {
                drawOval(tint, topLeft = Offset(size.width * 0.2f, size.height * 0.14f), size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.2f), style = Stroke(1.8f))
                drawLine(tint, Offset(size.width * 0.2f, size.height * 0.24f), Offset(size.width * 0.2f, size.height * 0.78f), 1.8f)
                drawLine(tint, Offset(size.width * 0.8f, size.height * 0.24f), Offset(size.width * 0.8f, size.height * 0.78f), 1.8f)
                drawOval(tint, topLeft = Offset(size.width * 0.2f, size.height * 0.68f), size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.2f), style = Stroke(1.8f))
                drawLine(tint, Offset(size.width * 0.2f, size.height * 0.5f), Offset(size.width * 0.8f, size.height * 0.5f), 1.4f)
            }

            AnalysisIconKind.Calculate -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.16f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                    style = Stroke(1.8f),
                )
                drawLine(tint, Offset(size.width * 0.28f, size.height * 0.36f), Offset(size.width * 0.72f, size.height * 0.36f), 1.6f)
                drawLine(tint, Offset(size.width * 0.35f, size.height * 0.54f), Offset(size.width * 0.47f, size.height * 0.54f), 1.6f)
                drawLine(tint, Offset(size.width * 0.53f, size.height * 0.54f), Offset(size.width * 0.65f, size.height * 0.54f), 1.6f)
            }
        }
    }
}
