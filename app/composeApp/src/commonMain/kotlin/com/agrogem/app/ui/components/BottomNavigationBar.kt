package com.agrogem.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.agrogem.app.navigation.AgroGemRoute
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NavigationBackground = Color(0xF2FFFFFF)
private val NavigationBorder = Color(0xFFEFEFEF)
private val NavigationShadow = Color(0x0F181D1A)
private val NavigationActive = Color(0xFF2E7D32)
private val NavigationInactive = Color(0xFF181D1A)

@Composable
fun BottomNavigationBar(
    currentRoute: AgroGemRoute,
    onNavigate: (AgroGemRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 10.dp, spotColor = NavigationShadow)
            .background(NavigationBackground)
            .border(width = 1.dp, color = NavigationBorder),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AgroGemRoute.all.forEach { route ->
            val tint = if (route == currentRoute) NavigationActive else NavigationInactive

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onNavigate(route) },
            ) {
                RouteIcon(
                    route = route,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RouteIcon(
    route: AgroGemRoute,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    when (route) {
        AgroGemRoute.Dashboard -> HomeIcon(tint = tint, modifier = modifier)
        AgroGemRoute.Camera -> CameraIcon(tint = tint, modifier = modifier)
        AgroGemRoute.Analysis -> ChatIcon(tint = tint, modifier = modifier)
        AgroGemRoute.Map -> GlobeIcon(tint = tint, modifier = modifier)
        AgroGemRoute.Report -> SettingsIcon(tint = tint, modifier = modifier)
    }
}

@Composable
private fun HomeIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.1f
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.45f)
            lineTo(size.width * 0.5f, size.height * 0.16f)
            lineTo(size.width * 0.85f, size.height * 0.45f)
            lineTo(size.width * 0.73f, size.height * 0.45f)
            lineTo(size.width * 0.73f, size.height * 0.82f)
            lineTo(size.width * 0.27f, size.height * 0.82f)
            lineTo(size.width * 0.27f, size.height * 0.45f)
            close()
        }
        drawPath(path = path, color = tint, style = Stroke(width = stroke))
        drawLine(
            color = tint,
            start = Offset(size.width * 0.45f, size.height * 0.82f),
            end = Offset(size.width * 0.45f, size.height * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CameraIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        val bodyTop = size.height * 0.32f
        val bodyHeight = size.height * 0.46f

        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.12f, bodyTop),
            size = androidx.compose.ui.geometry.Size(size.width * 0.76f, bodyHeight),
            cornerRadius = CornerRadius(size.width * 0.12f, size.width * 0.12f),
            style = Stroke(width = stroke),
        )

        drawCircle(
            color = tint,
            radius = size.width * 0.14f,
            center = Offset(size.width * 0.5f, bodyTop + (bodyHeight * 0.52f)),
            style = Stroke(width = stroke),
        )

        drawLine(
            color = tint,
            start = Offset(size.width * 0.28f, size.height * 0.25f),
            end = Offset(size.width * 0.43f, size.height * 0.25f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ChatIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        val bubble = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.28f)
            cubicTo(size.width * 0.2f, size.height * 0.18f, size.width * 0.3f, size.height * 0.12f, size.width * 0.4f, size.height * 0.12f)
            lineTo(size.width * 0.6f, size.height * 0.12f)
            cubicTo(size.width * 0.75f, size.height * 0.12f, size.width * 0.86f, size.height * 0.24f, size.width * 0.86f, size.height * 0.39f)
            lineTo(size.width * 0.86f, size.height * 0.54f)
            cubicTo(size.width * 0.86f, size.height * 0.7f, size.width * 0.73f, size.height * 0.82f, size.width * 0.57f, size.height * 0.82f)
            lineTo(size.width * 0.45f, size.height * 0.82f)
            lineTo(size.width * 0.28f, size.height * 0.93f)
            lineTo(size.width * 0.31f, size.height * 0.77f)
            cubicTo(size.width * 0.22f, size.height * 0.72f, size.width * 0.2f, size.height * 0.63f, size.width * 0.2f, size.height * 0.54f)
            close()
        }

        drawPath(path = bubble, color = tint, style = Stroke(width = stroke))

        repeat(3) { index ->
            drawCircle(
                color = tint,
                radius = size.width * 0.035f,
                center = Offset(
                    x = size.width * (0.4f + (index * 0.12f)),
                    y = size.height * 0.47f,
                ),
            )
        }
    }
}

@Composable
private fun GlobeIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        val radius = size.minDimension * 0.4f
        val center = center

        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = stroke),
        )

        drawLine(
            color = tint,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        drawLine(
            color = tint,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        drawOval(
            color = tint,
            topLeft = Offset(center.x - (radius * 0.42f), center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 0.84f, radius * 2f),
            style = Stroke(width = stroke),
        )
    }
}

@Composable
private fun SettingsIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        val center = center
        val outer = size.minDimension * 0.36f
        val inner = size.minDimension * 0.16f

        repeat(8) { step ->
            val angle = (step * 45.0) * (PI / 180.0)
            val start = Offset(
                x = center.x + (cos(angle).toFloat() * (outer - (stroke * 0.8f))),
                y = center.y + (sin(angle).toFloat() * (outer - (stroke * 0.8f))),
            )
            val end = Offset(
                x = center.x + (cos(angle).toFloat() * (outer + (stroke * 0.9f))),
                y = center.y + (sin(angle).toFloat() * (outer + (stroke * 0.9f))),
            )
            drawLine(color = tint, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
        }

        drawCircle(
            color = tint,
            radius = outer,
            center = center,
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = tint,
            radius = inner,
            center = center,
            style = Stroke(width = stroke),
        )
    }
}
