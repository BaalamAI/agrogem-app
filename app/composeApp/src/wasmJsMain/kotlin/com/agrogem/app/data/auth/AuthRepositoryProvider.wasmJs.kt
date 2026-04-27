package com.agrogem.app.data.auth

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.auth.domain.NoOpAuthRepository
import com.agrogem.app.data.session.SessionLocalStore

actual fun createAuthRepository(store: SessionLocalStore): AuthRepository = NoOpAuthRepository()
