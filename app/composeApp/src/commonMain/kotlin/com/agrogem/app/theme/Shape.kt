package com.agrogem.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class AgroGemShapeTokens(
    val card: RoundedCornerShape,
    val largeCard: RoundedCornerShape,
    val pill: RoundedCornerShape,
)

val DefaultAgroGemShapeTokens = AgroGemShapeTokens(
    card = RoundedCornerShape(12.dp),
    largeCard = RoundedCornerShape(16.dp),
    pill = RoundedCornerShape(9_999.dp),
)

val AgroGemShapes = Shapes(
    small = DefaultAgroGemShapeTokens.card,
    medium = DefaultAgroGemShapeTokens.largeCard,
    large = DefaultAgroGemShapeTokens.largeCard,
)
