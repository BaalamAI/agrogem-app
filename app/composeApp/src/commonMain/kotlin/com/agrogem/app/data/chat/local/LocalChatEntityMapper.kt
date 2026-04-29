package com.agrogem.app.data.chat.local

import com.agrogem.app.data.local.db.Chat_conversation
import com.agrogem.app.data.local.db.Chat_message
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.Conversation
import com.agrogem.app.ui.screens.chat.MessageSender
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalChatEntityMapper(
    private val json: Json = Json,
) {
    fun toConversation(entity: Chat_conversation): Conversation = Conversation(
        id = entity.conversation_id,
        title = entity.title,
        preview = entity.preview,
        timestamp = entity.timestamp_epoch_millis,
        timestampLabel = "Ahora",
        analysisId = entity.analysis_id,
        diagnosis = entity.diagnosis_pest_name?.let {
            DiagnosisResult(
                pestName = it,
                confidence = entity.diagnosis_confidence?.toFloat() ?: 0f,
                severity = entity.diagnosis_severity ?: "",
                affectedArea = entity.diagnosis_affected_area ?: "",
                cause = entity.diagnosis_cause ?: "",
                diagnosisText = entity.diagnosis_text ?: "",
                treatmentSteps = entity.diagnosis_treatment_steps_json?.let { stepsJson ->
                    json.decodeFromString<List<String>>(stepsJson)
                } ?: emptyList(),
                isConfidenceReliable = entity.diagnosis_is_confidence_reliable == 1L,
            )
        },
    )

    fun toConversationParams(model: Conversation, nowEpochMillis: Long): ConversationInsertParams = ConversationInsertParams(
        conversationId = model.id,
        analysisId = model.analysisId,
        title = model.title,
        preview = model.preview,
        timestampEpochMillis = model.timestamp,
        diagnosisPestName = model.diagnosis?.pestName,
        diagnosisConfidence = model.diagnosis?.confidence?.toDouble(),
        diagnosisSeverity = model.diagnosis?.severity,
        diagnosisAffectedArea = model.diagnosis?.affectedArea,
        diagnosisCause = model.diagnosis?.cause,
        diagnosisText = model.diagnosis?.diagnosisText,
        diagnosisTreatmentStepsJson = model.diagnosis?.let { json.encodeToString(it.treatmentSteps) },
        diagnosisIsConfidenceReliable = model.diagnosis?.let { if (it.isConfidenceReliable) 1L else 0L },
        createdAtEpochMillis = nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun toMessage(entity: Chat_message): ChatMessage = ChatMessage(
        id = entity.message_id,
        text = entity.text,
        thought = entity.thought,
        sender = if (entity.sender == "user") MessageSender.User else MessageSender.Assistant,
        attachments = emptyList(),
        timestamp = entity.timestamp_epoch_millis,
        isStreaming = entity.is_streaming != 0L,
    )

    fun toMessageParams(conversationId: String, message: ChatMessage, nowEpochMillis: Long): MessageInsertParams =
        MessageInsertParams(
            messageId = message.id,
            conversationId = conversationId,
            text = message.text,
            sender = if (message.sender == MessageSender.User) "user" else "assistant",
            // Local DB stores a JSON payload, but current chat runtime does not reconstruct
            // strongly-typed ChatAttachment yet. Keep explicit placeholder until attachment
            // domain/storage contract is finalized in a dedicated batch.
            attachmentsJson = "[]",
            thought = message.thought,
            isStreaming = if (message.isStreaming) 1L else 0L,
            timestampEpochMillis = message.timestamp,
            createdAtEpochMillis = nowEpochMillis,
        )
}

data class ConversationInsertParams(
    val conversationId: String,
    val analysisId: String?,
    val title: String,
    val preview: String,
    val timestampEpochMillis: Long,
    val diagnosisPestName: String?,
    val diagnosisConfidence: Double?,
    val diagnosisSeverity: String?,
    val diagnosisAffectedArea: String?,
    val diagnosisCause: String?,
    val diagnosisText: String?,
    val diagnosisTreatmentStepsJson: String?,
    val diagnosisIsConfidenceReliable: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class MessageInsertParams(
    val messageId: String,
    val conversationId: String,
    val text: String,
    val sender: String,
    val attachmentsJson: String,
    val thought: String?,
    val isStreaming: Long,
    val timestampEpochMillis: Long,
    val createdAtEpochMillis: Long,
)
