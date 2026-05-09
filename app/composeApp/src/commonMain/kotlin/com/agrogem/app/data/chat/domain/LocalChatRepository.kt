package com.agrogem.app.data.chat.domain

import com.agrogem.app.data.chat.local.LocalChatDataSource
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.Conversation
import kotlin.random.Random
import kotlin.time.Clock

interface LocalChatRepository {
    fun getOrCreateAnalysisConversation(analysisId: String, diagnosis: DiagnosisResult): Conversation
    fun getByAnalysisId(analysisId: String): Conversation?
    fun getById(conversationId: String): Conversation?
    fun listRecent(limit: Long = 50): List<Conversation>
    fun saveMessage(conversationId: String, message: ChatMessage)
    fun listMessages(conversationId: String): List<ChatMessage>
    fun createBlankConversation(): Conversation

    /**
     * Marks any chat_message rows still flagged `is_streaming=1` as failed,
     * so a process killed mid-inference doesn't leave a frozen "thinking…"
     * bubble after relaunch. Call once per process at app boot.
     */
    fun markOrphanStreamingMessagesAsFailed()

    /**
     * Removes a single message by id. Used to clean up a streaming placeholder
     * when the in-flight Gemma attempt errors and falls back to the backend.
     */
    fun deleteMessage(messageId: String)
}

class LocalChatRepositoryImpl(
    private val localDataSource: LocalChatDataSource,
) : LocalChatRepository {
    override fun getOrCreateAnalysisConversation(analysisId: String, diagnosis: DiagnosisResult): Conversation {
        localDataSource.getConversationByAnalysisId(analysisId)?.let { return it }

        val now = Clock.System.now().toEpochMilliseconds()
        val created = Conversation(
            id = conversationId(),
            title = "Análisis: ${diagnosis.pestName}",
            preview = diagnosis.diagnosisText,
            timestamp = now,
            timestampLabel = "Ahora",
            analysisId = analysisId,
            diagnosis = diagnosis,
        )
        localDataSource.upsertConversation(created)
        return created
    }

    override fun getByAnalysisId(analysisId: String): Conversation? =
        localDataSource.getConversationByAnalysisId(analysisId)

    override fun getById(conversationId: String): Conversation? =
        localDataSource.getConversationById(conversationId)

    override fun listRecent(limit: Long): List<Conversation> =
        localDataSource.listRecentConversations(limit)

    override fun saveMessage(conversationId: String, message: ChatMessage) {
        localDataSource.insertMessage(conversationId, message)

        val current = localDataSource.getConversationById(conversationId) ?: return
        localDataSource.upsertConversation(
            current.copy(
                preview = message.text,
                timestamp = message.timestamp,
            ),
        )
    }

    override fun listMessages(conversationId: String): List<ChatMessage> =
        localDataSource.listMessages(conversationId)

    override fun markOrphanStreamingMessagesAsFailed() {
        localDataSource.markOrphanStreamingMessagesAsFailed()
    }

    override fun deleteMessage(messageId: String) {
        localDataSource.deleteMessageById(messageId)
    }

    override fun createBlankConversation(): Conversation {
        val now = Clock.System.now().toEpochMilliseconds()
        val created = Conversation(
            id = conversationId(),
            title = "Nueva conversación",
            preview = "",
            timestamp = now,
            timestampLabel = "Ahora",
            analysisId = null,
            diagnosis = null,
        )
        localDataSource.upsertConversation(created)
        return created
    }

    private fun conversationId(): String =
        "conv_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(10000)}"
}
