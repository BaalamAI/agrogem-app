package com.agrogem.app.ui.screens.gemma_demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
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
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.ui.screens.chat.ChatContent

@Composable
fun GemmaDemoScreen(
    viewModel: GemmaDemoViewModel,
    onBack: () -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(AgroGemColors.Screen)) {
        if (isDownloading) {
            // Pantalla de carga/descarga
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = AgroGemColors.Primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Preparando Gemma 4...",
                    color = AgroGemColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Descargando modelo (2.5 GB). Revisa las notificaciones.",
                    color = AgroGemColors.TextChatHint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            // Ahora usamos el componente Stateless ChatContent
            // que es totalmente compatible con nuestro GemmaDemoViewModel
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
        }
    }
}
