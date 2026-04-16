package com.agrogem.app.ui.screens.home

import androidx.compose.foundation.background
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
import com.agrogem.app.ui.screens.figma.FigmaColors
import com.agrogem.app.ui.screens.figma.dashboardRecentItems
import com.agrogem.app.ui.screens.figma.components.LeafThumb
import com.agrogem.app.ui.screens.figma.components.Pill

import com.agrogem.app.ui.screens.figma.components.RoundIconButton
import com.agrogem.app.ui.screens.figma.components.StatusBadge

@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaColors.Screen)
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill(
                text = "Guatemala, Zacapa",
                background = FigmaColors.Surface,
                foreground = Color(0xFF383838),
                icon = "•",
                iconColor = Color(0xFF4B5E4C),
                horizontal = 12.dp,
                vertical = 6.dp,
            )
            RoundIconButton(
                label = "🔔",
                background = FigmaColors.Surface,
                onClick = {},
            )
        }

        WeatherCard()
        MetricsCard()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FigmaColors.Surface, RoundedCornerShape(30.dp))
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
                    color = Color.Black,
                    fontSize = 32.sp / 1.75f,
                    lineHeight = 28.sp,
                )
                Text(
                    text = "Ver todo",
                    color = Color.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onOpenHistory),
                )
            }

            dashboardRecentItems.forEachIndexed { index, item ->
                RecentAnalysisRow(item = item, seed = index)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WeatherCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "GUATEMALA, ZACAPA",
                color = Color(0xFF4E4E4E),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = "24°C",
                color = Color(0xFF242424),
                fontSize = 52.sp / 1.75f,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Día despejado",
                color = Color(0xFF4E4E4E),
                fontSize = 16.sp,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Pill(
                text = "Today",
                background = Color(0xFFF1F1F1),
                foreground = Color(0xFF8B8B8B),
                icon = "⌄",
                iconColor = Color(0xFF8B8B8B),
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 9.sp,
            )
            Text(
                text = "☀",
                fontSize = 34.sp,
                color = Color(0xFF4D4D4D),
            )
            Text(
                text = "Monday, 12 Oct",
                color = Color(0xFF616161),
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
            .background(FigmaColors.Surface, RoundedCornerShape(15.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricItem(icon = "💧", value = "78%", label = "HUMIDITY")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(Color(0x264D4D4D)),
        )
        MetricItem(icon = "☁", value = "65%", label = "CLOUDS")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(Color(0x264D4D4D)),
        )
        MetricItem(icon = "☼", value = "Low", label = "UV INDEX")
    }
}

@Composable
private fun MetricItem(
    icon: String,
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = icon, fontSize = 17.sp)
        Text(text = value, color = Color(0xFF4C4C4C), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text = label, color = Color(0x994C4C4C), fontSize = 10.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun RecentAnalysisRow(
    item: com.agrogem.app.ui.screens.figma.RecentAnalysisItem,
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
                    color = Color(0xFF6C6C6C),
                    fontSize = 12.sp,
                    letterSpacing = 1.1.sp,
                )
                StatusBadge(tone = item.tone)
            }

            Text(
                text = item.health,
                color = Color.Black,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            )
            Text(
                text = item.subtitle,
                color = Color.Black,
                fontSize = 14.sp,
            )
        }
    }
}
