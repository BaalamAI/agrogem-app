package com.agrogem.app.ui.screens.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val CameraSurfaceDark = Color(0xFF0D1310)
private val CameraSurfaceMid = Color(0xFF16201A)
private val CameraSurfaceLight = Color(0xFF2A372F)
private val CameraPrimary = Color(0xFF0D631B)
private val CameraNeon = Color(0xFFA3F69C)
private val CameraHintBg = Color(0xCC181D1A)
private val CameraSoftWhite = Color(0xD9FFFFFF)

@Composable
fun CameraScreen(
    onStartAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = remember { CameraViewModel() },
) {
    val _uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CameraSurfaceDark, CameraSurfaceMid, CameraSurfaceLight),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(9) { index ->
                val offsetX = (size.width / 10f) * (index + 1)
                drawCircle(
                    color = Color(0x663F5E43),
                    radius = 58f + (index * 6f),
                    center = Offset(offsetX, size.height * (0.26f + ((index % 3) * 0.1f))),
                )
            }

            drawRect(
                color = CameraPrimary.copy(alpha = 0.9f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 2f),
            )

            drawRoundRect(
                color = CameraNeon,
                topLeft = Offset(15f, 16f),
                size = Size(32f, 32f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = Stroke(1.8f),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 194.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(4) { index ->
                CameraThumb(index = index)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 297.dp)
                .size(width = 32.dp, height = 4.dp)
                .background(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                ),
        )

        CameraControlButton(
            icon = CameraControlIcon.Close,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 23.dp, top = 24.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 23.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CameraControlButton(icon = CameraControlIcon.Gallery)

                ShutterButton(
                    onClick = {
                        viewModel.onEvent(CameraEvent.OnStartAnalysis)
                        onStartAnalysis()
                    },
                )

                CameraControlButton(icon = CameraControlIcon.Reload)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.5.dp)
                    .background(CameraHintBg, androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CameraNeon, CircleShape),
                )
                Text(
                    text = "tomar foto para analizar con ia",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

private enum class CameraControlIcon {
    Close,
    Gallery,
    Reload,
}

@Composable
private fun CameraThumb(index: Int) {
    val colors = when (index % 2) {
        0 -> listOf(Color(0xFF30563A), Color(0xFF183021), Color(0xFF4F7B4A))
        else -> listOf(Color(0xFF273E2D), Color(0xFF15241B), Color(0xFF5A6F4B))
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                brush = Brush.linearGradient(colors),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (index % 2 == 0) "🌿" else "🍃",
            fontSize = 28.sp,
        )
    }
}

@Composable
private fun CameraControlButton(
    icon: CameraControlIcon,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(CameraSoftWhite, CircleShape)
            .clickable {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            when (icon) {
                CameraControlIcon.Close -> {
                    drawLine(Color(0xFF181D1A), Offset(size.width * 0.25f, size.height * 0.25f), Offset(size.width * 0.75f, size.height * 0.75f), 1.8f, StrokeCap.Round)
                    drawLine(Color(0xFF181D1A), Offset(size.width * 0.75f, size.height * 0.25f), Offset(size.width * 0.25f, size.height * 0.75f), 1.8f, StrokeCap.Round)
                }

                CameraControlIcon.Gallery -> {
                    drawRoundRect(
                        color = Color(0xFF181D1A),
                        topLeft = Offset(size.width * 0.16f, size.height * 0.2f),
                        size = Size(size.width * 0.68f, size.height * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                        style = Stroke(1.8f),
                    )
                    val mountain = Path().apply {
                        moveTo(size.width * 0.26f, size.height * 0.68f)
                        lineTo(size.width * 0.43f, size.height * 0.46f)
                        lineTo(size.width * 0.56f, size.height * 0.6f)
                        lineTo(size.width * 0.74f, size.height * 0.36f)
                    }
                    drawPath(mountain, Color(0xFF181D1A), style = Stroke(1.8f))
                    drawCircle(Color(0xFF181D1A), radius = 1.8f, center = Offset(size.width * 0.36f, size.height * 0.37f))
                }

                CameraControlIcon.Reload -> {
                    drawArc(
                        color = Color(0xFF181D1A),
                        startAngle = 35f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
                        size = Size(size.width * 0.6f, size.height * 0.6f),
                        style = Stroke(1.8f),
                    )
                    drawLine(
                        color = Color(0xFF181D1A),
                        start = Offset(size.width * 0.73f, size.height * 0.24f),
                        end = Offset(size.width * 0.82f, size.height * 0.38f),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .background(Color(0xD9FFFFFF), CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .background(Color.White, CircleShape),
            )
        }
    }
}
