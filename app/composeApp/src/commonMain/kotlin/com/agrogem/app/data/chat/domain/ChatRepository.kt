package com.agrogem.app.data.chat.domain

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.chat.api.ChatApi
import com.agrogem.app.data.chat.api.MessageDto
import com.agrogem.app.data.network.ApiError
import com.agrogem.app.ui.screens.chat.ChatAttachment
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.ChatMode
import com.agrogem.app.ui.screens.chat.MessageSender
sealed class ChatFailure {
    data object SessionExpired : ChatFailure()
    data class Network(val cause: Throwable) : ChatFailure()
    data object Server : ChatFailure()
}

sealed class ChatSendResult {
    data class Success(
        val conversationId: String,
        val messages: List<ChatMessage>,
    ) : ChatSendResult()

    data class Failure(val reason: ChatFailure) : ChatSendResult()
}

interface ChatRepository {
    suspend fun sendMessage(
        text: String,
        attachments: List<ChatAttachment>,
        mode: ChatMode,
    ): ChatSendResult
}

class ChatRepositoryImpl(
    private val api: ChatApi,
    private val authRepository: AuthRepository,
) : ChatRepository {

    override suspend fun sendMessage(
        text: String,
        attachments: List<ChatAttachment>,
        mode: ChatMode,
    ): ChatSendResult {
        val session = authRepository.restoreSession()
            ?: return ChatSendResult.Failure(ChatFailure.SessionExpired)

        return try {
            // TODO: attachments and mode are currently out of scope / intentionally dropped.
            // Forward them to ChatApi once the backend supports them.
            val conversation = api.sendMessage(
                sessionId = session.sessionId,
                content = text,
            )
            val messages = conversation.messages.map { dto ->
                ChatMessage(
                    id = generateMessageId(dto),
                    text = dto.content,
                    sender = mapSender(dto.role),
                    attachments = emptyList(),
                    timestamp = parseIso8601ToEpochMillis(dto.createdAt),
                )
            }
            ChatSendResult.Success(
                conversationId = conversation.id,
                messages = messages,
            )
        } catch (e: ApiError) {
            mapApiError(e)
        } catch (e: Exception) {
            ChatSendResult.Failure(ChatFailure.Network(e))
        }
    }

    private fun mapSender(role: String): MessageSender = when (role) {
        "user" -> MessageSender.User
        else -> MessageSender.Assistant
    }

    private fun mapApiError(error: ApiError): ChatSendResult = when (error) {
        is ApiError.NotFound -> ChatSendResult.Failure(ChatFailure.SessionExpired)
        is ApiError.NetworkError -> ChatSendResult.Failure(ChatFailure.Network(error.cause))
        else -> ChatSendResult.Failure(ChatFailure.Server)
    }

    private fun generateMessageId(dto: MessageDto): String {
        return "${dto.role}_${dto.content.hashCode()}_${dto.createdAt}"
    }
}

/**
 * Minimal ISO-8601 parser for the backend format `YYYY-MM-DDTHH:MM:SSZ`.
 * Pure Kotlin — works on all configured KMP targets.
 */
internal fun parseIso8601ToEpochMillis(iso: String): Long {
    // Expected: 2026-04-27T10:00:00Z
    if (iso.length < 19) return 0L
    val year = iso.substring(0, 4).toIntOrNull() ?: return 0L
    val month = iso.substring(5, 7).toIntOrNull() ?: return 0L
    val day = iso.substring(8, 10).toIntOrNull() ?: return 0L
    val hour = iso.substring(11, 13).toIntOrNull() ?: return 0L
    val minute = iso.substring(14, 16).toIntOrNull() ?: return 0L
    val second = iso.substring(17, 19).toIntOrNull() ?: return 0L

    var totalDays = 0
    for (y in 1970 until year) {
        totalDays += if (isLeapYear(y)) 366 else 365
    }
    val daysInMonth = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    for (m in 1 until month) {
        totalDays += daysInMonth[m - 1]
    }
    totalDays += day - 1

    val totalSeconds = totalDays * 86400L + hour * 3600L + minute * 60L + second
    return totalSeconds * 1000L
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
