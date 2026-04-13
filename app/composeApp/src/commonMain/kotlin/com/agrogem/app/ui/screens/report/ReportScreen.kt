package com.agrogem.app.ui.screens.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ReportBackground = Color(0xFFFFFFFF)
private val ReportPrimary = Color(0xFF0D631B)
private val ReportMutedText = Color(0xFF72777A)
private val ReportDanger = Color(0xFFB12D00)
private val ReportDarkButton = Color(0xBA202020)
private val ReportInputBorder = Color(0xFF979C9E)
private val ReportSoftCard = Color(0xFFF7F7F7)

@Composable
fun ReportScreen(
    onScanAgain: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = remember { ReportViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReportBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ReportTopActions(onBackToDashboard = onBackToDashboard)
            ReportHeroCard()
            ReportDiagnosisCard(diagnosis = uiState.diagnosis)
            ReportMediaPanel(onScanAgain = onScanAgain)
        }

        ReportMessageInput(onScanAgain = onScanAgain)
    }
}

@Composable
private fun ReportTopActions(onBackToDashboard: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ReportCircleActionButton(icon = ReportActionIcon.Back, onClick = onBackToDashboard)
        ReportCircleActionButton(icon = ReportActionIcon.Menu, onClick = {})
    }
}

private enum class ReportActionIcon {
    Back,
    Menu,
}

@Composable
private fun ReportCircleActionButton(
    icon: ReportActionIcon,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .shadow(1.dp, CircleShape)
            .background(
                color = Color.White.copy(alpha = 0.4f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            when (icon) {
                ReportActionIcon.Back -> {
                    drawLine(
                        color = ReportMutedText,
                        start = Offset(size.width * 0.68f, size.height * 0.2f),
                        end = Offset(size.width * 0.32f, size.height * 0.5f),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = ReportMutedText,
                        start = Offset(size.width * 0.32f, size.height * 0.5f),
                        end = Offset(size.width * 0.68f, size.height * 0.8f),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round,
                    )
                }

                ReportActionIcon.Menu -> {
                    repeat(3) { index ->
                        val y = size.height * (0.28f + (index * 0.22f))
                        drawLine(
                            color = ReportMutedText,
                            start = Offset(size.width * 0.2f, y),
                            end = Offset(size.width * 0.8f, y),
                            strokeWidth = 1.8f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF090E0B), Color(0xFF18221B)),
                ),
                shape = RoundedCornerShape(48.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xAA78A26B), Color.Transparent),
                        radius = 320f,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🌿",
                fontSize = 110.sp,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x99000000)),
                    ),
                    shape = RoundedCornerShape(48.dp),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .background(ReportDanger, RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReportWarningIcon()
                Text(
                    text = "SEVERIDAD: CRÍTICA",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        letterSpacing = 1.sp,
                    ),
                    color = Color.White,
                )
            }
            Text(
                text = "Roya del Cafeto",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 40.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.75).sp,
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ReportWarningIcon() {
    Canvas(modifier = Modifier.size(width = 13.dp, height = 11.dp)) {
        val triangle = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(triangle, Color.White)
        drawLine(
            color = ReportDanger,
            start = Offset(size.width / 2f, size.height * 0.32f),
            end = Offset(size.width / 2f, size.height * 0.7f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = ReportDanger, radius = 0.9f, center = Offset(size.width / 2f, size.height * 0.84f))
    }
}

@Composable
private fun ReportDiagnosisCard(diagnosis: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReportSoftCard, RoundedCornerShape(48.dp))
            .padding(horizontal = 32.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(ReportPrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(17.dp)) {
                drawArc(
                    color = Color.White,
                    startAngle = 210f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(1f, 1f),
                    size = Size(size.width - 2f, size.height - 2f),
                    style = Stroke(width = 1.7f),
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.78f, size.height * 0.32f),
                    end = Offset(size.width * 0.94f, size.height * 0.18f),
                    strokeWidth = 1.7f,
                    cap = StrokeCap.Round,
                )
            }
        }

        Text(
            text = "Diagnóstico de IA",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
            color = ReportPrimary,
        )
        Text(
            text = diagnosis,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 26.sp,
            ),
            color = ReportMutedText,
        )
    }
}

@Composable
private fun ReportMediaPanel(onScanAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 190.dp, height = 86.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .shadow(1.dp, RoundedCornerShape(20.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReportMediaRow(label = "Fotos")
        ReportMediaRow(label = "Cámara", onClick = onScanAgain)
    }
}

@Composable
private fun ReportMediaRow(
    label: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 15.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(ReportDarkButton, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.14f),
                    size = Size(size.width * 0.76f, size.height * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                    style = Stroke(width = 1.2f),
                )
                drawCircle(Color.White, radius = 1f, center = Offset(size.width * 0.36f, size.height * 0.4f))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 24.sp,
            ),
            color = Color.Black,
        )
    }
}

@Composable
private fun ReportMessageInput(onScanAgain: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(ReportDarkButton, CircleShape)
                .clickable(onClick = onScanAgain),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                drawLine(Color.White, Offset(size.width * 0.5f, size.height * 0.18f), Offset(size.width * 0.5f, size.height * 0.82f), 1.8f, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width * 0.18f, size.height * 0.5f), Offset(size.width * 0.82f, size.height * 0.5f), 1.8f, StrokeCap.Round)
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(Color.White, RoundedCornerShape(48.dp))
                .background(Color.Transparent)
                .padding(horizontal = 20.dp)
                .border(
                    width = 1.5.dp,
                    color = ReportInputBorder,
                    shape = RoundedCornerShape(48.dp),
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Type a message...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
                color = ReportMutedText,
            )
            Canvas(modifier = Modifier.size(20.dp)) {
                drawRoundRect(
                    color = ReportMutedText,
                    topLeft = Offset(size.width * 0.35f, size.height * 0.12f),
                    size = Size(size.width * 0.3f, size.height * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    style = Stroke(1.4f),
                )
                drawLine(ReportMutedText, Offset(size.width * 0.5f, size.height * 0.56f), Offset(size.width * 0.5f, size.height * 0.8f), 1.4f, StrokeCap.Round)
                drawLine(ReportMutedText, Offset(size.width * 0.38f, size.height * 0.8f), Offset(size.width * 0.62f, size.height * 0.8f), 1.4f, StrokeCap.Round)
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(ReportDarkButton, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val plane = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.5f)
                    lineTo(size.width * 0.82f, size.height * 0.24f)
                    lineTo(size.width * 0.64f, size.height * 0.82f)
                    close()
                }
                drawPath(path = plane, color = Color.White.copy(alpha = 0.95f), style = Stroke(width = 1.6f))
            }
        }
    }
}
