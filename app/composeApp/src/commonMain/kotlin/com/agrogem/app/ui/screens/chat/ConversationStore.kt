package com.agrogem.app.ui.screens.chat

import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * Simple in-memory store for analysis-born conversations.
 * Lives only while the app process is alive; no durable persistence.
 */
class ConversationStore {

    private val _conversations = MutableStateFlow<Map<String, Conversation>>(emptyMap())
    val conversations: StateFlow<Map<String, Conversation>> = _conversations.asStateFlow()

    /**
     * Save (or update) an analysis-born conversation keyed by [analysisId].
     */
    fun save(analysisId: String, diagnosis: DiagnosisResult) {
        _conversations.update { current ->
            val conversation = Conversation(
                id = analysisId,
                title = "Análisis: ${diagnosis.pestName}",
                preview = diagnosis.diagnosisText,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                timestampLabel = "Ahora",
                analysisId = analysisId,
                diagnosis = diagnosis,
            )
            current + (analysisId to conversation)
        }
    }

    /**
     * Retrieve a single conversation by its [analysisId], if present.
     */
    fun get(analysisId: String): Conversation? = _conversations.value[analysisId]

    /**
     * Remove a conversation from the store.
     */
    fun remove(analysisId: String) {
        _conversations.update { current -> current - analysisId }
    }
}
