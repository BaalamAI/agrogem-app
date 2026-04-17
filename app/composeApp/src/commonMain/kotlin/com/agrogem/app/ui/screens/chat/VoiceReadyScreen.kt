package com.agrogem.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.figma.FigmaColors
import com.agrogem.app.ui.screens.figma.components.RoundIconButton

@Composable
fun VoiceReadyScreen(
    voiceState: VoiceState,
    onDismiss: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaColors.Screen),
    ) {
        VoiceReadyOrb(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-32).dp, y = 194.dp),
        )
        VoiceReadyOrb(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 277.dp, y = 558.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 23.dp, top = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(label = "‹", onClick = onDismiss)
            RoundIconButton(label = "≡", onClick = {}, foreground = Color(0xFF747474))
        }

        Text(
            text = "AgroGemma",
            color = FigmaColors.Text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-79).dp),
        )

        VoiceReadyHint(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 86.dp),
            voiceState = voiceState,
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 25.dp, end = 12.dp, bottom = 27.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFE5E5E5), CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⋯", color = Color(0xFF787878), fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xBB202020), CircleShape)
                    .clickable(onClick = onStopRecording),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "↑", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VoiceReadyHint(
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoundWaveIcon()
        when (voiceState) {
            is VoiceState.Listening -> {
                Text(
                    text = "Ya puedes hablar",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            VoiceState.Processing -> {
                Text(
                    text = "Procesando...",
                    color = FigmaColors.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            is VoiceState.Error -> {
                Text(
                    text = voiceState.message,
                    color = FigmaColors.Alert,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            VoiceState.Idle -> {
                Text(
                    text = "Listo",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SoundWaveIcon() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color(0xFF438A30), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(8.dp)
                    .background(Color.White, CircleShape),
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .background(Color.White, CircleShape),
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(8.dp)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

@Composable
private fun VoiceReadyOrb(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(162.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x29438A30),
                        Color(0x0D438A30),
                        Color.Transparent,
                    ),
                ),
                CircleShape,
            ),
    )
}
