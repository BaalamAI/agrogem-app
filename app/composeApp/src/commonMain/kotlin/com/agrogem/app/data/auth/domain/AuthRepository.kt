package com.agrogem.app.data.auth.domain

import com.agrogem.app.data.auth.api.AuthApi
import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.session.SessionSnapshot

interface AuthRepository {
    suspend fun register(phone: String, password: String): AuthResult<SessionInfo>
    suspend fun login(phone: String, password: String): AuthResult<SessionInfo>
    suspend fun restoreSession(): SessionInfo?
    suspend fun clearSession()
    suspend fun healthCheck(): AuthResult<SessionInfo>
}

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val store: SessionLocalStore,
) : AuthRepository {

    override suspend fun register(phone: String, password: String): AuthResult<SessionInfo> {
        return try {
            val response = api.register(phone = phone, password = password)
            val info = SessionInfo(sessionId = response.sessionId, phone = phone)
            persist(info)
            AuthResult.Success(info)
        } catch (e: ApiError) {
            mapApiError(e)
        } catch (e: Exception) {
            AuthResult.Network(e)
        }
    }

    override suspend fun login(phone: String, password: String): AuthResult<SessionInfo> {
        return try {
            val response = api.login(phone = phone, password = password)
            val info = SessionInfo(sessionId = response.sessionId, phone = phone)
            persist(info)
            AuthResult.Success(info)
        } catch (e: ApiError) {
            mapApiError(e)
        } catch (e: Exception) {
            AuthResult.Network(e)
        }
    }

    override suspend fun restoreSession(): SessionInfo? {
        val snapshot = store.read()
        return if (snapshot.sessionId != null && snapshot.phone != null) {
            SessionInfo(sessionId = snapshot.sessionId, phone = snapshot.phone)
        } else {
            null
        }
    }

    override suspend fun clearSession() {
        store.clearSession()
    }

    override suspend fun healthCheck(): AuthResult<SessionInfo> {
        return AuthResult.NotFound(message = "Health check deferred (no stored password)")
    }

    private suspend fun persist(info: SessionInfo) {
        val current = store.read()
        store.write(
            SessionSnapshot(
                onboardingDone = current.onboardingDone,
                phone = info.phone,
                sessionId = info.sessionId,
            )
        )
    }

    private fun <T> mapApiError(error: ApiError): AuthResult<T> = when (error) {
        is ApiError.DuplicateRegistration -> AuthResult.Duplicate()
        is ApiError.NotFound -> AuthResult.NotFound()
        is ApiError.Validation -> AuthResult.Validation(error.message)
        is ApiError.Unauthorized -> AuthResult.Unauthorized()
        is ApiError.ServerError -> AuthResult.Server()
        is ApiError.NetworkError -> AuthResult.Network(error.cause)
    }
}
