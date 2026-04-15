package com.agrogem.app.ui.screens.figma

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private object FigmaColors {
    val Screen = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF7F7F7)
    val SurfaceSoft = Color(0xFFEFF4EE)
    val Border = Color(0xFFE3E3E3)
    val Text = Color(0xFF181D1A)
    val TextSecondary = Color(0xFF40493D)
    val TextHint = Color(0xFFB4BDB1)
    val Primary = Color(0xFF0D631B)
    val PrimarySoft = Color(0xFFA3F69C)
    val Alert = Color(0xFF824600)
    val AlertSoft = Color(0xFFFFECDB)
    val Danger = Color(0xFFBA1A1A)
    val DangerSoft = Color(0xFFFFE6E4)
    val ConfidenceBg = Color(0xFFE5F6E9)
    val ConfidenceText = Color(0xFF0D4926)
    val PillTrack = Color(0xFFE5E5E5)
    val OverlayDark = Color(0xCC181D1A)
    val CameraDarkTop = Color(0xFF2A4A5D)
    val CameraDarkBottom = Color(0xFF0D1310)
}

private enum class BadgeTone {
    Healthy,
    Warning,
    Critical,
}

private data class RecentAnalysisItem(
    val name: String,
    val subtitle: String,
    val health: String,
    val tone: BadgeTone,
)

private data class HistoryEntry(
    val crop: String,
    val meta: String,
    val status: String,
    val tone: BadgeTone,
    val seed: Int,
)

private data class ProductItem(
    val name: String,
    val price: String,
)

private val dashboardRecentItems = listOf(
    RecentAnalysisItem(
        name = "ALBAHACA",
        subtitle = "Sin plagas detectadas",
        health = "Salud: 98%",
        tone = BadgeTone.Healthy,
    ),
    RecentAnalysisItem(
        name = "TOMATE",
        subtitle = "Estrés hídrico leve",
        health = "Salud: 72%",
        tone = BadgeTone.Warning,
    ),
)

private val historyToday = listOf(
    HistoryEntry("Tomate Roma", "10:45 AM • Invernadero A", "SALUDABLE", BadgeTone.Healthy, 1),
    HistoryEntry("Maíz Dulce", "08:20 AM • Parcela Norte", "ALERTA", BadgeTone.Warning, 2),
)

private val historyYesterday = listOf(
    HistoryEntry("Papa Blanca", "04:15 PM • Sección C-4", "CRÍTICO", BadgeTone.Critical, 3),
    HistoryEntry("Papa Blanca", "04:15 PM • Sección C-4", "CRÍTICO", BadgeTone.Critical, 4),
)

private val products = listOf(
    ProductItem("Caldo Bordelés XL", "$24.50"),
    ProductItem("Caldo Bordelés XL", "$24.50"),
)

@Composable
fun HomeFigmaScreen(
    onOpenCamera: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaColors.Screen)
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill(
                text = "Guatemala, Zacapa",
                background = FigmaColors.Surface,
                foreground = Color(0xFF383838),
                icon = "•",
                iconColor = Color(0xFF4B5E4C),
                horizontal = 12.dp,
                vertical = 6.dp,
            )
            RoundIconButton(
                label = "🔔",
                background = FigmaColors.Surface,
                onClick = {},
            )
        }

        WeatherCard()
        MetricsCard()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FigmaColors.Surface, RoundedCornerShape(30.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Análisis Recientes",
                    color = Color.Black,
                    fontSize = 32.sp / 1.75f,
                    lineHeight = 28.sp,
                )
                Text(
                    text = "Ver todo",
                    color = Color.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onOpenHistory),
                )
            }

            dashboardRecentItems.forEachIndexed { index, item ->
                RecentAnalysisRow(item = item, seed = index)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryActionHint(
            text = "TOMAR FOTO PARA ANALIZAR CON IA",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onOpenCamera),
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WeatherCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "GUATEMALA, ZACAPA",
                color = Color(0xFF4E4E4E),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = "24°C",
                color = Color(0xFF242424),
                fontSize = 52.sp / 1.75f,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Día despejado",
                color = Color(0xFF4E4E4E),
                fontSize = 16.sp,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Pill(
                text = "Today",
                background = Color(0xFFF1F1F1),
                foreground = Color(0xFF8B8B8B),
                icon = "⌄",
                iconColor = Color(0xFF8B8B8B),
                horizontal = 8.dp,
                vertical = 4.dp,
                textSize = 9.sp,
            )
            Text(
                text = "☀",
                fontSize = 34.sp,
                color = Color(0xFF4D4D4D),
            )
            Text(
                text = "Monday, 12 Oct",
                color = Color(0xFF616161),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun MetricsCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.Surface, RoundedCornerShape(15.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricItem(icon = "💧", value = "78%", label = "HUMIDITY")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(Color(0x264D4D4D)),
        )
        MetricItem(icon = "☁", value = "65%", label = "CLOUDS")
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(1.dp)
                .background(Color(0x264D4D4D)),
        )
        MetricItem(icon = "☼", value = "Low", label = "UV INDEX")
    }
}

@Composable
private fun MetricItem(
    icon: String,
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = icon, fontSize = 17.sp)
        Text(text = value, color = Color(0xFF4C4C4C), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text = label, color = Color(0x994C4C4C), fontSize = 10.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun RecentAnalysisRow(
    item: RecentAnalysisItem,
    seed: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafThumb(seed = seed)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    color = Color(0xFF6C6C6C),
                    fontSize = 12.sp,
                    letterSpacing = 1.1.sp,
                )
                StatusBadge(tone = item.tone)
            }

            Text(
                text = item.health,
                color = Color.Black,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            )
            Text(
                text = item.subtitle,
                color = Color.Black,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun HistoryFigmaScreen(
    onOpenEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaColors.Screen)
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Historial de análisis",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )

        SearchBar()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { DateHeader("HOY, 24 OCTUBRE") }
            items(historyToday) { entry ->
                HistoryCard(entry = entry, onOpenEntry = onOpenEntry)
            }
            item { DateHeader("AYER, 23 OCTUBRE") }
            items(historyYesterday) { entry ->
                HistoryCard(entry = entry, onOpenEntry = onOpenEntry)
            }
        }
    }
}

@Composable
private fun SearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "⌕", color = Color(0xFFA5A5A5), fontSize = 16.sp)
        Text(text = "Buscar", color = Color(0xFFB6B6B6), fontSize = 14.sp)
    }
}

@Composable
private fun DateHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFDDE2DF)))
        Text(
            text = label,
            color = Color(0xFF707A6C),
            fontSize = 11.sp,
            letterSpacing = 1.1.sp,
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFDDE2DF)))
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    onOpenEntry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaColors.Surface, RoundedCornerShape(48.dp))
            .padding(16.dp)
            .clickable(onClick = onOpenEntry),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafThumb(seed = entry.seed, rounded = 16.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = entry.crop, color = FigmaColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = entry.meta, color = FigmaColors.TextHint, fontSize = 14.sp)
            StatusBadge(entry.tone, labelOverride = entry.status)
        }
        Text(text = "›", color = Color(0xFFB7C2B5), fontSize = 22.sp)
    }
}

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
fun DiagnosisFigmaScreen(
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiagnosisBaseLayout(
        modifier = modifier,
        topInset = 455.dp,
        sliceCollapsedOffset = 200.dp,
        showCaptureOverlay = true,
        showProducts = false,
        primaryButtonText = "Hablar con Agente",
        secondaryButtonText = "Hablar con agente",
        singleBottomAction = null,
        onPrimaryAction = onOpenPlan,
        onSecondaryAction = onOpenPlan,
    )
}

@Composable
fun TreatmentPlanFigmaScreen(
    onSaveAndExit: () -> Unit,
    onTalk: () -> Unit,
    onOpenProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiagnosisBaseLayout(
        modifier = modifier,
        topInset = 57.dp,
        sliceCollapsedOffset = 360.dp,
        showCaptureOverlay = false,
        showProducts = false,
        showVoiceBadge = true,
        showLinkToProducts = true,
        primaryButtonText = "Guardar y salir",
        secondaryButtonText = "Hablar con agente",
        onPrimaryAction = onSaveAndExit,
        onSecondaryAction = onTalk,
        onOpenProducts = onOpenProducts,
    )
}

@Composable
fun TreatmentProductsFigmaScreen(
    onSaveAndExit: () -> Unit,
    onTalk: () -> Unit,
    onOpenConversationSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiagnosisBaseLayout(
        modifier = modifier,
        topInset = 57.dp,
        sliceCollapsedOffset = 360.dp,
        showCaptureOverlay = false,
        showProducts = true,
        showVoiceBadge = true,
        showLinkToProducts = false,
        primaryButtonText = "Guardar y salir",
        secondaryButtonText = "Hablar con agente",
        onPrimaryAction = onSaveAndExit,
        onSecondaryAction = onTalk,
        onOpenConversationSummary = onOpenConversationSummary,
    )
}

@Composable
fun ConversationSummaryFigmaScreen(
    onViewConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiagnosisBaseLayout(
        modifier = modifier,
        topInset = 57.dp,
        sliceCollapsedOffset = 360.dp,
        showCaptureOverlay = false,
        showProducts = true,
        showVoiceBadge = true,
        primaryButtonText = "Ver conversación",
        singleBottomAction = "Ver conversación",
        onPrimaryAction = onViewConversation,
        onSecondaryAction = null,
    )
}

@Composable
private fun DiagnosisBaseLayout(
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
            .background(FigmaColors.Screen),
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
                .background(FigmaColors.Surface, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            collapsedOffset = sliceCollapsedOffset,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DragHandle()
            if (showVoiceBadge) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(FigmaColors.SurfaceSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "◍", color = FigmaColors.Primary, fontSize = 12.sp)
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
private fun DraggableSlice(
    modifier: Modifier = Modifier,
    collapsedOffset: Dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val collapsedOffsetPx = with(LocalDensity.current) { collapsedOffset.toPx() }

    Column(
        modifier = modifier
            .offset { IntOffset(0, offsetPx.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    offsetPx = (offsetPx + delta).coerceIn(0f, collapsedOffsetPx)
                },
                onDragStopped = { velocity ->
                    val shouldCollapse = offsetPx > (collapsedOffsetPx * 0.45f) || velocity > 1800f
                    offsetPx = if (shouldCollapse) collapsedOffsetPx else 0f
                },
            ),
        verticalArrangement = verticalArrangement,
        content = content,
    )
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
                background = FigmaColors.AlertSoft,
                foreground = FigmaColors.Alert,
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
                background = FigmaColors.ConfidenceBg,
                foreground = FigmaColors.ConfidenceText,
                icon = "◉",
                iconColor = FigmaColors.ConfidenceText,
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
private fun DiagnosisBodyText() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "✧", color = FigmaColors.Primary, fontSize = 14.sp)
            Text(text = "Diagnóstico", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "Se ha detectado una infección avanzada por Hemileia vastatrix. El 45% del follaje muestra pústulas activas. Se requiere intervención inmediata para evitar la pérdida total de la cosecha.",
            color = FigmaColors.TextSecondary,
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
            Text(text = "🛡", fontSize = 14.sp, color = FigmaColors.Primary)
            Text(text = "Plan de tratamiento", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        TreatmentStep(number = "1")
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFF3F3F3)))
        TreatmentStep(number = "2")

        if (showLinkToProducts) {
            Text(
                text = "Ver insumos sugeridos",
                color = FigmaColors.Primary,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onOpenProducts?.invoke() },
            )
        }

        if (showProducts) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "◍", fontSize = 15.sp, color = FigmaColors.Primary)
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
                .background(FigmaColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number, color = Color.White, fontSize = 12.sp)
        }
        Text(
            text = "Se ha detectado una infección avanzada por Hemileia vastatrix. El 45% del follaje muestra pústulas activas. Se requiere intervención inmediata para evitar la pérdida total de la cosecha.",
            color = FigmaColors.TextSecondary,
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
            .background(FigmaColors.SurfaceSoft, RoundedCornerShape(32.dp))
            .padding(12.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF6D5030), Color(0xFF121212)),
                    ),
                    RoundedCornerShape(20.dp),
                ),
        )

        Text(text = "ORGÁNICO", color = FigmaColors.Primary, fontSize = 11.sp, letterSpacing = 1.1.sp)
        Text(text = product.name, color = FigmaColors.Text, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = product.price, color = FigmaColors.TextSecondary, fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(FigmaColors.PrimarySoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", color = FigmaColors.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

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

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun RoundIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.4f),
    foreground: Color = Color(0xFF747474),
    size: androidx.compose.ui.unit.Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = foreground, fontSize = (size.value * 0.42f).sp)
    }
}

@Composable
private fun Pill(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconColor: Color = foreground,
    horizontal: androidx.compose.ui.unit.Dp = 12.dp,
    vertical: androidx.compose.ui.unit.Dp = 5.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 10.sp,
) {
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = horizontal, vertical = vertical),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Text(text = it, color = iconColor, fontSize = textSize)
        }
        Text(text = text, color = foreground, fontSize = textSize)
    }
}

@Composable
private fun StatusBadge(
    tone: BadgeTone,
    labelOverride: String? = null,
) {
    val (label, background, foreground) = when (tone) {
        BadgeTone.Healthy -> Triple(labelOverride ?: "ÓPTIMO", Color(0x330D631B), Color(0xFF438600))
        BadgeTone.Warning -> Triple(labelOverride ?: "ATENCIÓN", Color(0x4CFF7248), Color(0xFFDB0000))
        BadgeTone.Critical -> Triple(labelOverride ?: "CRÍTICO", FigmaColors.DangerSoft, FigmaColors.Danger)
    }

    Pill(
        text = label,
        background = background,
        foreground = foreground,
        horizontal = 10.dp,
        vertical = 4.dp,
        textSize = 10.sp,
    )
}

@Composable
private fun LeafThumb(
    seed: Int,
    rounded: androidx.compose.ui.unit.Dp = 32.dp,
    size: androidx.compose.ui.unit.Dp = 96.dp,
) {
    val brush = when (seed % 4) {
        0 -> Brush.linearGradient(listOf(Color(0xFF9AC55A), Color(0xFF27491E)))
        1 -> Brush.linearGradient(listOf(Color(0xFF2A5838), Color(0xFF16291A)))
        2 -> Brush.linearGradient(listOf(Color(0xFF789C4C), Color(0xFF1F311B)))
        else -> Brush.linearGradient(listOf(Color(0xFF6D8E5A), Color(0xFF22372B)))
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(brush, RoundedCornerShape(rounded)),
    )
}

@Composable
private fun PrimaryActionHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(FigmaColors.OverlayDark, RoundedCornerShape(999.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(FigmaColors.PrimarySoft, CircleShape),
        )
        Text(text = text, color = Color.White, fontSize = 12.sp, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun PlantBackdrop(
    modifier: Modifier,
    alpha: Float,
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3D6278), Color(0xFF8AB0C3), Color(0xFF7E8F6A), Color(0xFF22301E)),
                ),
            )
            .alpha(alpha),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC172013)),
                    ),
                ),
        )
    }
}

@Composable
private fun DashedTarget(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 210.dp, height = 217.dp)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFFF8600), RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color(0xFFE4E4E4), RoundedCornerShape(20.dp)),
        )
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

@Composable
private fun OutlinedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.dp, Color(0xFF008023), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = FigmaColors.Primary, fontSize = 12.sp)
    }
}

@Composable
private fun FilledPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF008026), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DotsIndicator(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(6.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape),
            )
        }
    }
}
