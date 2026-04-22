package com.agrogem.app.ui.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.ui.viewmodel.kmpViewModel

@Composable
fun MapRiskScreen(
    modifier: Modifier = Modifier,
    onBackToDashboard: () -> Unit = {},
    viewModel: MapRiskViewModel = kmpViewModel { MapRiskViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alertText = "${uiState.alerts.size} Alertas de plagas cercanas\ndetectadas en el valle."

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.MapBackground),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(
                top = 96.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "BUENOS DÍAS, AGRICULTOR",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            letterSpacing = 1.6.sp,
                        ),
                        color = AgroGemColors.MapPrimary,
                    )
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            lineHeight = 45.sp,
                            letterSpacing = (-0.9).sp,
                        ),
                        color = AgroGemColors.MapTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            item {
                MapPreviewCard()
            }

            item {
                AlertSummaryBanner(text = alertText)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        MapBackButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 16.dp),
            onClick = onBackToDashboard,
        )

        MapBrandHeader(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun MapPreviewCard(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(216.dp)
            .background(
                color = AgroGemColors.MapMutedCard,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(1.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = AgroGemColors.MapLinePrimary
            val secondaryLine = AgroGemColors.MapLineSecondary

            repeat(18) { index ->
                val y = size.height * (index / 17f)
                drawLine(
                    color = if (index % 3 == 0) lineColor else secondaryLine,
                    start = Offset(0f, y),
                    end = Offset(size.width, y + ((index % 2) * 4f)),
                    strokeWidth = if (index % 3 == 0) 1.4f else 0.8f,
                    cap = StrokeCap.Round,
                )
            }

            repeat(12) { index ->
                val x = size.width * (index / 11f)
                drawLine(
                    color = if (index % 2 == 0) lineColor else secondaryLine,
                    start = Offset(x, 0f),
                    end = Offset(x - ((index % 3) * 6f), size.height),
                    strokeWidth = if (index % 2 == 0) 1.2f else 0.8f,
                    cap = StrokeCap.Round,
                )
            }
        }

        Text(
            text = "VER MAPA DE ALERTAS  →",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 12.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 1.2.sp,
            ),
            color = AgroGemColors.MapAlertColor,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun AlertSummaryBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AgroGemColors.MapMutedCard,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 4.5.dp),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        color = AgroGemColors.MapTextSecondary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun MapBrandHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AgroGemColors.MapBackground.copy(alpha = 0.86f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapLeafMarkIcon()
            Text(
                text = "Agrogemma",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-1.2).sp,
                ),
                color = AgroGemColors.MapPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun MapBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(
                color = Color.White.copy(alpha = 0.4f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            drawLine(
                color = AgroGemColors.MapTextSecondary,
                start = Offset(size.width * 0.68f, size.height * 0.2f),
                end = Offset(size.width * 0.32f, size.height * 0.5f),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = AgroGemColors.MapTextSecondary,
                start = Offset(size.width * 0.32f, size.height * 0.5f),
                end = Offset(size.width * 0.68f, size.height * 0.8f),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MapLeafMarkIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val stroke = size.minDimension * 0.13f

        drawOval(
            color = AgroGemColors.MapPrimary,
            topLeft = Offset(x = size.width * 0.08f, y = size.height * 0.2f),
            size = androidx.compose.ui.geometry.Size(width = size.width * 0.78f, height = size.height * 0.58f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )

        drawLine(
            color = AgroGemColors.MapPrimary,
            start = Offset(x = size.width * 0.72f, y = size.height * 0.2f),
            end = Offset(x = size.width * 0.28f, y = size.height * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
