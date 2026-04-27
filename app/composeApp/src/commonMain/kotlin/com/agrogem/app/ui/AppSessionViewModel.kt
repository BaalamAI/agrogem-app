package com.agrogem.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.auth.domain.AuthResult
import com.agrogem.app.data.auth.domain.SessionInfo
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.session.SessionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-level session state holder.
 *
 * Owns bootstrap (restore on start) and onboarding auth completion
 * (register → fallback to login on duplicate).
 */
class AppSessionViewModel(
    private val authRepository: AuthRepository,
    private val sessionLocalStore: SessionLocalStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun bootstrap() {
        viewModelScope.launch {
            val snapshot = sessionLocalStore.read()
            val restored = authRepository.restoreSession()
            _uiState.value = SessionUiState(
                isLoading = false,
                session = restored,
                onboardingDone = snapshot.onboardingDone,
            )
        }
    }

    fun registerOrLogin(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.register(phone, password)) {
                is AuthResult.Success -> markDone(result.data)
                is AuthResult.Duplicate -> {
                    when (val loginResult = authRepository.login(phone, password)) {
                        is AuthResult.Success -> markDone(loginResult.data)
                        else -> _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = mapError(loginResult),
                        )
                    }
                }
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = mapError(result),
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun reportSessionExpired() {
        _uiState.value = _uiState.value.copy(
            error = "La sesión ha expirado, iniciá sesión de nuevo",
        )
    }

    private suspend fun markDone(session: SessionInfo) {
        sessionLocalStore.write(
            SessionSnapshot(
                onboardingDone = true,
                phone = session.phone,
                sessionId = session.sessionId,
            )
        )
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            session = session,
            onboardingDone = true,
        )
    }

    private fun mapError(result: AuthResult<*>): String = when (result) {
        is AuthResult.Duplicate -> "Phone already registered"
        is AuthResult.Validation -> result.message
        is AuthResult.Unauthorized -> "Invalid credentials. Please check your phone and password."
        is AuthResult.NotFound -> "Session not found"
        is AuthResult.Server -> result.message
        is AuthResult.Network -> "Network error. Please check your connection and try again."
        else -> "Unknown error"
    }
}

/**
 * Immutable UI state for the app session.
 */
data class SessionUiState(
    val isLoading: Boolean = false,
    val session: SessionInfo? = null,
    val onboardingDone: Boolean = false,
    val error: String? = null,
)
