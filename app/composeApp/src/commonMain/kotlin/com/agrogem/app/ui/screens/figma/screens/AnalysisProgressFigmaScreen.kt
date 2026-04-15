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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.ui.screens.figma.FigmaColors
import com.agrogem.app.ui.screens.figma.components.DragHandle
import com.agrogem.app.ui.screens.figma.components.DraggableSlice
import com.agrogem.app.ui.screens.figma.components.FilledPrimaryButton
import com.agrogem.app.ui.screens.figma.components.OutlinedPrimaryButton
import com.agrogem.app.ui.screens.figma.components.PlantBackdrop
import com.agrogem.app.ui.screens.figma.components.PrimaryActionHint
import com.agrogem.app.ui.screens.figma.components.DashedTarget

@Composable
fun AnalysisProgressFigmaScreen(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaColors.Screen),
    ) {
        PlantBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp),
            alpha = 0.96f,
        )

        DashedTarget(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 128.dp),
        )

        PrimaryActionHint(
            text = "ANALIZANDO CULTIVO CON IA...",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 375.dp),
        )

        DraggableSlice(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(FigmaColors.Surface, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            collapsedOffset = 240.dp,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DragHandle()
            StatusPanel()
            OutlinedPrimaryButton(text = "Cancelar Análisis", onClick = onCancel)
            FilledPrimaryButton(text = "Continuar", onClick = onContinue)
        }
    }
}

@Composable
private fun StatusPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.SurfaceMuted, RoundedCornerShape(48.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatusRow(
            iconBackground = Color(0xFF2E7D32),
            title = "Identificando patrones de hojas...",
            subtitle = "Analizando irregularidades celulares",
            titleColor = FigmaColors.Primary,
            alpha = 1f,
        )
        StatusRow(
            iconBackground = Color(0xFFDDE2DF),
            title = "Consultando base de datos de plagas...",
            subtitle = "Sincronizando con AgroCloud Index",
            alpha = 0.65f,
        )
        StatusRow(
            iconBackground = Color(0xFFDDE2DF),
            title = "Calculando severidad...",
            subtitle = "Estimación de impacto en cosecha",
            alpha = 0.42f,
        )
        StatusRow(
            iconBackground = Color(0xFFDDE2DF),
            title = "Calculando severidad...",
            subtitle = "Estimación de impacto en cosecha",
            alpha = 0.32f,
        )
    }
}

@Composable
private fun StatusRow(
    iconBackground: Color,
    title: String,
    subtitle: String,
    titleColor: Color = Color(0xFF181D1A),
    alpha: Float,
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
            Text(text = "◌", color = Color.White, fontSize = 13.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = titleColor, fontSize = 14.sp)
            Text(text = subtitle, color = FigmaColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
