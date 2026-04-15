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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.figma.FigmaColors
import com.agrogem.app.ui.screens.figma.components.DotsIndicator
import com.agrogem.app.ui.screens.figma.components.LeafThumb
import com.agrogem.app.ui.screens.figma.components.PlantBackdrop
import com.agrogem.app.ui.screens.figma.components.PrimaryActionHint
import com.agrogem.app.ui.screens.figma.components.RoundIconButton

@Composable
fun CameraCaptureFigmaScreen(
    onClose: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(FigmaColors.CameraDarkTop, FigmaColors.CameraDarkBottom),
                ),
            ),
    ) {
        PlantBackdrop(Modifier.fillMaxSize(), alpha = 0.95f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            RoundIconButton(label = "✕", onClick = onClose, background = Color(0xBB202020), foreground = Color.White)
            Spacer(modifier = Modifier.weight(1f))

            DotsIndicator(4)
            Spacer(modifier = Modifier.height(14.dp))
            CameraThumbRow()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(label = "🖼", onClick = {}, background = Color(0xBB202020), foreground = Color.White)
                ShutterButton(onAnalyze)
                RoundIconButton(label = "↻", onClick = {}, background = Color(0xBB202020), foreground = Color.White)
            }

            Spacer(modifier = Modifier.height(14.dp))
            PrimaryActionHint(
                text = "TOMAR FOTO PARA ANALIZAR CON IA",
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CameraThumbRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { index ->
            LeafThumb(seed = index, rounded = 10.dp, size = 96.dp)
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(Color.White.copy(alpha = 0.25f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .padding(8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, CircleShape)
                .border(2.dp, FigmaColors.Primary.copy(alpha = 0.2f), CircleShape),
        )
    }
}
