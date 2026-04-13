package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.dashboard.RecentAnalysis

private val AnalysisCardBackground = Color(0xFFF7F7F7)
private val AnalysisTextPrimary = Color(0xFF181D1A)
private val AnalysisTextSecondary = Color(0xFF40493D)

@Composable
fun AnalysisCard(
    analysis: RecentAnalysis,
    onClick: (RecentAnalysis) -> Unit,
    modifier: Modifier = Modifier,
    showCapturedAt: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AnalysisCardBackground,
                shape = RoundedCornerShape(48.dp),
            )
            .clickable { onClick(analysis) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CropThumbnail(
            cropName = analysis.cropName,
            modifier = Modifier.size(96.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = analysis.cropName.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 1.2.sp,
                    ),
                    color = AnalysisTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                SeverityBadge(
                    severity = analysis.severity,
                    compact = true,
                )
            }

            Text(
                text = "Salud: ${analysis.healthPercent}%",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 22.5.sp,
                ),
                color = AnalysisTextPrimary,
            )

            val supportingText = if (showCapturedAt) analysis.capturedAt else analysis.lotName
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                color = AnalysisTextSecondary,
            )
        }
    }
}

@Composable
private fun CropThumbnail(
    cropName: String,
    modifier: Modifier = Modifier,
) {
    val gradient = when (cropName.lowercase()) {
        "albahaca" -> listOf(Color(0xFF1C4A1F), Color(0xFF5A9A4D), Color(0xFF244E1F))
        "tomate" -> listOf(Color(0xFF142B20), Color(0xFF487F52), Color(0xFF1B3524))
        else -> listOf(Color(0xFF2B3D25), Color(0xFF6A8E59), Color(0xFF31482B))
    }

    val marker = when (cropName.lowercase()) {
        "albahaca" -> "🌿"
        "tomate" -> "🍃"
        else -> "🌱"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Text(
            text = marker,
            fontSize = 32.sp,
        )
    }
}
