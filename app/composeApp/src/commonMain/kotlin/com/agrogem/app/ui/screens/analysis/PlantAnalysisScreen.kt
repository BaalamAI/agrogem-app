package com.agrogem.app.ui.screens.analysis

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.ic_action_sound
import app.composeapp.generated.resources.ic_status_check
import app.composeapp.generated.resources.ic_status_plant
import app.composeapp.generated.resources.ic_status_shield
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.DiagnosisInfoBox
import com.agrogem.app.ui.components.DragHandle
import com.agrogem.app.ui.components.DraggableSlice
import com.agrogem.app.ui.components.FilledPrimaryButton
import com.agrogem.app.ui.components.OutlinedPrimaryButton
import com.agrogem.app.ui.components.PlantBackdrop
import com.agrogem.app.ui.components.PrimaryActionHint

/**
 * Unified plant analysis screen.
 * Shows captured image as backdrop; bottom sheet transitions between
 * Analyzing (progress steps) and Results (diagnosis + treatment) phases.
 *
 * @param fromHistory When true, the primary action label changes to "Regresar"
 *                    instead of "Guardar y salir".
 */
@Composable
fun PlantAnalysisScreen(
    viewModel: AnalysisFlowViewModel,
    onCancel: () -> Unit,
    onExit: () -> Unit,
    onTalkToAgent: (analysisId: String, diagnosis: DiagnosisResult) -> Unit,
    fromHistory: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val capturedImage by viewModel.capturedImage.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val isAnalyzing = phase is AnalysisPhase.Analyzing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.AnalysisBackdrop),
    ) {
        // Full-screen backdrop: real captured image or dark gradient fallback
        PlantBackdrop(
            imageUri = capturedImage?.uri,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.96f,
        )

        // "ANALIZANDO..." label — only during analysis phase
        if (isAnalyzing) {
            PrimaryActionHint(
                text = "ANALIZANDO CULTIVO CON IA...",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 375.dp),
            )
        }

        // ─── Bottom sheet ────────────────────────────────────────────────────
        // Keep the sheet surface attached to the draggable content to avoid
        // exposing a static white layer while swiping it down.
        DraggableSlice(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    AgroGemColors.Surface,
                    RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                )
                .padding(horizontal = 24.dp, vertical = 20.dp),
            collapsedOffset = 240.dp,
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.Top),
        ) {
            DragHandle()

            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "phase_transition",
            ) { currentPhase ->
                when (currentPhase) {
                    is AnalysisPhase.Analyzing -> AnalyzingContent(
                        steps = steps,
                        onCancel = onCancel,
                    )

                    is AnalysisPhase.Results -> ResultsContent(
                        result = viewModel.diagnosisResult,
                        exitLabel = if (fromHistory) "Regresar" else "Guardar y salir",
                        onExit = onExit,
                        onTalkToAgent = onTalkToAgent,
                        analysisId = "analysis_current",
                    )

                    is AnalysisPhase.Error -> ErrorContent(
                        message = currentPhase.message,
                        retryable = currentPhase.retryable,
                        onRetry = { viewModel.startAnalysis(viewModel.capturedImage.value ?: return@ErrorContent) },
                        onCancel = onCancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzingContent(
    steps: List<AnalysisStepUi>,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AgroGemColors.SurfaceMuted, RoundedCornerShape(48.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            steps.forEachIndexed { index, step ->
                val alphas = listOf(1f, 0.65f, 0.42f)
                val alpha = if (step.done) 1f else alphas.getOrElse(index) { 0.32f }
                val iconBg = if (step.done) AgroGemColors.AnalysisStepDone else AgroGemColors.AnalysisStepPending
                val titleColor = if (step.done) AgroGemColors.Primary else AgroGemColors.TextPrimary

                AnalysisStepRow(
                    iconBackground = iconBg,
                    title = step.title,
                    subtitle = step.subtitle,
                    titleColor = titleColor,
                    alpha = alpha,
                )
            }
        }

        OutlinedPrimaryButton(text = "Cancelar Análisis", onClick = onCancel)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = AgroGemColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (retryable) {
            FilledPrimaryButton(
                text = "Reintentar",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedPrimaryButton(
            text = "Cancelar",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AnalysisStepRow(
    iconBackground: Color,
    title: String,
    subtitle: String,
    titleColor: Color = AgroGemColors.TextPrimary,
    alpha: Float,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.alpha(alpha),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (iconBackground == AgroGemColors.AnalysisStepDone) "✓" else "◌",
                color = Color.White,
                fontSize = 13.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = titleColor, fontSize = 14.sp)
            Text(text = subtitle, color = AgroGemColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResultsContent(
    result: DiagnosisResult,
    exitLabel: String,
    onExit: () -> Unit,
    onTalkToAgent: (analysisId: String, diagnosis: DiagnosisResult) -> Unit,
    analysisId: String,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header: pest name + severity pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = result.pestName,
                    color = AgroGemColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AgroGemColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_sound,
                        contentDescription = "Sound",
                        tint = AgroGemColors.IconOnPrimary,
                        size = 15.dp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(AgroGemColors.AlertSoft, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(text = result.severity, color = AgroGemColors.Alert, fontSize = 8.sp)
            }
        }

        // Confidence pill
        Box(
            modifier = Modifier
                .background(AgroGemColors.ConfidenceBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_status_check,
                    contentDescription = "Confidence",
                    tint = AgroGemColors.ConfidenceText,
                    size = AgroGemIconSizes.Xs,
                )
                Text(
                    text = "${(result.confidence * 100).toInt()}% de confianza",
                    color = AgroGemColors.ConfidenceText,
                    fontSize = 8.sp,
                )
            }
        }

        // Info boxes: affected area + cause
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DiagnosisInfoBox(label = "Área afectada", value = result.affectedArea, modifier = Modifier.weight(1f))
            DiagnosisInfoBox(label = "Causa", value = result.cause, modifier = Modifier.weight(1f))
        }

        // Diagnosis body
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_status_plant,
                    contentDescription = "Diagnosis",
                    tint = Color.Unspecified,
                    size = AgroGemIconSizes.Sm,
                )
                Text(text = "Diagnóstico", color = AgroGemColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = result.diagnosisText,
                color = AgroGemColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 26.sp,
            )
        }

        // Treatment plan
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_status_shield,
                    contentDescription = "Treatment plan",
                    tint = Color.Unspecified,
                    size = 15.dp,
                )
                Text(
                    text = "Plan de tratamiento",
                    color = AgroGemColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            result.treatmentSteps.forEachIndexed { index, step ->
                TreatmentStepRow(number = "${index + 1}", text = step)
                if (index < result.treatmentSteps.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(AgroGemColors.DividerThin))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedPrimaryButton(
                text = exitLabel,
                onClick = onExit,
                modifier = Modifier.weight(1f),
            )
            FilledPrimaryButton(
                text = "Hablar con agente",
                onClick = { onTalkToAgent(analysisId, result) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TreatmentStepRow(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AgroGemColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number, color = Color.White, fontSize = 12.sp)
        }
        Text(text = text, color = AgroGemColors.TextSecondary, fontSize = 16.sp, lineHeight = 26.sp)
    }
}
