package com.agrogem.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.ic_action_notifications
import app.composeapp.generated.resources.ic_metric_cloud
import app.composeapp.generated.resources.ic_metric_location
import app.composeapp.generated.resources.ic_metric_uv
import app.composeapp.generated.resources.ic_metric_water
import app.composeapp.generated.resources.ic_weather_sunny
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.RoundIconButton
import com.agrogem.app.ui.components.SeverityBadge
import com.agrogem.app.ui.components.LeafThumb
import com.agrogem.app.ui.components.Pill
import com.agrogem.app.ui.preview.RecentAnalysisItem
import com.agrogem.app.ui.preview.dashboardRecentItems
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenGemmaDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen)
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocationChip(text = "Guatemala, Zacapa")
            RoundIconButton(
                label = "🔔",
                icon = Res.drawable.ic_action_notifications,
                contentDescription = "Notifications",
                background = AgroGemColors.Surface,
                foreground = AgroGemColors.IconBellTint,
                size = 42.dp,
                onClick = {},
            )
        }

        WeatherCard()
        MetricsCard()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AgroGemColors.Surface, RoundedCornerShape(30.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Análisis Recientes",
                    color = AgroGemColors.TextPrimary,
                    fontSize = 32.sp / 1.75f,
                    lineHeight = 28.sp,
                )
                Text(
                    text = "Ver todo",
                    color = AgroGemColors.TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onOpenHistory),
                )
            }

            dashboardRecentItems.forEachIndexed { index, item ->
                RecentAnalysisRow(item = item, seed = index)
            }
        }

        // SECCIÓN DEMO PARA DESARROLLADORES
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(AgroGemColors.Primary, RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenGemmaDemo),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PROBAR GEMMA 4 (DEMO)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LocationChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(AgroGemColors.Surface, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgroGemIcon(
            icon = Res.drawable.ic_metric_location,
            contentDescription = "Location",
            tint = AgroGemColors.TextLocation,
            size = null,
            modifier = Modifier
                .width(8.dp)
                .height(11.dp),
        )
        Text(
            text = text,
            color = AgroGemColors.TextDark,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun WeatherCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_metric_location,
                    contentDescription = "Weather location",
                    tint = AgroGemColors.TextLocationSmall,
                    size = null,
                    modifier = Modifier
                        .width(7.dp)
                        .height(10.dp),
                )
                Text(
                    text = "GUATEMALA, ZACAPA",
                    color = AgroGemColors.TextMedium,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                text = "24°C",
                color = AgroGemColors.TextBody,
                fontSize = 52.sp / 1.75f,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Día despejado",
                color = AgroGemColors.TextMedium,
                fontSize = 16.sp,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Pill(
                text = "Today",
                background = AgroGemColors.Surface,
                foreground = AgroGemColors.TextGraySecondary,
                icon = "⌄",
                iconColor = AgroGemColors.TextGraySecondary,
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 9.sp,
                modifier = Modifier.border(1.dp, AgroGemColors.PillTrackBorder, RoundedCornerShape(999.dp)),
            )
            AgroGemIcon(
                icon = Res.drawable.ic_weather_sunny,
                contentDescription = "Sunny weather",
                tint = AgroGemColors.TextIconGray,
                size = AgroGemIconSizes.Lg,
            )
            Text(
                text = "Monday, 12 Oct",
                color = AgroGemColors.TextLabel,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun MetricsCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(15.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricItem(icon = Res.drawable.ic_metric_water, value = "78%", label = "HUMIDITY")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(AgroGemColors.MetricDivider),
        )
        MetricItem(icon = Res.drawable.ic_metric_cloud, value = "65%", label = "CLOUDS")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(AgroGemColors.MetricDivider),
        )
        MetricItem(icon = Res.drawable.ic_metric_uv, value = "Low", label = "UV INDEX")
    }
}

@Composable
private fun MetricItem(
    icon: DrawableResource,
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AgroGemIcon(
            icon = icon,
            contentDescription = label,
            size = AgroGemIconSizes.Sm,
        )
        Text(text = value, color = AgroGemColors.TextGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text = label, color = AgroGemColors.MetricTextAlpha, fontSize = 10.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun RecentAnalysisRow(
    item: RecentAnalysisItem,
    seed: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafThumb(seed = seed)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    color = AgroGemColors.TextGrayMuted,
                    fontSize = 12.sp,
                    letterSpacing = 1.1.sp,
                )
                SeverityBadge(severity = item.severity)
            }

            Text(
                text = item.health,
                color = AgroGemColors.TextPrimary,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            )
            Text(
                text = item.subtitle,
                color = AgroGemColors.TextPrimary,
                fontSize = 14.sp,
            )
        }
    }
}
