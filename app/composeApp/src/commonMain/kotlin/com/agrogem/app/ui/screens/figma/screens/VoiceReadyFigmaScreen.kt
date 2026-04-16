package com.agrogem.app.ui.screens.figma.screens

import androidx.compose.foundation.background
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
fun VoiceReadyFigmaScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFB8D8B8), FigmaColors.Screen),
                    radius = 980f,
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopStart)
                .offset(x = (-90).dp, y = 180.dp)
                .background(Color(0x552FA639), CircleShape),
        )

        Box(
            modifier = Modifier
                .size(230.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 92.dp, y = (-130).dp)
                .background(Color(0x552FA639), CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp, bottom = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(label = "‹", onClick = onBack)
                RoundIconButton(label = "≡", onClick = {}, foreground = Color(0xFF929292))
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "AgroGemma",
                color = FigmaColors.Text,
                fontSize = 36.sp / 1.75f,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(130.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF438A30), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⋮", color = Color.White, fontSize = 10.sp)
                }
                Text(text = "Ya puedes hablar", color = Color.Black, fontSize = 32.sp / 2.0f)
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(label = "···", onClick = {}, background = Color(0x80E0E0E0), foreground = Color(0xFF4A4A4A), size = 32.dp)
                RoundIconButton(label = "◉", onClick = onOpenChat, background = Color(0xFF5D6764), foreground = Color.White, size = 32.dp)
            }
        }
    }
}
