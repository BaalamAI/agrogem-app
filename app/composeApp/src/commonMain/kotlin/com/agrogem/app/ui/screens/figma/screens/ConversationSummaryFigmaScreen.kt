package com.agrogem.app.ui.screens.figma.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agrogem.app.ui.screens.figma.components.DiagnosisBaseLayout

@Composable
fun ConversationSummaryFigmaScreen(
    onViewConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiagnosisBaseLayout(
        modifier = modifier,
        topInset = 57.dp,
        sliceCollapsedOffset = 360.dp,
        showCaptureOverlay = false,
        showProducts = true,
        showVoiceBadge = true,
        primaryButtonText = "Ver conversación",
        singleBottomAction = "Ver conversación",
        onPrimaryAction = onViewConversation,
        onSecondaryAction = null,
    )
}
