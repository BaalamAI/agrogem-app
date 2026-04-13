package com.agrogem.app.ui.screens.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform seam: current implementations render a styled mock viewport.
 * Replace actuals with real camera integrations when hardware scope starts.
 */
@Composable
expect fun CameraPlaceholder(
    modifier: Modifier = Modifier,
)
