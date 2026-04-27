package com.agrogem.app.data.chat

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.chat.api.KtorChatApi
import com.agrogem.app.data.chat.domain.ChatRepository
import com.agrogem.app.data.chat.domain.ChatRepositoryImpl
import com.agrogem.app.data.network.HttpClientFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun createChatRepository(authRepository: AuthRepository): ChatRepository {
    val client = HttpClientFactory.create(engine = OkHttp.create())
    val api = KtorChatApi(client)
    return ChatRepositoryImpl(api = api, authRepository = authRepository)
}
