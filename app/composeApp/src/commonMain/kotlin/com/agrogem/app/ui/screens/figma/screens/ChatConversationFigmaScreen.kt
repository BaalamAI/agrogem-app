package com.agrogem.app.ui.screens.figma.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.figma.FigmaColors
import com.agrogem.app.ui.screens.figma.components.FilledPrimaryButton
import com.agrogem.app.ui.screens.figma.components.Pill
import com.agrogem.app.ui.screens.figma.components.RoundIconButton

@Composable
fun ChatConversationFigmaScreen(
    onBack: () -> Unit,
    onRequestClose: () -> Unit,
    showConfirmDialog: Boolean,
    onConfirmClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
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
                RoundIconButton(label = "‹", onClick = onBack)
                RoundIconButton(label = "≡", onClick = {}, foreground = Color(0xFF929292))
                Text(
                    text = "Guardado automáticamente 11:58",
                    color = Color(0xFFABABAB),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(FigmaColors.SurfaceSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "◍", color = FigmaColors.Primary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DiagnosisHeaderCompact()
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(text = "✧", color = FigmaColors.Primary, fontSize = 24.sp)
                Text(
                    text = "Hola, veo que tienes un problema con tus cultivos. Se ha detectado una infección avanzada por Hemileia vastatrix. El 45% del follaje muestra pústulas activas. Se requiere intervención inmediata para evitar la pérdida total de la cosecha. ¿Tenías otra duda o algo en lo que pueda ayudarte?",
                    color = Color.Black,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ChatInputArea(onRequestClose = onRequestClose)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!showConfirmDialog) {
            AttachmentMenu(
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
private fun DiagnosisHeaderCompact() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Plaga detectada", color = Color.Black, fontSize = 32.sp / 1.75f, fontWeight = FontWeight.Medium)
            Pill(
                text = "Problema iniciando",
                background = FigmaColors.AlertSoft,
                foreground = FigmaColors.Alert,
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 8.sp,
            )
        }

        Pill(
            text = "95% de confianza",
            background = FigmaColors.ConfidenceBg,
            foreground = FigmaColors.ConfidenceText,
            icon = "◉",
            iconColor = FigmaColors.ConfidenceText,
            horizontal = 8.dp,
            vertical = 4.dp,
            textSize = 8.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DiagnosisInfoBox(label = "Área afectada", value = "Tallo y hoja", modifier = Modifier.weight(1f))
            DiagnosisInfoBox(label = "Causa", value = "Hongo, Hemileia vastatrix", italicTail = true, modifier = Modifier.weight(1f))
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

@Composable
private fun ChatInputArea(onRequestClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(FigmaColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFFE5E5E5), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "○", color = Color(0xFF7A7A7A), fontSize = 10.sp)
            }
            Text(text = "Preguntale algo sobre tus cultivos", color = Color(0xFFBDBDBD), fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(label = "+", onClick = {}, background = Color(0xB9E5E5E5), foreground = Color.Black, size = 24.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF438A30), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⋮", color = Color.White, fontSize = 10.sp)
                }

                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .background(Color(0xB9E5E5E5), RoundedCornerShape(90.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "◉", color = Color.Black, fontSize = 10.sp)
                    Text(text = "Hablar", color = Color.Black, fontSize = 10.sp)
                }

                RoundIconButton(
                    label = "↑",
                    onClick = onRequestClose,
                    background = Color(0xB9E5E5E5),
                    foreground = Color.Black,
                    size = 24.dp,
                )
            }
        }
    }
}

@Composable
private fun AttachmentMenu(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(190.dp)
            .background(FigmaColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(20.dp))
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(FigmaColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "◉", color = Color.White, fontSize = 10.sp)
            }
            Text(text = "Fotos", color = Color.Black, fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(FigmaColors.OverlayDark, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "◌", color = Color.White, fontSize = 10.sp)
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
            text = "Esta conversación se guardará automáticamente al salir",
            color = Color.Black,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Si desea acceder a esta otra vez puede ir a historial de análisis, ingresar al análisis y luego presionar en ver conversación.",
            color = Color(0xFF939393),
            fontSize = 8.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        FilledPrimaryButton(text = "Confirmar", onClick = onConfirm)
    }
}
