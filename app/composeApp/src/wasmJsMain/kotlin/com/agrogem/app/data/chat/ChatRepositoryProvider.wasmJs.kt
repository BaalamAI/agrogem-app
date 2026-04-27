package com.agrogem.app.data.chat

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.chat.domain.ChatFailure
import com.agrogem.app.data.chat.domain.ChatRepository
import com.agrogem.app.data.chat.domain.ChatSendResult
import com.agrogem.app.ui.screens.chat.ChatAttachment
import com.agrogem.app.ui.screens.chat.ChatMode

actual fun createChatRepository(authRepository: AuthRepository): ChatRepository = NoOpChatRepository()

private class NoOpChatRepository : ChatRepository {
    override suspend fun sendMessage(
        text: String,
        attachments: List<ChatAttachment>,
        mode: ChatMode,
    ): ChatSendResult = ChatSendResult.Failure(
        ChatFailure.Network(Exception("Chat networking not available on this platform"))
    )
}
