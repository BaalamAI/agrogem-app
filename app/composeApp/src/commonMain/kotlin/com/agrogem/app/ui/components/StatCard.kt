package com.agrogem.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.dashboard.DashboardStat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val StatCardBackground = Color(0xFFF7F7F7)
private val StatTextPrimary = Color(0xFF181D1A)
private val StatTextSecondary = Color(0xFF40493D)
private val StatPrimary = Color(0xFF0D631B)
private val StatWarning = Color(0xFFB12D00)

@Composable
fun StatCard(
    stat: DashboardStat,
    modifier: Modifier = Modifier,
) {
    val iconTint = if (stat.id == "humidity") StatWarning else StatPrimary

    Column(
        modifier = modifier
            .background(
                color = StatCardBackground,
                shape = RoundedCornerShape(48.dp),
            )
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatIcon(
                statId = stat.id,
                tint = iconTint,
            )
            SeverityBadge(
                severity = stat.severity,
                labelOverride = stat.badgeLabel,
            )
        }
        Text(
            text = stat.value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = (-1.5).sp,
            ),
            color = StatTextPrimary,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = stat.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.7.sp,
            ),
            color = StatTextSecondary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StatIcon(
    statId: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    when (statId) {
        "humidity" -> HumidityIcon(
            tint = tint,
            modifier = modifier,
        )

        else -> TemperatureIcon(
            tint = tint,
            modifier = modifier,
        )
    }
}

@Composable
private fun TemperatureIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(26.dp)) {
        val center = center
        val coreRadius = size.minDimension * 0.2f
        val stroke = size.minDimension * 0.08f
        val rayStart = coreRadius + (stroke * 1.2f)
        val rayEnd = size.minDimension * 0.46f

        drawCircle(
            color = tint,
            radius = coreRadius,
            center = center,
            style = Stroke(width = stroke),
        )

        repeat(8) { step ->
            val angle = ((step * 45.0) - 90.0) * (PI / 180.0)
            val start = Offset(
                x = center.x + (cos(angle).toFloat() * rayStart),
                y = center.y + (sin(angle).toFloat() * rayStart),
            )
            val end = Offset(
                x = center.x + (cos(angle).toFloat() * rayEnd),
                y = center.y + (sin(angle).toFloat() * rayEnd),
            )

            drawLine(
                color = tint,
                start = start,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun HumidityIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 18.dp, height = 21.dp)) {
        val droplet = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            cubicTo(
                x1 = size.width * 0.84f,
                y1 = size.height * 0.3f,
                x2 = size.width,
                y2 = size.height * 0.54f,
                x3 = size.width,
                y3 = size.height * 0.72f,
            )
            cubicTo(
                x1 = size.width,
                y1 = size.height * 0.9f,
                x2 = size.width * 0.82f,
                y2 = size.height,
                x3 = size.width * 0.5f,
                y3 = size.height,
            )
            cubicTo(
                x1 = size.width * 0.18f,
                y1 = size.height,
                x2 = 0f,
                y2 = size.height * 0.9f,
                x3 = 0f,
                y3 = size.height * 0.72f,
            )
            cubicTo(
                x1 = 0f,
                y1 = size.height * 0.54f,
                x2 = size.width * 0.16f,
                y2 = size.height * 0.3f,
                x3 = size.width * 0.5f,
                y3 = 0f,
            )
            close()
        }

        drawPath(
            path = droplet,
            color = tint,
            style = Stroke(
                width = size.minDimension * 0.1f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
