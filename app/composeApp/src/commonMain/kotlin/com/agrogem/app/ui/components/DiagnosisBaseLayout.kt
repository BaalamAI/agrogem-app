package com.agrogem.app.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.ui.preview.ProductItem
import com.agrogem.app.ui.preview.products

@Composable
internal fun DiagnosisBaseLayout(
    modifier: Modifier,
    topInset: Dp,
    sliceCollapsedOffset: Dp,
    showCaptureOverlay: Boolean,
    showProducts: Boolean,
    primaryButtonText: String,
    secondaryButtonText: String? = null,
    showVoiceBadge: Boolean = false,
    showLinkToProducts: Boolean = false,
    singleBottomAction: String? = null,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (() -> Unit)? = null,
    onOpenProducts: (() -> Unit)? = null,
    onOpenConversationSummary: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen),
    ) {
        PlantBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .height(545.dp),
            alpha = 0.95f,
        )

        if (showCaptureOverlay) {
            DashedTarget(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 127.dp),
            )

            PrimaryActionHint(
                text = "ANALIZANDO CULTIVO CON IA...",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 375.dp),
            )
        }

        DraggableSlice(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = topInset)
                .background(AgroGemColors.Surface, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            collapsedOffset = sliceCollapsedOffset,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
        ) {
            DragHandle()
            if (showVoiceBadge) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(AgroGemColors.SurfaceSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "◍", color = AgroGemColors.Primary, fontSize = 12.sp)
                }
            }
            DiagnosisHeader()
            DiagnosisBodyText()
            TreatmentSection(
                showProducts = showProducts,
                onOpenProducts = onOpenProducts,
                onOpenConversationSummary = onOpenConversationSummary,
                showLinkToProducts = showLinkToProducts,
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (singleBottomAction == null) {
                if (secondaryButtonText != null && onSecondaryAction != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedPrimaryButton(
                            text = primaryButtonText,
                            onClick = onPrimaryAction,
                            modifier = Modifier.weight(1f),
                        )
                        FilledPrimaryButton(
                            text = secondaryButtonText,
                            onClick = onSecondaryAction,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    FilledPrimaryButton(text = primaryButtonText, onClick = onPrimaryAction)
                }
            } else {
                FilledPrimaryButton(text = singleBottomAction, onClick = onPrimaryAction)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DiagnosisHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Plaga detectada", color = Color.Black, fontSize = 28.sp / 1.55f, fontWeight = FontWeight.Medium)
            Pill(
                text = "Problema iniciando",
                background = AgroGemColors.AlertSoft,
                foreground = AgroGemColors.Alert,
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 8.sp,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill(
                text = "95% de confianza",
                background = AgroGemColors.ConfidenceBg,
                foreground = AgroGemColors.ConfidenceText,
                icon = "◉",
                iconColor = AgroGemColors.ConfidenceText,
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 8.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DiagnosisInfoBox(label = "Área afectada", value = "Tallo y hoja", modifier = Modifier.weight(1f))
            DiagnosisInfoBox(label = "Causa", value = "Hongo, Hemileia vastatrix", italicTail = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DiagnosisBodyText() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "✧", color = AgroGemColors.Primary, fontSize = 14.sp)
            Text(text = "Diagnóstico", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "Se ha detectado una infección avanzada por Hemileia vastatrix. El 45% del follaje muestra pústulas activas. Se requiere intervención inmediata para evitar la pérdida total de la cosecha.",
            color = AgroGemColors.TextSecondary,
            fontSize = 16.sp,
            lineHeight = 26.sp,
        )
    }
}

@Composable
private fun TreatmentSection(
    showProducts: Boolean,
    onOpenProducts: (() -> Unit)?,
    onOpenConversationSummary: (() -> Unit)?,
    showLinkToProducts: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🛡", fontSize = 14.sp, color = AgroGemColors.Primary)
            Text(text = "Plan de tratamiento", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        TreatmentStep(number = "1")
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(AgroGemColors.DividerThin))
        TreatmentStep(number = "2")

        if (showLinkToProducts) {
            Text(
                text = "Ver insumos sugeridos",
                color = AgroGemColors.Primary,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onOpenProducts?.invoke() },
            )
        }

        if (showProducts) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "◍", fontSize = 15.sp, color = AgroGemColors.Primary)
                Text(text = "Insumos sugeridos", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                products.forEachIndexed { index, product ->
                    ProductCard(
                        product = product,
                        onClick = {
                            if (index == 0) {
                                onOpenConversationSummary?.invoke()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TreatmentStep(number: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AgroGemColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number, color = Color.White, fontSize = 12.sp)
        }
        Text(
            text = "Se ha detectado una infección avanzada por Hemileia vastatrix. El 45% del follaje muestra pústulas activas. Se requiere intervención inmediata para evitar la pérdida total de la cosecha.",
            color = AgroGemColors.TextSecondary,
            fontSize = 16.sp,
            lineHeight = 26.sp,
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AgroGemColors.SurfaceSoft, RoundedCornerShape(32.dp))
            .padding(12.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.radialGradient(AgroGemColors.ProductCardGradient),
                    RoundedCornerShape(20.dp),
                ),
        )

        Text(text = "ORGÁNICO", color = AgroGemColors.Primary, fontSize = 11.sp, letterSpacing = 1.1.sp)
        Text(text = product.name, color = AgroGemColors.TextPrimary, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = product.price, color = AgroGemColors.TextSecondary, fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(AgroGemColors.PrimarySoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", color = AgroGemColors.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun DashedTarget(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 210.dp, height = 217.dp)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(2.dp, AgroGemColors.ProductAlertBorder, RoundedCornerShape(12.dp)),
    )
}
