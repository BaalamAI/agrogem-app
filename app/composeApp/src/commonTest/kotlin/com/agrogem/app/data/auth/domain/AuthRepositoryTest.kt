package com.agrogem.app.data.auth.domain

import com.agrogem.app.data.auth.api.AuthApi
import com.agrogem.app.data.auth.api.KtorAuthApi
import com.agrogem.app.data.auth.api.SessionResponse
import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.network.HttpClientFactory
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.session.SessionSnapshot
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthRepositoryTest {

    private lateinit var store: SessionLocalStore

    @BeforeTest
    fun setup() = runTest {
        store = SessionLocalStore()
        store.clearSession()
    }

    private fun repositoryWithEngine(engine: MockEngine): AuthRepository {
        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = engine))
        return AuthRepositoryImpl(api = api, store = store)
    }

    @Test
    fun `register succeeds and persists session info`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"session_id":"sess-reg-001"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.register(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Success<SessionInfo>>(result)
        assertEquals("sess-reg-001", result.data.sessionId)
        assertEquals("+50255550000", result.data.phone)

        val snapshot = store.read()
        assertEquals("sess-reg-001", snapshot.sessionId)
        assertEquals("+50255550000", snapshot.phone)
    }

    @Test
    fun `login succeeds and persists session info`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"session_id":"sess-login-002"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.login(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Success<SessionInfo>>(result)
        assertEquals("sess-login-002", result.data.sessionId)
        assertEquals("+50255550000", result.data.phone)

        val snapshot = store.read()
        assertEquals("sess-login-002", snapshot.sessionId)
        assertEquals("+50255550000", snapshot.phone)
    }

    @Test
    fun `register returns duplicate when api returns 409`() = runTest {
        val engine = MockEngine {
            respondError(status = HttpStatusCode.Conflict)
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.register(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Duplicate>(result)
    }

    @Test
    fun `login returns not found when api returns 404`() = runTest {
        val engine = MockEngine {
            respondError(status = HttpStatusCode.NotFound)
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.login(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.NotFound>(result)
    }

    @Test
    fun `register returns validation when api returns 422`() = runTest {
        val engine = MockEngine {
            respond(
                content = "Phone invalid",
                status = HttpStatusCode.UnprocessableEntity,
            )
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.register(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Validation>(result)
        assertEquals("Phone invalid", result.message)
    }

    @Test
    fun `login returns unauthorized when api returns 401`() = runTest {
        val engine = MockEngine {
            respondError(status = HttpStatusCode.Unauthorized)
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.login(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Unauthorized>(result)
    }

    @Test
    fun `register returns server error when api returns 500`() = runTest {
        val engine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.register(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Server>(result)
    }

    @Test
    fun `login returns server error when api returns 500`() = runTest {
        val engine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.login(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Server>(result)
    }

    @Test
    fun `login returns network error when api throws network exception`() = runTest {
        val engine = MockEngine {
            throw RuntimeException("timeout")
        }
        val repo = repositoryWithEngine(engine)

        val result = repo.login(phone = "+50255550000", password = "secret123")

        assertIs<AuthResult.Network>(result)
        assertEquals("timeout", result.cause.message)
    }

    @Test
    fun `restore session returns session info when store has data`() = runTest {
        store.write(SessionSnapshot(sessionId = "sess-old", phone = "+50255550000", onboardingDone = true))
        val repo = repositoryWithEngine(MockEngine { respondError(HttpStatusCode.OK) })

        val result = repo.restoreSession()

        assertNotNull(result)
        assertEquals("sess-old", result.sessionId)
        assertEquals("+50255550000", result.phone)
    }

    @Test
    fun `restore session returns null when store is empty`() = runTest {
        val repo = repositoryWithEngine(MockEngine { respondError(HttpStatusCode.OK) })

        val result = repo.restoreSession()

        assertNull(result)
    }

    @Test
    fun `restore session returns null when session id is missing`() = runTest {
        store.write(SessionSnapshot(sessionId = null, phone = "+50255550000", onboardingDone = true))
        val repo = repositoryWithEngine(MockEngine { respondError(HttpStatusCode.OK) })

        val result = repo.restoreSession()

        assertNull(result)
    }

    @Test
    fun `restore session returns null when phone is missing`() = runTest {
        store.write(SessionSnapshot(sessionId = "sess-old", phone = null, onboardingDone = true))
        val repo = repositoryWithEngine(MockEngine { respondError(HttpStatusCode.OK) })

        val result = repo.restoreSession()

        assertNull(result)
    }

    @Test
    fun `clear session removes stored data`() = runTest {
        store.write(SessionSnapshot(sessionId = "x", phone = "+1", onboardingDone = true))
        val repo = repositoryWithEngine(MockEngine { respondError(HttpStatusCode.OK) })

        repo.clearSession()

        val snapshot = store.read()
        assertNull(snapshot.sessionId)
        assertNull(snapshot.phone)
        assertEquals(false, snapshot.onboardingDone)
    }

    @Test
    fun `health check returns deferred not found`() = runTest {
        store.write(SessionSnapshot(sessionId = "sess-old", phone = "+50255550000", onboardingDone = true))
        val repo = repositoryWithEngine(MockEngine { respondError(HttpStatusCode.OK) })

        val result = repo.healthCheck()

        assertIs<AuthResult.NotFound>(result)
        assertEquals("Health check deferred (no stored password)", result.message)
    }

    @Test
    fun `health check returns deferred not found when no phone stored`() = runTest {
        val repo = repositoryWithEngine(MockEngine { respondError(HttpStatusCode.OK) })

        val result = repo.healthCheck()

        assertIs<AuthResult.NotFound>(result)
        assertEquals("Health check deferred (no stored password)", result.message)
    }
}
