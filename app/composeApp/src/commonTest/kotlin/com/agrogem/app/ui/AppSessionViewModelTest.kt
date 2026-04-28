package com.agrogem.app.ui

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.auth.domain.AuthResult
import com.agrogem.app.data.auth.domain.SessionInfo
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.session.SessionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Bootstrap Tests ==========

    @Test
    fun `bootstrap restores session and onboarding flag from store`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.write(SessionSnapshot(onboardingDone = true, phone = "+50255550000", sessionId = "sess-001"))
        val repo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.bootstrap()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(true, state.onboardingDone)
        val session = state.session
        assertNotNull(session)
        assertEquals("sess-001", session.sessionId)
        assertEquals("+50255550000", session.phone)
    }

    @Test
    fun `bootstrap with empty store sets anonymous state`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(restoredSession = null)
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.bootstrap()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.onboardingDone)
        assertNull(state.session)
        assertNull(state.error)
    }

    // ========== Register / Login Tests ==========

    @Test
    fun `registerOrLogin with success persists session and marks onboarding done`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Success(SessionInfo("sess-reg", "+50255550000"))
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(true, state.onboardingDone)
        val session = state.session
        assertNotNull(session)
        assertEquals("sess-reg", session.sessionId)

        val stored = store.read()
        assertEquals(true, stored.onboardingDone)
        assertEquals("+50255550000", stored.phone)
        assertEquals("sess-reg", stored.sessionId)
    }

    @Test
    fun `registerOrLogin keeps persisted onboarding profile fields`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.write(
            SessionSnapshot(
                onboardingDone = true,
                name = "Kevin",
                crops = "maíz",
                area = "3 hectáreas",
                stage = "floración",
            )
        )
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Success(SessionInfo("sess-reg", "+50255550000"))
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val stored = store.read()
        assertEquals("Kevin", stored.name)
        assertEquals("maíz", stored.crops)
        assertEquals("3 hectáreas", stored.area)
        assertEquals("floración", stored.stage)
    }

    @Test
    fun `registerOrLogin with duplicate falls back to login`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Duplicate(),
            loginResult = AuthResult.Success(SessionInfo("sess-login", "+50255550000"))
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(true, state.onboardingDone)
        val session = state.session
        assertNotNull(session)
        assertEquals("sess-login", session.sessionId)
        assertTrue(repo.loginCalled)
    }

    @Test
    fun `registerOrLogin passes same password to register and fallback login`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Duplicate(),
            loginResult = AuthResult.Success(SessionInfo("sess-login", "+50255550000"))
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        assertEquals("secret123", repo.lastRegisterPassword)
        assertEquals("secret123", repo.lastLoginPassword)
    }

    @Test
    fun `registerOrLogin with duplicate and login failure shows error`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Duplicate(),
            loginResult = AuthResult.Network(Exception("timeout"))
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.onboardingDone)
        assertNotNull(state.error)
        assertEquals("Network error. Please check your connection and try again.", state.error)

        val stored = store.read()
        assertEquals(false, stored.onboardingDone)
    }

    @Test
    fun `registerOrLogin with duplicate and login server error shows error`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Duplicate(),
            loginResult = AuthResult.Server(message = "Login service unavailable")
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.onboardingDone)
        assertNotNull(state.error)
        assertEquals("Login service unavailable", state.error)
        assertTrue(repo.loginCalled)

        val stored = store.read()
        assertEquals(false, stored.onboardingDone)
        assertNull(stored.sessionId)
    }

    @Test
    fun `registerOrLogin with network error shows error and does not mark done`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Network(Exception("no connection"))
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.onboardingDone)
        assertNotNull(state.error)

        val stored = store.read()
        assertEquals(false, stored.onboardingDone)
        assertNull(stored.sessionId)
    }

    @Test
    fun `registerOrLogin with server error shows error`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Server(message = "Internal server error")
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.onboardingDone)
        assertEquals("Internal server error", state.error)
    }

    @Test
    fun `registerOrLogin with validation error shows validation message`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Validation("Phone invalid")
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.onboardingDone)
        assertEquals("Phone invalid", state.error)
    }

    @Test
    fun `registerOrLogin with unauthorized error shows credentials message`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Unauthorized()
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.onboardingDone)
        assertEquals("Invalid credentials. Please check your phone and password.", state.error)
    }

    @Test
    fun `clearError removes error from state`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(registerResult = AuthResult.Network(Exception("fail")))
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `reportSessionExpired sets session expired error text`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository()
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.reportSessionExpired()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("La sesión ha expirado, iniciá sesión de nuevo", state.error)
    }

    @Test
    fun `reportSessionExpired does not mutate session or onboarding state`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.write(SessionSnapshot(onboardingDone = true, phone = "+50255550000", sessionId = "sess-001"))
        val repo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.bootstrap()
        advanceUntilIdle()

        viewModel.reportSessionExpired()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("sess-001", state.session?.sessionId)
        assertEquals(true, state.onboardingDone)
        assertEquals(false, state.isLoading)
        assertEquals("La sesión ha expirado, iniciá sesión de nuevo", state.error)
    }

    @Test
    fun `retry after network error succeeds`() = runTest(testDispatcher) {
        val store = SessionLocalStore()
        store.clearSession()
        val repo = FakeAuthRepository(
            registerResult = AuthResult.Network(Exception("fail"))
        )
        val viewModel = AppSessionViewModel(repo, store)

        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        repo.registerResult = AuthResult.Success(SessionInfo("sess-retry", "+50255550000"))
        viewModel.registerOrLogin(phone = "+50255550000", password = "secret123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(true, state.onboardingDone)
        assertEquals("sess-retry", state.session?.sessionId)
    }

    // ========== Fakes ==========

    private class FakeAuthRepository(
        var registerResult: AuthResult<SessionInfo> = AuthResult.Success(SessionInfo("sid", "+50255550000")),
        var loginResult: AuthResult<SessionInfo> = AuthResult.Success(SessionInfo("sid", "+50255550000")),
        var restoredSession: SessionInfo? = null,
    ) : AuthRepository {
        var loginCalled: Boolean = false
        var lastRegisterPassword: String? = null
        var lastLoginPassword: String? = null

        override suspend fun register(phone: String, password: String): AuthResult<SessionInfo> {
            lastRegisterPassword = password
            return registerResult
        }
        override suspend fun login(phone: String, password: String): AuthResult<SessionInfo> {
            loginCalled = true
            lastLoginPassword = password
            return loginResult
        }
        override suspend fun restoreSession(): SessionInfo? = restoredSession
        override suspend fun clearSession() {}
        override suspend fun healthCheck(): AuthResult<SessionInfo> =
            AuthResult.NotFound(message = "Health check deferred (no stored password)")
    }
}
