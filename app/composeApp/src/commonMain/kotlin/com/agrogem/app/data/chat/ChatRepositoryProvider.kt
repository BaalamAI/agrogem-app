package com.agrogem.app.data.chat

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.chat.domain.ChatRepository

/**
 * Platform-specific creation of [ChatRepository].
 */
expect fun createChatRepository(authRepository: AuthRepository): ChatRepository
