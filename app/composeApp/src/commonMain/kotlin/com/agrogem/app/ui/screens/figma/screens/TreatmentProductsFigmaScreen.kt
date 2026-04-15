package com.agrogem.app.ui.screens.figma.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agrogem.app.ui.screens.figma.components.DiagnosisBaseLayout

@Composable
fun TreatmentProductsFigmaScreen(
    onSaveAndExit: () -> Unit,
    onTalk: () -> Unit,
    onOpenConversationSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiagnosisBaseLayout(
        modifier = modifier,
        topInset = 57.dp,
        sliceCollapsedOffset = 360.dp,
        showCaptureOverlay = false,
        showProducts = true,
        showVoiceBadge = true,
        showLinkToProducts = false,
        primaryButtonText = "Guardar y salir",
        secondaryButtonText = "Hablar con agente",
        onPrimaryAction = onSaveAndExit,
        onSecondaryAction = onTalk,
        onOpenConversationSummary = onOpenConversationSummary,
    )
}
