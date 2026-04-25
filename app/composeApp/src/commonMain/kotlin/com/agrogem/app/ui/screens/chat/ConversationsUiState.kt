package com.agrogem.app.ui.screens.chat

import androidx.compose.runtime.Immutable

/**
 * UI state for the conversations list screen.
 */
@Immutable
data class ConversationsUiState(
    val analysisConversations: List<Conversation> = emptyList(),
    val normalConversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
)
