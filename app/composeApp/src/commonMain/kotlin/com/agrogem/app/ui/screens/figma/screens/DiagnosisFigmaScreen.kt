package com.agrogem.app.ui.screens.figma.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agrogem.app.ui.screens.figma.components.DiagnosisBaseLayout

@Composable
fun DiagnosisFigmaScreen(
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiagnosisBaseLayout(
        modifier = modifier,
        topInset = 455.dp,
        sliceCollapsedOffset = 200.dp,
        showCaptureOverlay = true,
        showProducts = false,
        primaryButtonText = "Hablar con Agente",
        secondaryButtonText = "Hablar con agente",
        singleBottomAction = null,
        onPrimaryAction = onOpenPlan,
        onSecondaryAction = onOpenPlan,
    )
}
