package com.agrogem.app.data.chat.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface ChatApi {
    suspend fun sendMessage(sessionId: String, content: String): ChatConversationDto
}

class KtorChatApi(private val client: HttpClient) : ChatApi {

    override suspend fun sendMessage(sessionId: String, content: String): ChatConversationDto {
        return try {
            val response = client.post("/chat/messages") {
                setBody(
                    ChatSendRequest(
                        sessionId = sessionId,
                        message = MessagePayload(role = "user", content = content)
                    )
                )
            }
            if (response.status.value in 200..299) {
                response.body<ChatConversationDto>()
            } else {
                throw ApiError.from(response.status, response.body<String>())
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.from(e)
        }
    }
}
