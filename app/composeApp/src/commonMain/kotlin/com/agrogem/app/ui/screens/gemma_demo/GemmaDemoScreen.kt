package com.agrogem.app.ui.screens.gemma_demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogem.app.data.GemmaPreparationStatus
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.ui.components.GemmaPreparationStatusScreen
import com.agrogem.app.ui.screens.chat.ChatContent

@Composable
fun GemmaDemoScreen(
    viewModel: GemmaDemoViewModel,
    onBack: () -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val preparationStatus by viewModel.preparationStatus.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(AgroGemColors.Screen)) {
        if (preparationStatus == GemmaPreparationStatus.Downloading || preparationStatus == GemmaPreparationStatus.Preparing || preparationStatus == GemmaPreparationStatus.NotPrepared) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GemmaPreparationStatusScreen(
                    productName = "Gemma 4",
                    status = preparationStatus,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                ChatContent(
                    uiState = uiState,
                    onEvent = { viewModel.onEvent(it) },
                    onBack = onBack,
                    onRequestClose = onBack,
                    onMicClick = { /* No implementado para demo */ },
                    onLaunchCamera = onLaunchCamera,
                    onLaunchGallery = onLaunchGallery,
                    showConfirmDialog = false
                )

                if (preparationStatus is GemmaPreparationStatus.Unavailable) {
                    Text(
                        text = "Gemma no está disponible. Este demo no puede responder con IA local.",
                        color = Color(0xFF7A4B00),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 72.dp)
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
