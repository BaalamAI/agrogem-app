package com.agrogem.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.ic_action_add
import app.composeapp.generated.resources.ic_action_back
import app.composeapp.generated.resources.ic_action_camera
import app.composeapp.generated.resources.ic_action_gallery
import app.composeapp.generated.resources.ic_action_magic
import app.composeapp.generated.resources.ic_action_menu
import app.composeapp.generated.resources.ic_action_mic
import app.composeapp.generated.resources.ic_action_sound
import app.composeapp.generated.resources.ic_status_check
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.AgroGemIconColors
import com.agrogem.app.ui.screens.figma.FigmaColors
import com.agrogem.app.ui.screens.figma.components.FilledPrimaryButton
import com.agrogem.app.ui.screens.figma.components.Pill
import com.agrogem.app.ui.screens.figma.components.RoundIconButton

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onRequestClose: () -> Unit,
    onMicClick: () -> Unit,
    onLaunchCamera: () -> Unit = {},
    onLaunchGallery: () -> Unit = {},
    showConfirmDialog: Boolean,
    onConfirmClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingAttachments = uiState.attachments
    val chatMode = uiState.mode
    val messages = uiState.messages

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaColors.Screen),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(label = "‹", icon = Res.drawable.ic_action_back, contentDescription = "Back", onClick = onBack)
                RoundIconButton(label = "≡", icon = Res.drawable.ic_action_menu, contentDescription = "Menu", onClick = onRequestClose, foreground = Color(0xFF929292))
                Text(
                    text = "Guardado automáticamente 11:58",
                    color = Color(0xFFABABAB),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(FigmaColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_sound,
                        contentDescription = "Sound",
                        tint = AgroGemIconColors.OnPrimary,
                        size = AgroGemIconSizes.Xs,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show diagnosis header only when chat is seeded with analysis context
            val seededDiagnosis = (chatMode as? ChatMode.AnalysisSeeded)?.diagnosis
            if (seededDiagnosis != null) {
                SeededChatHeader(diagnosis = seededDiagnosis)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Render messages from state — seed message (assistant) appears first in seeded mode
            messages.forEach { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            ChatInputArea(
                inputText = uiState.inputText,
                onInputChanged = { viewModel.onEvent(ChatEvent.InputChanged(it)) },
                onAttachClick = { viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(true)) },
                onMicClick = onMicClick,
                onSendClick = { viewModel.onEvent(ChatEvent.SendMessage) },
                pendingAttachments = pendingAttachments,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!showConfirmDialog && uiState.showAttachmentMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { viewModel.onEvent(ChatEvent.ToggleAttachmentMenu(false)) },
                    ),
            )

            AttachmentMenu(
                onPhotosClick = {
                    viewModel.onEvent(ChatEvent.RequestGallery)
                    onLaunchGallery()
                },
                onCameraClick = {
                    viewModel.onEvent(ChatEvent.RequestCamera)
                    onLaunchCamera()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = (-68).dp)
                    .padding(start = 25.dp),
            )
        }

        if (showConfirmDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x4D000000)),
            )

            ConfirmDialog(
                onConfirm = { onConfirmClose?.invoke() },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SeededChatHeader(diagnosis: DiagnosisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = diagnosis.pestName,
                color = Color.Black,
                fontSize = 32.sp / 1.75f,
                fontWeight = FontWeight.Medium,
            )
            Pill(
                text = diagnosis.severity,
                background = FigmaColors.AlertSoft,
                foreground = FigmaColors.Alert,
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 8.sp,
            )
        }

        Row(
            modifier = Modifier
                .background(FigmaColors.ConfidenceBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_status_check,
                contentDescription = "Confidence",
                tint = FigmaColors.ConfidenceText,
                size = AgroGemIconSizes.Xs,
            )
            Text(
                text = "${(diagnosis.confidence * 100).toInt()}% de confianza",
                color = FigmaColors.ConfidenceText,
                fontSize = 8.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DiagnosisInfoBox(
                label = "Área afectada",
                value = diagnosis.affectedArea,
                modifier = Modifier.weight(1f),
            )
            DiagnosisInfoBox(
                label = "Causa",
                value = diagnosis.cause,
                italicTail = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DiagnosisInfoBox(
    label: String,
    value: String,
    italicTail: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(51.dp)
            .background(FigmaColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFEDEDED), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, color = Color(0xFF747474), fontSize = 8.sp)
        if (italicTail) {
            Text(
                text = value,
                color = Color.Black,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Text(text = value, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Renders a single chat message bubble, styled according to sender.
 */
@Composable
private fun MessageBubble(message: ChatMessage) {
    when (message.sender) {
        MessageSender.Assistant -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_magic,
                    contentDescription = "AgroGem assistant",
                    tint = FigmaColors.Primary,
                    size = AgroGemIconSizes.Md,
                )
                Text(
                    text = message.text,
                    color = Color.Black,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        MessageSender.User -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(FigmaColors.Primary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatInputArea(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    pendingAttachments: List<ChatAttachment>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(FigmaColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val hasPendingAttachments = pendingAttachments.isNotEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (hasPendingAttachments) FigmaColors.Primary else Color(0xFFE5E5E5), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasPendingAttachments) "${pendingAttachments.size}" else "○",
                    color = if (hasPendingAttachments) Color.White else Color(0xFF7A7A7A),
                    fontSize = 10.sp,
                )
            }

            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 12.sp,
                ),
                maxLines = 3,
                decorationBox = { innerTextField ->
                    if (inputText.isBlank()) {
                        Text(
                            text = if (hasPendingAttachments) {
                                "${pendingAttachments.size} adjuntos listos"
                            } else {
                                "Preguntale algo sobre tus cultivos"
                            },
                            color = if (hasPendingAttachments) FigmaColors.Primary else Color(0xFFBDBDBD),
                            fontSize = 12.sp,
                        )
                    }
                    innerTextField()
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(label = "+", icon = Res.drawable.ic_action_add, contentDescription = "Add attachment", onClick = onAttachClick, background = Color(0xB9E5E5E5), foreground = Color.Black, size = 24.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF438A30), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_menu,
                        contentDescription = "More options",
                        tint = AgroGemIconColors.OnPrimary,
                        size = AgroGemIconSizes.Sm,
                    )
                }

                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .background(Color(0xB9E5E5E5), RoundedCornerShape(90.dp))
                        .padding(horizontal = 8.dp)
                        .clickable(onClick = onMicClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_mic,
                        contentDescription = "Mic",
                        tint = AgroGemIconColors.OnSurface,
                        size = AgroGemIconSizes.Xs,
                    )
                    Text(text = "Hablar", color = Color.Black, fontSize = 10.sp)
                }

                RoundIconButton(
                    label = "↑",
                    onClick = onSendClick,
                    background = Color(0xB9E5E5E5),
                    foreground = Color.Black,
                    size = 24.dp,
                )
            }
        }
    }
}

@Composable
private fun AttachmentMenu(
    onPhotosClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(190.dp)
            .background(FigmaColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(20.dp))
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onPhotosClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(FigmaColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_gallery,
                    contentDescription = "Gallery",
                    tint = AgroGemIconColors.OnPrimary,
                    size = AgroGemIconSizes.Xs,
                )
            }
            Text(text = "Fotos", color = Color.Black, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.clickable(onClick = onCameraClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(FigmaColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_camera,
                    contentDescription = "Camera",
                    tint = AgroGemIconColors.OnPrimary,
                    size = AgroGemIconSizes.Tiny,
                )
            }
            Text(text = "Cámara", color = Color.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ConfirmDialog(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(221.dp)
            .background(Color.White, RoundedCornerShape(25.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Esta conversación se guardará automaticamente al salir",
            color = Color.Black,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Si desea acceder a esta otra vez puede ir a historial de análisis, ingresa al análisis y luego presionar en ver conversación",
            color = Color(0xFF939393),
            fontSize = 8.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        FilledPrimaryButton(text = "Confirmar", onClick = onConfirm)
    }
}
