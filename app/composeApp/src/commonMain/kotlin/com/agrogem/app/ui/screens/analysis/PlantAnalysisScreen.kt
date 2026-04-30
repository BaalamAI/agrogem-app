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
import androidx.compose.foundation.layout.width
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
    val currentAnalysisId by viewModel.analysisId.collectAsStateWithLifecycle()

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
                        analysisId = currentAnalysisId ?: "analysis_current",
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
    val doneCount = steps.count { it.done }
    val totalSteps = steps.size.coerceAtLeast(1)
    val currentStepIndex = steps.indexOfFirst { !it.done }.let { if (it == -1) steps.lastIndex else it }
    val progressLabel = when {
        doneCount == 0 -> "Planta en revisión"
        doneCount < totalSteps -> "Planta en diagnóstico"
        else -> "Diagnóstico completado"
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AgroGemColors.SurfaceMuted, RoundedCornerShape(28.dp))
                .border(1.dp, AgroGemColors.DividerThin, RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(AgroGemColors.AnalysisStepPending, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (doneCount == totalSteps) "🌿" else "🌱",
                    fontSize = 16.sp,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = progressLabel,
                    color = AgroGemColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(totalSteps) { index ->
                        val isReached = index < doneCount
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(4.dp)
                                .background(
                                    if (isReached) AgroGemColors.Primary else AgroGemColors.DividerThin,
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                    }
                }
            }
            Text(
                text = "$doneCount/$totalSteps",
                color = AgroGemColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AgroGemColors.SurfaceMuted, RoundedCornerShape(48.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            steps.forEachIndexed { index, step ->
                val alphas = listOf(1f, 0.65f, 0.42f)
                val isCurrent = index == currentStepIndex && !step.done
                val alpha = when {
                    step.done -> 1f
                    isCurrent -> 0.92f
                    else -> alphas.getOrElse(index) { 0.32f }
                }
                val iconBg = when {
                    step.done -> AgroGemColors.AnalysisStepDone
                    isCurrent -> AgroGemColors.Primary
                    else -> AgroGemColors.AnalysisStepPending
                }
                val titleColor = if (step.done || isCurrent) AgroGemColors.Primary else AgroGemColors.TextPrimary

                AnalysisStepRow(
                    iconBackground = iconBg,
                    title = step.title,
                    subtitle = step.subtitle,
                    titleColor = titleColor,
                    alpha = alpha,
                    indicator = when {
                        step.done -> "✓"
                        isCurrent -> "●"
                        else -> "◌"
                    },
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
    indicator: String,
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
                text = indicator,
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
    val infoItems = listOfNotNull(
        result.affectedArea.takeIf { it.isNotBlank() }?.let { "Área afectada" to it },
        result.cause.takeIf { it.isNotBlank() }?.let { "Causa" to it },
    )

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = result.pestName,
                    modifier = Modifier.weight(1f),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(AgroGemColors.AlertSoft, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(text = result.severity, color = AgroGemColors.Alert, fontSize = 10.sp)
                }

                if (result.isConfidenceReliable) {
                    Box(
                        modifier = Modifier
                            .background(AgroGemColors.ConfidenceBg, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }

        // Info boxes: affected area + cause
        if (infoItems.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                infoItems.forEach { (label, value) ->
                    DiagnosisInfoBox(label = label, value = value, modifier = Modifier.weight(1f))
                }
            }
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
