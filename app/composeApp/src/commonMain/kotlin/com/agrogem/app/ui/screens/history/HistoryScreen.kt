package com.agrogem.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.agrogem.app.ui.screens.figma.historyToday
import com.agrogem.app.ui.screens.figma.historyYesterday
import com.agrogem.app.ui.screens.figma.components.LeafThumb
import com.agrogem.app.ui.screens.figma.components.StatusBadge

@Composable
fun HistoryScreen(
    onOpenEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaColors.Screen)
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Historial de análisis",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )

        SearchBar()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { DateHeader("HOY, 24 OCTUBRE") }
            items(historyToday) { entry ->
                HistoryCard(entry = entry, onOpenEntry = onOpenEntry)
            }
            item { DateHeader("AYER, 23 OCTUBRE") }
            items(historyYesterday) { entry ->
                HistoryCard(entry = entry, onOpenEntry = onOpenEntry)
            }
        }
    }
}

@Composable
private fun SearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "⌕", color = Color(0xFFA5A5A5), fontSize = 16.sp)
        Text(text = "Buscar", color = Color(0xFFB6B6B6), fontSize = 14.sp)
    }
}

@Composable
private fun DateHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFDDE2DF)))
        Text(
            text = label,
            color = Color(0xFF707A6C),
            fontSize = 11.sp,
            letterSpacing = 1.1.sp,
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFDDE2DF)))
    }
}

@Composable
private fun HistoryCard(
    entry: com.agrogem.app.ui.screens.figma.HistoryEntry,
    onOpenEntry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.Surface, RoundedCornerShape(48.dp))
            .padding(16.dp)
            .clickable(onClick = onOpenEntry),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafThumb(seed = entry.seed, rounded = 16.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = entry.crop, color = FigmaColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = entry.meta, color = FigmaColors.TextHint, fontSize = 14.sp)
            StatusBadge(entry.tone, labelOverride = entry.status)
        }
        Text(text = "›", color = Color(0xFFB7C2B5), fontSize = 22.sp)
    }
}
