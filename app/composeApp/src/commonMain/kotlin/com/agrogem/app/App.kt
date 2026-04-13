package com.agrogem.app

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.agrogem.app.theme.AgroGemTheme
import com.agrogem.app.ui.AppShell

/**
 * Shared app entry point consumed by Android and iOS platform launchers.
 */
@Composable
@Preview
fun App() {
    AgroGemTheme {
        AppShell(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        )
    }
}
