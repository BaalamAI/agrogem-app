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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.ic_action_search
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.LeafThumb
import com.agrogem.app.ui.components.SeverityBadge

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenEntry: (PersistedHistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen)
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Historial de análisis",
            color = AgroGemColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )

        SearchBar()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (uiState.entries.isNotEmpty()) {
                item { DateHeader("ANÁLISIS RECIENTES") }
                items(uiState.entries, key = { it.analysisId }) { entry ->
                    HistoryCard(entry = entry, onOpenEntry = onOpenEntry)
                }
            }
        }
    }
}

@Composable
private fun SearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, AgroGemColors.BorderDivider, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgroGemIcon(
            icon = Res.drawable.ic_action_search,
            contentDescription = "Search",
            tint = AgroGemColors.TextSearchIcon,
            size = AgroGemIconSizes.Sm,
        )
        Text(text = "Buscar", color = AgroGemColors.TextPlaceholder, fontSize = 14.sp)
    }
}

@Composable
private fun DateHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AgroGemColors.DividerLine))
        Text(
            text = label,
            color = AgroGemColors.TextDateHeader,
            fontSize = 11.sp,
            letterSpacing = 1.1.sp,
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AgroGemColors.DividerLine))
    }
}

@Composable
private fun HistoryCard(
    entry: PersistedHistoryEntry,
    onOpenEntry: (PersistedHistoryEntry) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(48.dp))
            .padding(16.dp)
            .clickable(onClick = { onOpenEntry(entry) }),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafThumb(seed = entry.analysisId.hashCode(), rounded = 16.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = entry.crop, color = AgroGemColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = entry.meta, color = AgroGemColors.TextHint, fontSize = 14.sp)
            SeverityBadge(severity = entry.severity, labelOverride = entry.status)
        }
        Text(text = "›", color = AgroGemColors.TextNavChevron, fontSize = 22.sp)
    }
}
