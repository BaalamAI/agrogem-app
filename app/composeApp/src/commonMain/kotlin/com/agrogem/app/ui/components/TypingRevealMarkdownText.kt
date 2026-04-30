package com.agrogem.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier

@Composable
fun TypingRevealMarkdownText(
    text: String,
    revealKey: Any,
    shouldAnimate: Boolean,
    onRevealCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visibleChars by remember(revealKey, shouldAnimate) { mutableIntStateOf(if (shouldAnimate) 0 else text.length) }

    LaunchedEffect(revealKey, shouldAnimate, text.length) {
        if (!shouldAnimate) {
            visibleChars = text.length
            return@LaunchedEffect
        }

        visibleChars = 0
        if (text.isBlank()) {
            onRevealCompleted()
            return@LaunchedEffect
        }
    }

    LaunchedEffect(revealKey, shouldAnimate, text.length) {
        if (!shouldAnimate || text.isBlank()) return@LaunchedEffect

        val totalChars = text.length
        val charsPerSecond = 240f
        var lastFrameNanos = 0L
        var carryChars = 0f

        while (visibleChars < totalChars) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                    return@withFrameNanos
                }

                val elapsedSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameTimeNanos

                val charsToAdvance = (elapsedSeconds * charsPerSecond) + carryChars
                val wholeChars = charsToAdvance.toInt()
                carryChars = charsToAdvance - wholeChars

                if (wholeChars > 0) {
                    visibleChars = (visibleChars + wholeChars).coerceAtMost(totalChars)
                }
            }
        }

        visibleChars = totalChars
        onRevealCompleted()
    }

    MarkdownText(
        text = text.take(visibleChars.coerceAtMost(text.length)),
        modifier = modifier,
    )
}
