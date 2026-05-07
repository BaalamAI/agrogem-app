package com.agrogem.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.logo_isotipo
import com.agrogem.app.data.GemmaPreparationStatus
import com.agrogem.app.theme.AgroGemBrand
import com.agrogem.app.theme.AgroGemColors
import org.jetbrains.compose.resources.painterResource

@Composable
fun GemmaPreparationStatusScreen(
    productName: String,
    status: GemmaPreparationStatus,
    progress: Int? = null,
    modifier: Modifier = Modifier,
) {
    val isDownloading = status == GemmaPreparationStatus.Downloading && progress != null
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDownloading) progress!!.coerceIn(0, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "progress",
    )

    Box(
        modifier = modifier.fillMaxSize().background(AgroGemBrand.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            // Logo con anillos pulsantes
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp),
            ) {
                PulsingRings()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(AgroGemBrand.Verde50, RoundedCornerShape(20.dp)),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.logo_isotipo),
                        contentDescription = "AgroGem",
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "AgroGem",
                color = AgroGemBrand.Gris900,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            // Badge "con Gemma 4"
            Box(
                modifier = Modifier
                    .background(AgroGemBrand.Verde50, RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "con $productName",
                    color = AgroGemBrand.Verde900,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(36.dp))

            // Barra de progreso redondeada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp)),
            ) {
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = AgroGemBrand.VerdeAgrogem,
                        trackColor = AgroGemBrand.Verde50,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = AgroGemBrand.VerdeAgrogem,
                        trackColor = AgroGemBrand.Verde50,
                    )
                }
            }

            if (isDownloading) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$progress%",
                    color = AgroGemBrand.Gris700,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = gemmaPreparationHeadline(status),
                color = AgroGemBrand.Gris900,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = gemmaPreparationSupportingText(status),
                color = AgroGemBrand.Gris700,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun PulsingRings() {
    val transition = rememberInfiniteTransition(label = "pulsingRings")
    val pulse1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2400, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "ring1",
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2400, delayMillis = 800, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "ring2",
    )
    val pulse3 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2400, delayMillis = 1600, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "ring3",
    )
    val ringColor = AgroGemBrand.VerdeAgrogem

    Canvas(modifier = Modifier.size(180.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val minRadius = maxRadius * 0.48f
        for (pulse in listOf(pulse1, pulse2, pulse3)) {
            val radius = minRadius + (maxRadius - minRadius) * pulse
            val alpha = 0.28f * (1f - pulse)
            drawCircle(
                color = ringColor.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
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
