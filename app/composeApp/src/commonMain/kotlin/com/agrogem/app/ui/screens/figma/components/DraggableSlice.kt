package com.agrogem.app.ui.screens.figma.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
internal fun DraggableSlice(
    modifier: Modifier = Modifier,
    collapsedOffset: Dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val collapsedOffsetPx = with(LocalDensity.current) { collapsedOffset.toPx() }

    Column(
        modifier = Modifier
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
            )
            .then(modifier),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
