package com.agrogem.app.ui.screens.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.agrogem.app.ui.AppSessionViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.ic_action_magic
import com.agrogem.app.data.rememberLocationPermissionRequester
import com.agrogem.app.data.rememberNotificationPermissionRequester
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.FilledPrimaryButton
import com.agrogem.app.ui.components.RoundIconButton
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.MessageSender
import com.agrogem.app.ui.screens.onboarding.OnboardingChatStage

@Composable
fun OnboardingChatScreen(
    viewModel: OnboardingChatViewModel,
    appSessionViewModel: AppSessionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.onboardingChatStage ?: OnboardingChatStage.Conversation

    val locationRequester = rememberLocationPermissionRequester { _ ->
        viewModel.continueOnboardingAfterLocationPermission()
    }
    val notificationRequester = rememberNotificationPermissionRequester { granted ->
        viewModel.completeOnboarding(alertsEnabled = granted)
    }

    LaunchedEffect(Unit) {
        viewModel.startOnboardingChat()
    }

    val progress = uiState.onboardingProgress

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen),
    ) {
        when (stage) {
            OnboardingChatStage.Conversation -> ConversationScreen(
                messages = uiState.messages,
                inputText = uiState.inputText,
                onInputChanged = { viewModel.onInputChanged(it) },
                onBack = onBack,
                onSendMessage = { viewModel.sendOnboardingMessage(it) },
                progress = progress,
            )

            OnboardingChatStage.AwaitingLocationPermission -> ConversationScreen(
                messages = uiState.messages,
                inputText = uiState.inputText,
                onInputChanged = { viewModel.onInputChanged(it) },
                onBack = onBack,
                onSendMessage = { viewModel.sendOnboardingMessage(it) },
                progress = progress,
                showLocationModal = true,
                onAllowLocation = { locationRequester.request() },
                onRejectLocation = { viewModel.continueOnboardingAfterLocationPermission() },
            )

            OnboardingChatStage.AlertsPreferences -> LocationAndAlertsScreen(
                progress = progress,
                onSkipAlerts = { viewModel.skipOnboardingAlerts() },
                onActivateAlerts = { notificationRequester.request() },
            )

            OnboardingChatStage.Final -> FinalCompletionScreen(
                alertsEnabled = uiState.alertsEnabled,
                userName = uiState.userName,
                onStart = {
                    appSessionViewModel.completeOnboardingLocally(
                        name = uiState.userName,
                        crops = uiState.userCrops,
                        area = uiState.userArea,
                        stage = uiState.userStage,
                    )
                },
            )
        }
    }
}

@Composable
private fun ConversationScreen(
    messages: List<ChatMessage>,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    progress: Float,
    showLocationModal: Boolean = false,
    onAllowLocation: (() -> Unit)? = null,
    onRejectLocation: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "AgroGemma",
            color = AgroGemColors.Primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingProgressBar(progress = progress)

        Spacer(modifier = Modifier.height(30.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
        ) {
            messages.forEachIndexed { index, message ->
                MessageRow(message = message)
                if (index != messages.lastIndex) {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!showLocationModal) {
            OnboardingChatInput(
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = 16.dp),
                text = inputText,
                onTextChange = onInputChanged,
                onSend = onSendMessage,
            )
        }
    }

    if (showLocationModal) {
        LocationPermissionModal(
            onAllow = onAllowLocation ?: {},
            onReject = onRejectLocation ?: {},
        )
    }
}

@Composable
private fun LocationAndAlertsScreen(
    progress: Float,
    onSkipAlerts: () -> Unit,
    onActivateAlerts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "AgroGemma",
            color = AgroGemColors.Primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingProgressBar(progress = progress)

        Spacer(modifier = Modifier.height(30.dp))

        PromptRow(text = "¿Querés que te avise cuando haya alertas importantes para tus cultivos? 🔔")

        Spacer(modifier = Modifier.height(14.dp))

        AlertsPreferenceSection(
            onSkipAlerts = onSkipAlerts,
            onActivateAlerts = onActivateAlerts,
        )
    }
}

@Composable
private fun OnboardingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE8E8E8)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(AgroGemColors.Primary, RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun MessageRow(message: ChatMessage) {
    when (message.sender) {
        MessageSender.Assistant -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AgroGemIcon(
                    icon = Res.drawable.ic_action_magic,
                    contentDescription = "AgroGemma",
                    tint = AgroGemColors.Primary,
                    size = AgroGemIconSizes.Sm,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = message.text,
                    color = AgroGemColors.TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        MessageSender.User -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = message.text,
                        color = AgroGemColors.TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2F8E34),
                                    Color(0xFFABD557),
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionModal(
    onAllow: () -> Unit,
    onReject: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x12000000))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        LocationPermissionCard(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .width(316.dp),
            onReject = onReject,
            onAllow = onAllow,
        )
    }
}

@Composable
private fun LocationPermissionCard(
    modifier: Modifier = Modifier,
    onAllow: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "AgroGemma quiere acceder a tu ubicación",
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )

        Text(
            text = "Se usará para mostrarte información climática y alertas de tu zona",
            color = Color(0xFF878787),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )

        if (onAllow != null || onReject != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                SmallPermissionButton(
                    text = "No permitir",
                    filled = false,
                    onClick = onReject ?: {},
                    modifier = Modifier.width(102.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                SmallPermissionButton(
                    text = "Permitir",
                    filled = true,
                    onClick = onAllow ?: {},
                    modifier = Modifier.width(102.dp),
                )
            }
        }
    }
}

@Composable
private fun PromptRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AgroGemIcon(
            icon = Res.drawable.ic_action_magic,
            contentDescription = "AgroGemma",
            tint = AgroGemColors.Primary,
            size = AgroGemIconSizes.Sm,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = text,
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun AlertsPreferenceSection(
    onSkipAlerts: () -> Unit,
    onActivateAlerts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlertExampleRow(
                icon = "🌧️",
                text = "Ejemplo: \"Se esperan lluvias fuertes mañana en tu zona. Revisá el drenaje de tu tomate.\"",
            )
            AlertExampleRow(
                icon = "🐛",
                text = "Ejemplo: \"Alerta de mosca blanca reportada en tu zona. Chequeá tus plantas.\"",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            SmallPermissionButton(
                text = "Ahora no",
                filled = false,
                onClick = onSkipAlerts,
                modifier = Modifier.width(102.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            SmallPermissionButton(
                text = "Sí, Activar",
                filled = true,
                onClick = onActivateAlerts,
                modifier = Modifier.width(102.dp),
            )
        }
    }
}

@Composable
private fun AlertExampleRow(
    icon: String,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFEAF5EA), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, fontSize = 16.sp)
        }

        Text(
            text = text,
            color = Color(0xFF686868),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SmallPermissionButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(27.dp)
            .background(
                color = if (filled) AgroGemColors.PrimaryButton else Color.White,
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = if (filled) 1.dp else 1.dp,
                color = AgroGemColors.PrimaryButtonBorder,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) Color.White else AgroGemColors.Primary,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OnboardingChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = AgroGemColors.TextPrimary,
                fontSize = 12.sp,
            ),
            maxLines = 3,
            decorationBox = { innerTextField ->
                if (text.isBlank()) {
                    Text(
                        text = "Preguntale algo sobre tus cultivos",
                        color = AgroGemColors.ChatAttachHint,
                        fontSize = 12.sp,
                    )
                }
                innerTextField()
            },
        )

        RoundIconButton(
            label = "↑",
            icon = Res.drawable.ic_action_magic,
            contentDescription = "Send",
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                }
            },
            background = AgroGemColors.PillTrackSemi,
            foreground = AgroGemColors.TextPrimary,
            size = 24.dp,
        )
    }
}

@Composable
private fun FinalCompletionScreen(
    alertsEnabled: Boolean,
    userName: String?,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "AgroGemma",
            color = AgroGemColors.Primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingProgressBar(progress = 1f)

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_action_magic,
                contentDescription = "AgroGemma",
                tint = AgroGemColors.Primary,
                size = AgroGemIconSizes.Sm,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "¡Todo listo! 🎉",
                color = AgroGemColors.TextPrimary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (alertsEnabled) {
                "Ya podés empezar a usar AgroGemma para cuidar tus cultivos. Te vamos a avisar sobre clima, plagas y alertas importantes de tu zona.\n\n¿Querés que empecemos?"
            } else {
                "Ya podés empezar a usar AgroGemma para cuidar tus cultivos. Por ahora no vamos a enviarte notificaciones, pero vas a poder activar las alertas importantes de tu zona más adelante cuando quieras.\n\n¿Querés que empecemos?"
            },
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.width(173.dp)) {
            FilledPrimaryButton(
                text = "Empezar",
                onClick = onStart,
                enabled = true,
            )
        }

        Spacer(modifier = Modifier.height(220.dp))
    }
}
