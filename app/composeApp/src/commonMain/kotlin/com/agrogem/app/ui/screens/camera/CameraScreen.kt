package com.agrogem.app.ui.screens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.agrogem.app.theme.AgroGemPalette

@Composable
fun CameraScreen(
    onStartAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = remember { CameraViewModel() },
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = AgroGemPalette.spacing

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.padding(top = spacing.lg),
            ) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = uiState.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            CameraPlaceholder(
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = "Guía rápida",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        items(uiState.guideLines) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = AgroGemPalette.shapes.card,
                    )
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
            ) {
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.onEvent(CameraEvent.OnStartAnalysis)
                    onStartAnalysis()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm),
            ) {
                Text(uiState.primaryActionLabel)
            }
        }

        item {
            Text(
                text = uiState.hint,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.lg),
            )
        }
    }
}
