package com.agrogem.app.data.chat.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSendRequest(
    @SerialName("session_id")
    val sessionId: String,
    val message: MessagePayload,
)

@Serializable
data class MessagePayload(
    val role: String,
    val content: String,
)

@Serializable
data class ChatConversationDto(
    val id: String,
    val messages: List<MessageDto>,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String,
    @SerialName("created_at")
    val createdAt: String,
)
