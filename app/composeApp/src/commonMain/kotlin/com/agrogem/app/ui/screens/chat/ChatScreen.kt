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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
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
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.DiagnosisInfoBox
import com.agrogem.app.ui.components.FilledPrimaryButton
import com.agrogem.app.ui.components.Pill
import com.agrogem.app.ui.components.RoundIconButton

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onRequestClose: () -> Unit,
    onMicClick: () -> Unit,
    onLaunchCamera: () -> Unit = {},
    onLaunchGallery: () -> Unit = {},
    showConfirmDialog: Boolean,
    showComposer: Boolean = true,
    onConfirmClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    ChatContent(
        uiState = uiState,
        onEvent = { viewModel.onEvent(it) },
        onBack = onBack,
        onRequestClose = onRequestClose,
        onMicClick = onMicClick,
        onLaunchCamera = onLaunchCamera,
        onLaunchGallery = onLaunchGallery,
        showConfirmDialog = showConfirmDialog,
        showComposer = showComposer,
        onConfirmClose = onConfirmClose,
        modifier = modifier
    )
}

@Composable
fun ChatContent(
    uiState: ChatUiState,
    onEvent: (ChatEvent) -> Unit,
    onBack: () -> Unit,
    onRequestClose: () -> Unit,
    onMicClick: () -> Unit,
    onLaunchCamera: () -> Unit = {},
    onLaunchGallery: () -> Unit = {},
    showConfirmDialog: Boolean,
    showComposer: Boolean = true,
    onConfirmClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pendingAttachments = uiState.attachments
    val chatMode = uiState.mode
    val messages = uiState.messages

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen),
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
                RoundIconButton(label = "≡", icon = Res.drawable.ic_action_menu, contentDescription = "Menu", onClick = onRequestClose, foreground = AgroGemColors.IconMenuTint)
                Text(
                    text = "Guardado automáticamente 11:58",
                    color = AgroGemColors.TextChatTimestamp,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (uiState.useThinking) AgroGemColors.Primary else AgroGemColors.ChatAttachBg, 
                            CircleShape
                        )
                        .clickable { onEvent(ChatEvent.ToggleThinking(!uiState.useThinking)) },
                    contentAlignment = Alignment.Center,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_magic,
                        contentDescription = "Toggle Thinking",
                        tint = if (uiState.useThinking) AgroGemColors.IconOnPrimary else AgroGemColors.ChatAttachText,
                        size = AgroGemIconSizes.Xs,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(AgroGemColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_sound,
                        contentDescription = "Sound",
                        tint = AgroGemColors.IconOnPrimary,
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }

            if (showComposer) {
                Column(
                    modifier = Modifier
                        .imePadding()
                        .padding(bottom = 16.dp),
                ) {
                    uiState.error?.let { errorMessage ->
                        ErrorBanner(
                            message = errorMessage,
                            onDismiss = { viewModel.onEvent(ChatEvent.DismissError) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ChatInputArea(
                        inputText = uiState.inputText,
                        onInputChanged = { onEvent(ChatEvent.InputChanged(it)) },
                        onAttachClick = { onEvent(ChatEvent.ToggleAttachmentMenu(true)) },
                        onMicClick = onMicClick,
                        onSendClick = { if (!uiState.isLoading) viewModel.onEvent(ChatEvent.SendMessage) },
                        pendingAttachments = pendingAttachments,
                        isLoading = uiState.isLoading,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (!showConfirmDialog && uiState.showAttachmentMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onEvent(ChatEvent.ToggleAttachmentMenu(false)) },
                    ),
            )

            AttachmentMenu(
                onPhotosClick = {
                    onEvent(ChatEvent.RequestGallery)
                    onLaunchGallery()
                },
                onCameraClick = {
                    onEvent(ChatEvent.RequestCamera)
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
                    .background(AgroGemColors.OverlayDim),
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
                color = AgroGemColors.TextPrimary,
                fontSize = 32.sp / 1.75f,
                fontWeight = FontWeight.Medium,
            )
            Pill(
                text = diagnosis.severity,
                background = AgroGemColors.AlertSoft,
                foreground = AgroGemColors.Alert,
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 8.sp,
            )
        }

        Row(
            modifier = Modifier
                .background(AgroGemColors.ConfidenceBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                text = "${(diagnosis.confidence * 100).toInt()}% de confianza",
                color = AgroGemColors.ConfidenceText,
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
                    tint = AgroGemColors.Primary,
                    size = AgroGemIconSizes.Md,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (message.thought != null && message.thought.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(AgroGemColors.PillTrackSemi, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Pensando...",
                                    color = AgroGemColors.Primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = message.thought,
                                    color = AgroGemColors.TextChatHint,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                        }
                    }
                    Text(
                        text = message.text,
                        color = AgroGemColors.TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
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
                        .background(AgroGemColors.Primary, RoundedCornerShape(12.dp))
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
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val hasPendingAttachments = pendingAttachments.isNotEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (hasPendingAttachments) AgroGemColors.Primary else AgroGemColors.ChatAttachBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasPendingAttachments) "${pendingAttachments.size}" else "○",
                    color = if (hasPendingAttachments) Color.White else AgroGemColors.ChatAttachText,
                    fontSize = 10.sp,
                )
            }

            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = AgroGemColors.TextPrimary,
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
                            color = if (hasPendingAttachments) AgroGemColors.Primary else AgroGemColors.ChatAttachHint,
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
            RoundIconButton(label = "+", icon = Res.drawable.ic_action_add, contentDescription = "Add attachment", onClick = onAttachClick, background = AgroGemColors.PillTrackSemi, foreground = AgroGemColors.TextPrimary, size = 24.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(AgroGemColors.PrimaryAction, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_menu,
                        contentDescription = "More options",
                        tint = AgroGemColors.IconOnPrimary,
                        size = AgroGemIconSizes.Sm,
                    )
                }

                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .background(AgroGemColors.PillTrackSemi, RoundedCornerShape(90.dp))
                        .padding(horizontal = 8.dp)
                        .clickable(onClick = onMicClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_mic,
                        contentDescription = "Mic",
                        tint = AgroGemColors.IconOnSurface,
                        size = AgroGemIconSizes.Xs,
                    )
                    Text(text = "Hablar", color = AgroGemColors.TextPrimary, fontSize = 10.sp)
                }

                RoundIconButton(
                    label = if (isLoading) "○" else "↑",
                    onClick = onSendClick,
                    background = if (isLoading) AgroGemColors.PillTrackSemi.copy(alpha = 0.5f) else AgroGemColors.PillTrackSemi,
                    foreground = if (isLoading) AgroGemColors.TextPrimary.copy(alpha = 0.5f) else AgroGemColors.TextPrimary,
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
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, AgroGemColors.ChatBorder, RoundedCornerShape(20.dp))
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
                    .background(AgroGemColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_gallery,
                    contentDescription = "Gallery",
                    tint = AgroGemColors.IconOnPrimary,
                    size = AgroGemIconSizes.Xs,
                )
            }
            Text(text = "Fotos", color = AgroGemColors.TextPrimary, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.clickable(onClick = onCameraClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(AgroGemColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_camera,
                    contentDescription = "Camera",
                    tint = AgroGemColors.IconOnPrimary,
                    size = AgroGemIconSizes.Tiny,
                )
            }
            Text(text = "Cámara", color = AgroGemColors.TextPrimary, fontSize = 12.sp)
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
            .background(AgroGemColors.Surface, RoundedCornerShape(25.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Esta conversación se guardará automaticamente al salir",
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Si desea acceder a esta otra vez puede ir a historial de análisis, ingresa al análisis y luego presionar en ver conversación",
            color = AgroGemColors.TextChatHint,
            fontSize = 8.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        FilledPrimaryButton(text = "Confirmar", onClick = onConfirm)
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AgroGemColors.AlertSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = AgroGemColors.Alert,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "✕",
            color = AgroGemColors.Alert,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onDismiss),
        )
    }
}
