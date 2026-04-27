package com.agrogem.app.data.auth.domain

/**
 * Domain representation of an authenticated user session.
 */
data class SessionInfo(
    val sessionId: String,
    val phone: String,
)
