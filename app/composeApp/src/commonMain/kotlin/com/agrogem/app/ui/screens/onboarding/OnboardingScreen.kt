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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.ic_action_add
import app.composeapp.generated.resources.ic_action_magic
import app.composeapp.generated.resources.ic_action_mic
import app.composeapp.generated.resources.ic_action_speak
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.theme.AgroGemIconSizes
import com.agrogem.app.ui.components.AgroGemIcon
import com.agrogem.app.ui.components.FilledPrimaryButton
import com.agrogem.app.ui.components.RoundIconButton

private const val OnboardingStepCount = 10

@Composable
fun OnboardingScreen(
    step: Int,
    onFinish: () -> Unit,
    onWelcomeAdvance: () -> Unit,
    onWriteWithAgroGemma: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onboardingStep = OnboardingUiStep.fromIndex(step)
    var welcomeConsumed by remember(step) { mutableStateOf(false) }

    if (onboardingStep == OnboardingUiStep.Welcome && !welcomeConsumed) {
        LaunchedEffect(Unit) {
            delay(2200)
            if (!welcomeConsumed) {
                welcomeConsumed = true
                onWelcomeAdvance()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen),
    ) {
        when (onboardingStep) {
            OnboardingUiStep.Welcome -> WelcomeScreen(
                onAdvance = {
                    if (!welcomeConsumed) {
                        welcomeConsumed = true
                        onWelcomeAdvance()
                    }
                },
            )
            OnboardingUiStep.Choice -> ChoiceScreen(
                onWriteWithAgroGemma = onWriteWithAgroGemma,
                onSpeakWithAgroGemma = {},
            )
            OnboardingUiStep.AskName -> AskNameScreen()
            OnboardingUiStep.NameAnswered -> NameAnsweredScreen()
            OnboardingUiStep.CropsAnswered -> CropsAnsweredScreen()
            OnboardingUiStep.StageAnswered -> StageAnsweredScreen()
            OnboardingUiStep.LocationIntro -> LocationIntroScreen()
            OnboardingUiStep.LocationPermission -> LocationPermissionScreen()
            OnboardingUiStep.AlertsPermission -> AlertsPermissionScreen(onFinish = onFinish)
            OnboardingUiStep.Final -> FinalScreen(onFinish = onFinish)
        }
    }
}

private enum class OnboardingUiStep {
    Welcome,
    Choice,
    AskName,
    NameAnswered,
    CropsAnswered,
    StageAnswered,
    LocationIntro,
    LocationPermission,
    AlertsPermission,
    Final,
    ;

    companion object {
        fun fromIndex(index: Int): OnboardingUiStep =
            values().getOrNull(index.coerceIn(0, OnboardingStepCount - 1)) ?: Welcome
    }
}

@Composable
private fun WelcomeScreen(onAdvance: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        OnboardingCloudBackdrop()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Bienvenido",
                color = Color.Transparent,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AgroGemColors.Primary,
                            Color(0xFFABD557),
                        ),
                    ),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.64).sp,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Antes de empezar hablemos...",
                color = Color(0xFFB0B0B0),
                fontSize = 10.sp,
                letterSpacing = (-0.2).sp,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onAdvance),
        )
    }
}

@Composable
private fun ChoiceScreen(
    onWriteWithAgroGemma: () -> Unit,
    onSpeakWithAgroGemma: () -> Unit,
) {
    OnboardingConversationFrame(
        progress = 0f,
        content = {
            BotPrompt(
                text = "👋 ¡Hola! Soy AgroGemma, tu asistente agrícola.\n\nVoy a ayudarte a cuidar tus cultivos con consejos personalizados para tu zona y tu etapa de siembra. ¡Empecemos!\n\n¿Qué método preferís para capturar tus datos iniciales?",
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChoiceCard(
                    icon = Res.drawable.ic_action_mic,
                    title = "Hablar con\nAgroGemma",
                    onClick = onSpeakWithAgroGemma,
                    modifier = Modifier.width(155.dp),
                )
                ChoiceCard(
                    icon = Res.drawable.ic_action_speak,
                    title = "Escribir con\nAgroGemma",
                    onClick = onWriteWithAgroGemma,
                    modifier = Modifier.width(155.dp),
                )
            }
        },
    )
}

@Composable
private fun AskNameScreen() {
    OnboardingConversationFrame(
        progress = 2f / 9f,
        content = {
            BotPrompt(text = "Para empezar, ¿cómo te llamás? 😊")

            Spacer(modifier = Modifier.height(260.dp))

            ConversationComposer(
                placeholder = "Click acá para responderle a AgroGemma",
            )
        },
    )
}

@Composable
private fun NameAnsweredScreen() {
    OnboardingConversationFrame(
        progress = 3f / 9f,
        content = {
            BotPrompt(text = "Para empezar, ¿cómo te llamás? 😊")

            Spacer(modifier = Modifier.height(20.dp))

            UserBubble(text = "Me llamo Alejandro")

            Spacer(modifier = Modifier.height(20.dp))

            BotPrompt(text = "Mucho gusto, Alejandro. ¿Qué cultivo o cultivos tenés? Podés mencionar más de uno. 🌱")

            Spacer(modifier = Modifier.height(180.dp))

            ConversationComposer(
                placeholder = "Click acá para responderle a AgroGemma",
            )
        },
    )
}

@Composable
private fun CropsAnsweredScreen() {
    OnboardingConversationFrame(
        progress = 4f / 9f,
        content = {
            BotPrompt(text = "Mucho gusto, Alejandro. ¿Qué cultivo o cultivos tenés? Podés mencionar más de uno. 🌱")

            Spacer(modifier = Modifier.height(18.dp))

            UserBubble(text = "Tengo aguacate, tomate y arroz")

            Spacer(modifier = Modifier.height(18.dp))

            BotPrompt(
                text = "¿Cuántas manzanas o hectáreas tiene tu cultivo de aguacate, tomate y arroz? Si puedes dame las dimensiones por separado o sea cuanto mide cada cultivo mejor.",
            )

            Spacer(modifier = Modifier.height(160.dp))

            ConversationComposer(
                placeholder = "Click acá para responderle a AgroGemma",
            )
        },
    )
}

@Composable
private fun StageAnsweredScreen() {
    OnboardingConversationFrame(
        progress = 5f / 9f,
        content = {
            BotPrompt(
                text = "¿Cuánto tiempo tiene que lo sembraste? ¿En qué etapa está?\n\n1. Alistando la tierra — preparación y siembra\n2. Saliendo el puyón — nacencia\n3. Poniéndose shule — crecimiento\n4. Dando la flor — floración\n5. Cargando el fruto — llenado\n6. Punto de corte — cosecha",
            )

            Spacer(modifier = Modifier.height(18.dp))

            UserBubble(text = "Esta en crecimiento, ya tiene como 4 semanas")

            Spacer(modifier = Modifier.height(145.dp))

            ConversationComposer(
                placeholder = "Click acá para responderle a AgroGemma",
            )
        },
    )
}

@Composable
private fun LocationIntroScreen() {
    OnboardingConversationFrame(
        progress = 6f / 9f,
        content = {
            BotPrompt(
                text = "Para darte consejos más exactos necesito saber dónde están tus cultivos. Así puedo ver la temperatura, lluvias y condiciones de tu zona. 📍",
            )

            Spacer(modifier = Modifier.height(24.dp))

            LocationPermissionCard(
                title = "AgroGemma quiere acceder a tu ubicación",
                body = "Se usará para mostrarte información climática y alertas de tu zona",
            )
        },
    )
}

@Composable
private fun LocationPermissionScreen() {
    OnboardingConversationFrame(
        progress = 7f / 9f,
        content = {
            BotPrompt(
                text = "Para darte consejos más exactos necesito saber dónde están tus cultivos. Así puedo ver la temperatura, lluvias y condiciones de tu zona. 📍",
            )

            Spacer(modifier = Modifier.height(24.dp))

            LocationPermissionCard(
                title = "AgroGemma quiere acceder a tu ubicación",
                body = "Se usará para mostrarte información climática y alertas de tu zona",
            )

            Spacer(modifier = Modifier.height(20.dp))

            BotPrompt(text = "¿Querés que te avise cuando haya alertas importantes para tus cultivos? 🔔")

            Spacer(modifier = Modifier.height(14.dp))

            AlertsExamplesCard()
        },
    )
}

@Composable
private fun AlertsPermissionScreen(onFinish: () -> Unit) {
    OnboardingConversationFrame(
        progress = 8f / 9f,
        content = {
            BotPrompt(text = "¿Querés que te avise cuando haya alertas importantes para tus cultivos? 🔔")

            Spacer(modifier = Modifier.height(14.dp))

            AlertsExamplesCard()

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SmallActionButton(
                    text = "Ahora no",
                    filled = false,
                    onClick = onFinish,
                    modifier = Modifier.width(155.dp),
                )
                SmallActionButton(
                    text = "Sí, Activar",
                    filled = true,
                    onClick = onFinish,
                    modifier = Modifier.width(155.dp),
                )
            }
        },
    )
}

@Composable
private fun FinalScreen(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(92.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            AgroGemIcon(
                icon = Res.drawable.ic_action_magic,
                contentDescription = "AgroGemma",
                tint = AgroGemColors.Primary,
                size = AgroGemIconSizes.Sm,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "¡Todo listo! 🎉",
                color = AgroGemColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Ya podés empezar a usar AgroGemma para cuidar tus cultivos. Vamos a monitorear el clima, las plagas y las alertas de tu zona.\n\n¿Querés que empecemos?",
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp),
        )

        Spacer(modifier = Modifier.height(120.dp))

        Box(modifier = Modifier.width(170.dp)) {
            FilledPrimaryButton(
                text = "Empezar",
                onClick = onFinish,
            )
        }

        Spacer(modifier = Modifier.height(52.dp))
    }
}

@Composable
private fun OnboardingConversationFrame(
    progress: Float,
    content: @Composable () -> Unit,
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

        ProgressBar(progress = progress)

        Spacer(modifier = Modifier.height(30.dp))

        content()
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE4E4E4)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(AgroGemColors.Primary, RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun BotPrompt(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AgroGemIcon(
            icon = Res.drawable.ic_action_magic,
            contentDescription = "AgroGemma",
            tint = AgroGemColors.Primary,
            size = AgroGemIconSizes.Sm,
            modifier = Modifier.offset(y = 2.dp),
        )

        Text(
            text = text,
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChoiceCard(
    icon: org.jetbrains.compose.resources.DrawableResource,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(98.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AgroGemColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AgroGemIcon(
                icon = icon,
                contentDescription = title,
                tint = Color.White,
                size = AgroGemIconSizes.Xs,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 212.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = text,
                color = AgroGemColors.TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0D631B),
                            Color(0xFFABD557),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun ConversationComposer(
    placeholder: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 130.dp)
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7DCE46),
                                Color(0xFF0D631B),
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )

            Text(
                text = placeholder,
                color = AgroGemColors.TextHint,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                label = "+",
                icon = Res.drawable.ic_action_add,
                contentDescription = "Agregar adjunto",
                onClick = {},
                background = AgroGemColors.ChatAttachBg,
                foreground = AgroGemColors.TextPrimary,
                size = 24.dp,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(AgroGemColors.PrimaryAction, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_mic,
                        contentDescription = "Hablar",
                        tint = Color.White,
                        size = AgroGemIconSizes.Xs,
                    )
                }

                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .background(AgroGemColors.ChatAttachBg, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp)
                        .clickable(onClick = {}),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AgroGemIcon(
                        icon = Res.drawable.ic_action_mic,
                        contentDescription = "Hablar",
                        tint = AgroGemColors.IconOnSurface,
                        size = AgroGemIconSizes.Xs,
                    )
                    Text(
                        text = "Hablar",
                        color = AgroGemColors.TextPrimary,
                        fontSize = 10.sp,
                    )
                }

                RoundIconButton(
                    label = "↑",
                    onClick = {},
                    background = AgroGemColors.ChatAttachBg,
                    foreground = AgroGemColors.TextPrimary,
                    size = 24.dp,
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 34.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = title,
            color = AgroGemColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )

        Text(
            text = body,
            color = AgroGemColors.TextHint,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SmallActionButton(
                text = "No permitir",
                filled = false,
                onClick = {},
                modifier = Modifier.width(155.dp),
            )
            SmallActionButton(
                text = "Permitir",
                filled = true,
                onClick = {},
                modifier = Modifier.width(155.dp),
            )
        }
    }
}

@Composable
private fun AlertsExamplesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, AgroGemColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ExampleAlertRow(
            icon = "🌧️",
            text = "Ejemplo: \"Se esperan lluvias fuertes mañana en tu zona. Revisá el drenaje de tu tomate.\"",
        )
        ExampleAlertRow(
            icon = "🐛",
            text = "Ejemplo: \"Alerta de mosca blanca reportada en Retalhuleu. Chequeá tus plantas.\"",
        )
    }
}

@Composable
private fun ExampleAlertRow(
    icon: String,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, AgroGemColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFEAF5EA), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                fontSize = 16.sp,
            )
        }

        Text(
            text = text,
            color = AgroGemColors.TextHint,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SmallActionButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .background(
                color = if (filled) AgroGemColors.Primary else Color.White,
                shape = RoundedCornerShape(999.dp),
            )
            .border(
                width = if (filled) 0.dp else 1.dp,
                color = AgroGemColors.Primary,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) Color.White else AgroGemColors.Primary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OnboardingCloudBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        GlowCircle(
            modifier = Modifier
                .offset(x = (-60).dp, y = 86.dp)
                .size(220.dp),
            color = Color(0xFFABD557).copy(alpha = 0.34f),
        )
        GlowCircle(
            modifier = Modifier
                .offset(x = 256.dp, y = 488.dp)
                .size(220.dp),
            color = Color(0xFFABD557).copy(alpha = 0.30f),
        )
        GlowCircle(
            modifier = Modifier
                .offset(x = 178.dp, y = 164.dp)
                .size(180.dp),
            color = Color(0xFFDFF6D7).copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun GlowCircle(
    modifier: Modifier,
    color: Color,
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color,
                        Color.Transparent,
                    ),
                ),
                shape = CircleShape,
            ),
    )
}
