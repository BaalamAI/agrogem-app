package com.agrogem.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.agrogem.app.theme.AgroGemColors

@Composable
internal fun PlantBackdrop(
    modifier: Modifier,
    alpha: Float,
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(AgroGemColors.BackdropGradient),
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
                        listOf(Color.Transparent, AgroGemColors.BackdropOverlay),
                    ),
                ),
        )
    }
}

/**
 * Overload that displays a captured image from a device URI.
 * Falls back to the gradient backdrop if [imageUri] is null.
 */
@Composable
internal fun PlantBackdrop(
    imageUri: String?,
    modifier: Modifier,
    alpha: Float,
) {
    if (imageUri == null) {
        PlantBackdrop(modifier = modifier, alpha = alpha)
        return
    }

    Box(modifier = modifier.alpha(alpha)) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Captured plant image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay at the bottom for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, AgroGemColors.BackdropOverlay),
                    ),
                ),
        )
    }
}
