package com.agrogem.app.ui.screens.chat

import androidx.compose.runtime.Immutable
import com.agrogem.app.ui.screens.analysis.DiagnosisResult

/**
 * Represents a conversation entry in the conversations list.
 * Used for the mock conversations screen MVP.
 */
@Immutable
data class Conversation(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val timestampLabel: String,
    val analysisId: String? = null,
    val diagnosis: DiagnosisResult? = null,
)
