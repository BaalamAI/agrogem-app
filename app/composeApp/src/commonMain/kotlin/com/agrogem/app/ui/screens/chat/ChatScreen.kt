package com.agrogem.app.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.agrogem
import app.composeapp.generated.resources.ic_action_back
import app.composeapp.generated.resources.ic_action_camera
import app.composeapp.generated.resources.ic_action_chevron_down
import app.composeapp.generated.resources.ic_action_copy
import app.composeapp.generated.resources.ic_action_gallery
import app.composeapp.generated.resources.ic_action_sound
import app.composeapp.generated.resources.ic_status_check
import app.composeapp.generated.resources.logo_isotipo
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import com.agrogem.app.theme.AgroGemBrand
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import org.jetbrains.compose.resources.painterResource
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.DiagnosisInfoBox
import com.agrogem.app.ui.components.FilledPrimaryButton
import com.agrogem.app.ui.components.Pill
import com.agrogem.app.ui.components.RoundIconButton
import kotlinx.coroutines.launch
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.rememberMarkdownState

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
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemBrand.Background)
            .systemBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(label = "‹", icon = Res.drawable.ic_action_back, contentDescription = "Back", onClick = onBack, size = 32.dp)
                ModelSelector(
                    useThinking = uiState.useThinking,
                    onToggleThinking = { onEvent(ChatEvent.ToggleThinking(it)) },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(AgroGemBrand.Black, CircleShape)
                        .clickable { onEvent(ChatEvent.NewSession) },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.logo_isotipo),
                        contentDescription = "Nueva conversación",
                        modifier = Modifier.size(20.dp),
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
            val lastMessage = messages.lastOrNull()
            val showAssistantBrandMark = lastMessage != null
                && lastMessage.sender == MessageSender.Assistant
                && !lastMessage.isStreaming
                && lastMessage.text.isNotBlank()

            val listState = rememberLazyListState()
            val lastMessageId = lastMessage?.id
            val streamingTick = if (lastMessage?.isStreaming == true) lastMessage.text.length else 0
            val reversedMessages = remember(messages) { messages.asReversed() }
            // With reverseLayout, item index 0 is rendered at the visual bottom,
            // so animating to 0 keeps the newest content pinned above the composer.
            LaunchedEffect(messages.size, lastMessageId, streamingTick) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(0)
                }
            }
            if (messages.isEmpty()) {
                EmptyChatState(
                    onSuggestionClick = { suggestion -> onEvent(ChatEvent.InputChanged(suggestion)) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                val coroutineScope = rememberCoroutineScope()
                val notAtBottom by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0 ||
                            listState.firstVisibleItemScrollOffset > 80
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        if (showAssistantBrandMark) {
                            item("assistant_brand_mark") {
                                Image(
                                    painter = painterResource(Res.drawable.agrogem),
                                    contentDescription = "AgroGem",
                                    modifier = Modifier
                                        .padding(top = 20.dp, bottom = 16.dp)
                                        .height(28.dp),
                                )
                            }
                        }
                        items(
                            items = reversedMessages,
                            key = { it.id },
                            contentType = { it.sender },
                        ) { message ->
                            MessageBubble(
                                message = message,
                                speakingMessageId = uiState.speakingMessageId,
                                onPlayAssistantMessage = onEvent,
                            )
                        }
                    }
                    ScrollToBottomFab(
                        visible = notAtBottom,
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 8.dp, end = 12.dp),
                    )
                }
            }

            if (showComposer) {
                Column(
                    modifier = Modifier
                        .imePadding()
                        .padding(bottom = 16.dp),
                ) {
                    if (uiState.contextWarning != ContextWarningLevel.None) {
                        ContextWarningBanner(
                            level = uiState.contextWarning,
                            onNewSession = { onEvent(ChatEvent.NewSession) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    uiState.error?.let { errorMessage ->
                        ErrorBanner(
                            message = errorMessage,
                            onDismiss = { onEvent(ChatEvent.DismissError) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ChatInputArea(
                        inputText = uiState.inputText,
                        onInputChanged = { onEvent(ChatEvent.InputChanged(it)) },
                        onAttachClick = { onEvent(ChatEvent.ToggleAttachmentMenu(true)) },
                        onMicClick = onMicClick,
                        onSendClick = {
                            if (!uiState.isLoading) {
                                onEvent(ChatEvent.SendMessage)
                                focusManager.clearFocus()
                            }
                        },
                        pendingAttachments = pendingAttachments,
                        isLoading = uiState.isLoading,
                        onRemoveAttachment = { onEvent(ChatEvent.RemoveAttachment(it)) },
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
private fun ModelSelector(
    useThinking: Boolean,
    onToggleThinking: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Gemma 4",
                    color = AgroGemBrand.Text.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                AgroGemIcon(
                    icon = Res.drawable.ic_action_chevron_down,
                    contentDescription = "Abrir selector de modelo",
                    tint = AgroGemBrand.Gris400,
                    size = AgroGemIconSizes.Xs,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = (-28).dp, y = 4.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = AgroGemColors.Surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, AgroGemColors.ChatBorder),
                modifier = Modifier.width(136.dp),
            ) {
                Text(
                    text = "Modelo",
                    color = AgroGemBrand.Gris400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Gemma 4",
                            color = AgroGemBrand.Text.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    trailingIcon = {
                        AgroGemIcon(
                            icon = Res.drawable.ic_status_check,
                            contentDescription = "Modelo seleccionado",
                            tint = AgroGemBrand.Verde600,
                            size = AgroGemIconSizes.Xs,
                        )
                    },
                    onClick = { expanded = false },
                )
                HorizontalDivider(color = AgroGemColors.ChatBorder)
                Text(
                    text = "Modo",
                    color = AgroGemBrand.Gris400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Normal",
                            color = AgroGemBrand.Text.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    trailingIcon = {
                        if (!useThinking) {
                            AgroGemIcon(
                                icon = Res.drawable.ic_status_check,
                                contentDescription = "Modo activo",
                                tint = AgroGemBrand.Verde600,
                                size = AgroGemIconSizes.Xs,
                            )
                        }
                    },
                    onClick = {
                        onToggleThinking(false)
                        expanded = false
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Thinking",
                            color = AgroGemBrand.Text.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    trailingIcon = {
                        if (useThinking) {
                            AgroGemIcon(
                                icon = Res.drawable.ic_status_check,
                                contentDescription = "Modo activo",
                                tint = AgroGemBrand.Verde600,
                                size = AgroGemIconSizes.Xs,
                            )
                        }
                    },
                    onClick = {
                        onToggleThinking(true)
                        expanded = false
                    },
                )
            }
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
                color = AgroGemBrand.Text.Primary,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = remember {
        listOf(
            "¿Qué plagas afectan al maíz?",
            "Mi cultivo se ve amarillo",
            "Riego óptimo para café",
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(AgroGemBrand.Verde400.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo_isotipo),
                contentDescription = "AgroGem",
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¿En qué puedo ayudarte hoy?",
            color = AgroGemBrand.Text.Primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    text = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AgroGemColors.ChatAttachBg, RoundedCornerShape(20.dp))
            .border(1.dp, AgroGemColors.ChatBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = AgroGemBrand.Text.Primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Renders a single chat message bubble, styled according to sender.
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    speakingMessageId: String?,
    onPlayAssistantMessage: (ChatEvent) -> Unit,
) {
    when (message.sender) {
        MessageSender.Assistant -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (message.toolsUsed.isNotEmpty()) {
                    ToolsUsedIndicator(
                        tools = message.toolsUsed,
                        isLive = message.isStreaming,
                    )
                }
                if (message.thought != null && message.thought.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(AgroGemColors.PillTrackSemi, RoundedCornerShape(18.dp))
                            .padding(10.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Pensando...",
                                color = AgroGemBrand.Verde600,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = message.thought,
                                color = AgroGemBrand.Gris400,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }
                }
                if (message.isStreaming && message.text.isBlank()) {
                    ThinkingIndicator()
                } else if (message.isStreaming) {
                    StreamingTextWithCursor(text = message.text)
                } else {
                    AssistantMarkdownText(markdown = message.text)
                    if (message.text.isNotBlank()) {
                        AssistantMessageActions(
                            isSpeaking = speakingMessageId == message.id,
                            messageText = message.text,
                            onPlay = { onPlayAssistantMessage(ChatEvent.PlayAssistantMessage(message.id)) },
                        )
                    }
                }
            }
        }
        MessageSender.User -> {
            val imageAttachments = message.attachments.filterIsInstance<ChatAttachment.Image>()
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                imageAttachments.forEach { attachment ->
                    AsyncImage(
                        model = attachment.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .sizeIn(maxWidth = 220.dp, maxHeight = 200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(AgroGemBrand.Verde600, RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantMarkdownText(markdown: String) {
    val markdownState = rememberMarkdownState(
        content = markdown,
        retainState = true,
    )
    val bodyStyle = TextStyle(
        color = AgroGemBrand.Text.Primary,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    )
    val headingStyle = bodyStyle.copy(fontWeight = FontWeight.Bold)

    Markdown(
        markdownState = markdownState,
        modifier = Modifier.fillMaxWidth(),
        colors = markdownColor(
            text = AgroGemBrand.Text.Primary,
            codeBackground = AgroGemColors.PillTrackSemi,
            inlineCodeBackground = AgroGemColors.PillTrackSemi,
            dividerColor = AgroGemBrand.Gris400.copy(alpha = 0.35f),
            tableBackground = AgroGemColors.PillTrackSemi,
        ),
        padding = markdownPadding(
            block = 10.dp,
            list = 4.dp,
            listItemTop = 4.dp,
            listItemBottom = 4.dp,
            listIndent = 18.dp,
            codeBlock = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            blockQuote = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
            blockQuoteText = PaddingValues(vertical = 4.dp),
        ),
        typography = markdownTypography(
            h1 = headingStyle.copy(fontSize = 20.sp, lineHeight = 28.sp),
            h2 = headingStyle.copy(fontSize = 18.sp, lineHeight = 26.sp),
            h3 = headingStyle.copy(fontSize = 16.sp, lineHeight = 24.sp),
            h4 = headingStyle,
            h5 = headingStyle,
            h6 = headingStyle,
            text = bodyStyle,
            code = bodyStyle.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal),
            inlineCode = bodyStyle.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal),
            quote = bodyStyle.copy(fontStyle = FontStyle.Italic),
            paragraph = bodyStyle,
            ordered = bodyStyle,
            bullet = bodyStyle,
            list = bodyStyle,
            textLink = TextLinkStyles(
                style = bodyStyle.copy(
                    color = AgroGemBrand.Verde600,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ).toSpanStyle(),
            ),
            table = bodyStyle,
        ),
        loading = { modifier ->
            Text(
                text = markdown,
                color = AgroGemBrand.Text.Primary,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = modifier,
            )
        },
        error = { modifier ->
            Text(
                text = markdown,
                color = AgroGemBrand.Text.Primary,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = modifier,
            )
        },
    )
}

@Composable
private fun AssistantMessageActions(
    isSpeaking: Boolean,
    messageText: String,
    onPlay: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { clipboard.setText(AnnotatedString(messageText)) },
            contentAlignment = Alignment.Center,
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_action_copy,
                contentDescription = "Copiar respuesta",
                tint = AgroGemBrand.Gris400,
                size = AgroGemIconSizes.Sm,
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_action_sound,
                contentDescription = "Reproducir respuesta",
                tint = if (isSpeaking) AgroGemBrand.Verde400 else AgroGemBrand.Gris400,
                size = AgroGemIconSizes.Sm,
            )
        }
    }
}

@Composable
private fun ScrollToBottomFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AgroGemBrand.Black.copy(alpha = 0.85f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_action_chevron_down,
                contentDescription = "Ir al final",
                tint = Color.White,
                size = AgroGemIconSizes.Sm,
            )
        }
    }
}

@Composable
private fun StreamingTextWithCursor(text: String) {
    val cursorId = "cursor"
    val annotated = buildAnnotatedString {
        append(text)
        appendInlineContent(cursorId, "▍")
    }
    val inlineContent = mapOf(
        cursorId to InlineTextContent(
            placeholder = Placeholder(
                width = 9.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            ),
            children = { BlinkingCursor() },
        ),
    )
    BasicText(
        text = annotated,
        inlineContent = inlineContent,
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(
            color = AgroGemBrand.Text.Primary,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        ),
    )
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursor_alpha",
    )
    Box(
        modifier = Modifier
            .size(width = 7.dp, height = 16.dp)
            .background(
                AgroGemBrand.Verde600.copy(alpha = alpha),
                RoundedCornerShape(1.dp),
            ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolsUsedIndicator(tools: List<String>, isLive: Boolean) {
    val transition = rememberInfiniteTransition(label = "tools_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tools_pulse_alpha",
    )
    val label = if (isLive) "Consultando datos:" else "Datos consultados:"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemBrand.Gris700.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        Color.White.copy(alpha = pulseAlpha),
                        CircleShape,
                    ),
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tools.forEach { toolName ->
                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = toolName,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Pensando",
            color = AgroGemBrand.Gris400,
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
        )
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(AgroGemBrand.Verde400.copy(alpha = alpha), CircleShape),
            )
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
    onRemoveAttachment: (Int) -> Unit,
) {
    val hasPendingAttachments = pendingAttachments.isNotEmpty()
    val canSend = inputText.isNotBlank() || hasPendingAttachments

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasPendingAttachments) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pendingAttachments.forEachIndexed { index, attachment ->
                    if (attachment is ChatAttachment.Image) {
                        Box(modifier = Modifier.size(72.dp)) {
                            AsyncImage(
                                model = attachment.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .clickable { onRemoveAttachment(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "×",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(AgroGemColors.ChatAttachBg, CircleShape)
                .clickable(onClick = onAttachClick),
            contentAlignment = Alignment.Center,
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_action_camera,
                contentDescription = "Adjuntar",
                tint = AgroGemBrand.Text.Primary,
                size = AgroGemIconSizes.Sm,
            )
            if (hasPendingAttachments) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(AgroGemBrand.Verde400, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pendingAttachments.size.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .background(AgroGemColors.ChatAttachBg, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = AgroGemBrand.Text.Primary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                ),
                maxLines = 5,
                decorationBox = { innerTextField ->
                    if (inputText.isBlank()) {
                        Text(
                            text = "Pregunta lo que quieras",
                            color = AgroGemColors.ChatAttachHint,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                },
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (canSend) AgroGemBrand.Verde400 else AgroGemColors.ChatAttachBg,
                    CircleShape,
                )
                .clickable(enabled = !isLoading) {
                    if (canSend) onSendClick() else onMicClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (canSend) {
                Text(
                    text = if (isLoading) "…" else "↑",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                VoiceWaveBars(color = AgroGemBrand.Text.Primary)
            }
        }
    }
    }
}

@Composable
private fun VoiceWaveBars(color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).height(6.dp).background(color, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(3.dp).height(11.dp).background(color, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(3.dp).height(9.dp).background(color, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(3.dp).height(5.dp).background(color, RoundedCornerShape(2.dp)))
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
            .width(230.dp)
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, AgroGemColors.ChatBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPhotosClick)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AgroGemColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_gallery,
                    contentDescription = "Gallery",
                    tint = AgroGemColors.IconOnPrimary,
                    size = AgroGemIconSizes.Md,
                )
            }
            Text(text = "Fotos", color = AgroGemBrand.Text.Primary, fontSize = 15.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCameraClick)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AgroGemColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_camera,
                    contentDescription = "Camera",
                    tint = AgroGemColors.IconOnPrimary,
                    size = AgroGemIconSizes.Md,
                )
            }
            Text(text = "Cámara", color = AgroGemBrand.Text.Primary, fontSize = 15.sp)
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
            color = AgroGemBrand.Text.Primary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Si desea acceder a esta otra vez puede ir a historial de análisis, ingresa al análisis y luego presionar en ver conversación",
            color = AgroGemBrand.Gris400,
            fontSize = 8.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        FilledPrimaryButton(text = "Confirmar", onClick = onConfirm)
    }
}

@Composable
private fun ContextWarningBanner(
    level: ContextWarningLevel,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (bgColor, fgColor, message) = when (level) {
        ContextWarningLevel.Mild -> Triple(
            AgroGemBrand.Verde50,
            AgroGemBrand.Verde600,
            "El contexto va creciendo. Considera iniciar una nueva conversación si cambias de tema.",
        )
        ContextWarningLevel.Strong -> Triple(
            AgroGemColors.AlertSoft,
            AgroGemColors.Alert,
            "El contexto está casi lleno. Te recomendamos iniciar una nueva conversación pronto.",
        )
        ContextWarningLevel.Critical -> Triple(
            AgroGemColors.DangerSoft,
            AgroGemColors.Danger,
            "Contexto lleno. Las respuestas pueden perder precisión.",
        )
        ContextWarningLevel.None -> return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = fgColor,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (level == ContextWarningLevel.Critical) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AgroGemColors.Danger)
                    .clickable(onClick = onNewSession)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Nueva conv.",
                    color = AgroGemColors.IconOnPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
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
