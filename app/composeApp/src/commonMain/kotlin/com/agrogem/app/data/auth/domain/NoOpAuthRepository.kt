package com.agrogem.app.data.auth.domain

/**
 * No-op auth repository for platforms without networking support (e.g. wasmJs).
 */
class NoOpAuthRepository : AuthRepository {
    override suspend fun register(phone: String, password: String): AuthResult<SessionInfo> =
        AuthResult.Network(Exception("Networking not available on this platform"))

    override suspend fun login(phone: String, password: String): AuthResult<SessionInfo> =
        AuthResult.Network(Exception("Networking not available on this platform"))

    override suspend fun restoreSession(): SessionInfo? = null
    override suspend fun clearSession() {}
    override suspend fun healthCheck(): AuthResult<SessionInfo> =
        AuthResult.NotFound(message = "Health check deferred (no stored password)")
}
