package com.agrogem.app.data.chat

import com.agrogem.app.data.chat.domain.LocalChatRepository
import com.agrogem.app.data.chat.domain.LocalChatRepositoryImpl
import com.agrogem.app.data.chat.local.LocalChatDataSource
import com.agrogem.app.data.local.db.LocalDatabaseProvider

fun createLocalChatRepository(): LocalChatRepository = LocalChatRepositoryImpl(
    localDataSource = LocalChatDataSource(LocalDatabaseProvider.database),
)
