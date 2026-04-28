package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.data.GemmaPreparationStatus
import com.agrogem.app.theme.AgroGemColors

@Composable
fun GemmaPreparationStatusScreen(
    productName: String,
    status: GemmaPreparationStatus,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = productName,
                color = AgroGemColors.Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(color = AgroGemColors.Primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = gemmaPreparationHeadline(status),
                color = AgroGemColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = gemmaPreparationSupportingText(status),
                color = AgroGemColors.TextChatHint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun GemmaPreparationHint(status: GemmaPreparationStatus, modifier: Modifier = Modifier) {
    val background = when (status) {
        GemmaPreparationStatus.Ready -> Color(0xFFE8F5E9)
        is GemmaPreparationStatus.Unavailable -> Color(0xFFFFF3E0)
        else -> Color(0xFFEFF3FF)
    }

    val textColor = when (status) {
        GemmaPreparationStatus.Ready -> Color(0xFF1B5E20)
        is GemmaPreparationStatus.Unavailable -> Color(0xFF7A4B00)
        else -> AgroGemColors.TextPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (status == GemmaPreparationStatus.NotPrepared || status == GemmaPreparationStatus.Downloading || status == GemmaPreparationStatus.Preparing) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = AgroGemColors.Primary,
            )
        }
        Text(
            text = gemmaPreparationHintText(status),
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

fun gemmaPreparationHeadline(status: GemmaPreparationStatus): String = when (status) {
    GemmaPreparationStatus.Downloading -> "Descargando modelo..."
    GemmaPreparationStatus.NotPrepared,
    GemmaPreparationStatus.Preparing,
    -> "Preparando modelo..."
    GemmaPreparationStatus.Ready -> "Modelo listo"
    is GemmaPreparationStatus.Unavailable -> "Modelo no disponible"
}

fun gemmaPreparationSupportingText(status: GemmaPreparationStatus): String = when (status) {
    GemmaPreparationStatus.Downloading -> "AgroGemma se está descargando en este dispositivo. Esto puede tardar varios minutos."
    GemmaPreparationStatus.NotPrepared,
    GemmaPreparationStatus.Preparing,
    -> "AgroGemma se está preparando para usarse localmente en este dispositivo."
    GemmaPreparationStatus.Ready -> "Todo listo para usar IA local en este dispositivo."
    is GemmaPreparationStatus.Unavailable -> "No se pudo preparar AgroGemma local en este momento."
}

fun gemmaPreparationHintText(status: GemmaPreparationStatus): String = when (status) {
    GemmaPreparationStatus.Downloading -> "Descargando modelo de AgroGemma en este dispositivo..."
    GemmaPreparationStatus.NotPrepared,
    GemmaPreparationStatus.Preparing,
    -> "Preparando AgroGemma en este dispositivo..."
    GemmaPreparationStatus.Ready -> "AgroGemma listo ✅"
    is GemmaPreparationStatus.Unavailable -> "AgroGemma no está disponible en este dispositivo. Seguimos en modo guiado."
}
