package com.agrogem.app.data.auth

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.session.SessionLocalStore

/**
 * Platform-specific creation of [AuthRepository].
 */
expect fun createAuthRepository(store: SessionLocalStore): AuthRepository
