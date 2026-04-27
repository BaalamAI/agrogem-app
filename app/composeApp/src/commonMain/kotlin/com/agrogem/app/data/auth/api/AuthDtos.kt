package com.agrogem.app.data.auth.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val phone: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val phone: String,
    val password: String,
)

@Serializable
data class SessionResponse(
    @SerialName("session_id")
    val sessionId: String,
)
