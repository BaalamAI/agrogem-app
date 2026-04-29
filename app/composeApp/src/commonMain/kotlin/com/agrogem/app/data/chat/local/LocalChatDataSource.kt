package com.agrogem.app.data.chat.local

import com.agrogem.app.data.local.db.AgroGemDatabase
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.Conversation
import kotlin.time.Clock

class LocalChatDataSource(
    private val database: AgroGemDatabase,
    private val mapper: LocalChatEntityMapper = LocalChatEntityMapper(),
) {
    fun upsertConversation(conversation: Conversation) {
        val now = Clock.System.now().toEpochMilliseconds()
        val params = mapper.toConversationParams(conversation, now)
        database.analysisQueries.upsertChatConversation(
            conversation_id = params.conversationId,
            analysis_id = params.analysisId,
            title = params.title,
            preview = params.preview,
            timestamp_epoch_millis = params.timestampEpochMillis,
            diagnosis_pest_name = params.diagnosisPestName,
            diagnosis_confidence = params.diagnosisConfidence,
            diagnosis_severity = params.diagnosisSeverity,
            diagnosis_affected_area = params.diagnosisAffectedArea,
            diagnosis_cause = params.diagnosisCause,
            diagnosis_text = params.diagnosisText,
            diagnosis_treatment_steps_json = params.diagnosisTreatmentStepsJson,
            diagnosis_is_confidence_reliable = params.diagnosisIsConfidenceReliable,
            created_at_epoch_millis = params.createdAtEpochMillis,
            updated_at_epoch_millis = params.updatedAtEpochMillis,
        )
    }

    fun getConversationById(conversationId: String): Conversation? =
        database.analysisQueries.selectChatConversationById(conversationId)
            .executeAsOneOrNull()
            ?.let(mapper::toConversation)

    fun getConversationByAnalysisId(analysisId: String): Conversation? =
        database.analysisQueries.selectChatConversationByAnalysisId(analysisId)
            .executeAsOneOrNull()
            ?.let(mapper::toConversation)

    fun listRecentConversations(limit: Long): List<Conversation> =
        database.analysisQueries.selectRecentChatConversations(limit)
            .executeAsList()
            .map(mapper::toConversation)

    fun insertMessage(conversationId: String, message: ChatMessage) {
        val now = Clock.System.now().toEpochMilliseconds()
        val params = mapper.toMessageParams(conversationId, message, now)
        database.analysisQueries.insertChatMessage(
            message_id = params.messageId,
            conversation_id = params.conversationId,
            text = params.text,
            sender = params.sender,
            attachments_json = params.attachmentsJson,
            thought = params.thought,
            is_streaming = params.isStreaming,
            timestamp_epoch_millis = params.timestampEpochMillis,
            created_at_epoch_millis = params.createdAtEpochMillis,
        )
    }

    fun listMessages(conversationId: String): List<ChatMessage> =
        database.analysisQueries.selectChatMessagesByConversationId(conversationId)
            .executeAsList()
            .map(mapper::toMessage)
}
