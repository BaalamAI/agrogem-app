package com.agrogem.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.ui.components.FilledPrimaryButton
import com.agrogem.app.ui.components.Pill

@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onOpenConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen)
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Conversaciones",
            color = AgroGemColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )

        FilledPrimaryButton(
            text = "Nuevo chat",
            onClick = onNewChat,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.analysisConversations.isNotEmpty()) {
                item {
                    SectionLabel("Vinculadas a análisis")
                }
                items(uiState.analysisConversations, key = { it.id }) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { onOpenConversation(conversation) },
                        showAnalysisBadge = true,
                    )
                }
            }

            if (uiState.normalConversations.isNotEmpty()) {
                item {
                    SectionLabel("Conversaciones")
                }
                items(uiState.normalConversations, key = { it.id }) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { onOpenConversation(conversation) },
                        showAnalysisBadge = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AgroGemColors.TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit,
    showAnalysisBadge: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgroGemColors.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = conversation.title,
                color = AgroGemColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showAnalysisBadge) {
                Pill(
                    text = "Análisis",
                    background = AgroGemColors.ConfidenceBg,
                    foreground = AgroGemColors.ConfidenceText,
                    horizontal = 8.dp,
                    vertical = 4.dp,
                    textSize = 10.sp,
                )
            }
        }

        Text(
            text = conversation.preview,
            color = AgroGemColors.TextHint,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (showAnalysisBadge && conversation.analysisId != null) {
            Text(
                text = "Ligado a ${conversation.analysisId}",
                color = AgroGemColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Text(
            text = conversation.timestampLabel,
            color = AgroGemColors.TextMuted,
            fontSize = 10.sp,
        )
    }
}
