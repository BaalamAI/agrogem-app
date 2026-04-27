package com.agrogem.app.data.chat.domain

import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.auth.domain.AuthResult
import com.agrogem.app.data.auth.domain.SessionInfo
import com.agrogem.app.data.chat.api.ChatApi
import com.agrogem.app.data.chat.api.ChatConversationDto
import com.agrogem.app.data.chat.api.MessageDto
import com.agrogem.app.data.network.ApiError
import com.agrogem.app.ui.screens.chat.ChatAttachment
import com.agrogem.app.ui.screens.chat.ChatMessage
import com.agrogem.app.ui.screens.chat.ChatMode
import com.agrogem.app.ui.screens.chat.MessageSender
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatRepositoryTest {

    // ========== Success Path ==========

    @Test
    fun `sendMessage maps backend conversation to domain messages`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val chatApi = FakeChatApi(
            result = ChatConversationDto(
                id = "+50255550000",
                messages = listOf(
                    MessageDto(role = "user", content = "Hola", createdAt = "2026-04-27T10:00:00Z"),
                    MessageDto(role = "assistant", content = "Hola, ¿en qué puedo ayudarte?", createdAt = "2026-04-27T10:00:01Z"),
                ),
                createdAt = "2026-04-27T09:00:00Z",
                updatedAt = "2026-04-27T10:00:01Z",
            )
        )
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        val result = repo.sendMessage(text = "Hola", attachments = emptyList(), mode = ChatMode.Blank)

        assertIs<ChatSendResult.Success>(result)
        assertEquals("+50255550000", result.conversationId)
        assertEquals(2, result.messages.size)

        val userMessage = result.messages[0]
        assertEquals("Hola", userMessage.text)
        assertEquals(MessageSender.User, userMessage.sender)
        assertTrue(userMessage.timestamp > 0)

        val assistantMessage = result.messages[1]
        assertEquals("Hola, ¿en qué puedo ayudarte?", assistantMessage.text)
        assertEquals(MessageSender.Assistant, assistantMessage.sender)
        assertTrue(assistantMessage.timestamp > userMessage.timestamp)
    }

    @Test
    fun `sendMessage passes session id and text to api`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-abc", "+50255550000"))
        val chatApi = FakeChatApi(
            result = ChatConversationDto(
                id = "+50255550000",
                messages = listOf(MessageDto(role = "user", content = "Test", createdAt = "2026-04-27T10:00:00Z")),
                createdAt = "2026-04-27T09:00:00Z",
                updatedAt = "2026-04-27T10:00:00Z",
            )
        )
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        repo.sendMessage(text = "Test message", attachments = emptyList(), mode = ChatMode.Blank)

        assertEquals("sess-abc", chatApi.lastSessionId)
        assertEquals("Test message", chatApi.lastContent)
    }

    @Test
    fun `sendMessage with analysis seeded mode still calls api`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val chatApi = FakeChatApi(
            result = ChatConversationDto(
                id = "+50255550000",
                messages = listOf(MessageDto(role = "assistant", content = "Reply", createdAt = "2026-04-27T10:00:00Z")),
                createdAt = "2026-04-27T09:00:00Z",
                updatedAt = "2026-04-27T10:00:00Z",
            )
        )
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)
        val mode = ChatMode.AnalysisSeeded("analysis-1", mockDiagnosis())

        val result = repo.sendMessage(text = "question", attachments = emptyList(), mode = mode)

        assertIs<ChatSendResult.Success>(result)
        assertEquals(1, result.messages.size)
    }

    // ========== Failure Paths ==========

    @Test
    fun `sendMessage returns SessionExpired when no stored session`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = null)
        val chatApi = FakeChatApi(result = null) // should not be called
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        val result = repo.sendMessage(text = "Hola", attachments = emptyList(), mode = ChatMode.Blank)

        assertIs<ChatSendResult.Failure>(result)
        assertIs<ChatFailure.SessionExpired>(result.reason)
        assertEquals(false, chatApi.wasCalled)
    }

    @Test
    fun `sendMessage returns SessionExpired when api throws NotFound`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val chatApi = FakeChatApi(error = ApiError.NotFound)
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        val result = repo.sendMessage(text = "Hola", attachments = emptyList(), mode = ChatMode.Blank)

        assertIs<ChatSendResult.Failure>(result)
        assertIs<ChatFailure.SessionExpired>(result.reason)
    }

    @Test
    fun `sendMessage returns Network when api throws NetworkError`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val chatApi = FakeChatApi(error = ApiError.NetworkError(cause = Exception("timeout")))
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        val result = repo.sendMessage(text = "Hola", attachments = emptyList(), mode = ChatMode.Blank)

        assertIs<ChatSendResult.Failure>(result)
        assertIs<ChatFailure.Network>(result.reason)
        assertEquals("timeout", (result.reason as ChatFailure.Network).cause.message)
    }

    @Test
    fun `sendMessage returns Server when api throws ServerError`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val chatApi = FakeChatApi(error = ApiError.ServerError)
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        val result = repo.sendMessage(text = "Hola", attachments = emptyList(), mode = ChatMode.Blank)

        assertIs<ChatSendResult.Failure>(result)
        assertIs<ChatFailure.Server>(result.reason)
    }

    @Test
    fun `sendMessage returns Network on generic exception`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val chatApi = FakeChatApi(error = RuntimeException("crash"))
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        val result = repo.sendMessage(text = "Hola", attachments = emptyList(), mode = ChatMode.Blank)

        assertIs<ChatSendResult.Failure>(result)
        assertIs<ChatFailure.Network>(result.reason)
        assertEquals("crash", (result.reason as ChatFailure.Network).cause.message)
    }

    @Test
    fun `sendMessage maps unknown role to Assistant`() = runTest {
        val authRepo = FakeAuthRepository(restoredSession = SessionInfo("sess-001", "+50255550000"))
        val chatApi = FakeChatApi(
            result = ChatConversationDto(
                id = "+50255550000",
                messages = listOf(
                    MessageDto(role = "system", content = "System msg", createdAt = "2026-04-27T10:00:00Z"),
                ),
                createdAt = "2026-04-27T09:00:00Z",
                updatedAt = "2026-04-27T10:00:00Z",
            )
        )
        val repo = ChatRepositoryImpl(api = chatApi, authRepository = authRepo)

        val result = repo.sendMessage(text = "Hola", attachments = emptyList(), mode = ChatMode.Blank)

        assertIs<ChatSendResult.Success>(result)
        assertEquals(MessageSender.Assistant, result.messages[0].sender)
    }

    // ========== Fakes ==========

    private class FakeAuthRepository(
        var restoredSession: SessionInfo? = null,
    ) : AuthRepository {
        override suspend fun register(phone: String, password: String): AuthResult<SessionInfo> =
            AuthResult.Success(SessionInfo("sid", phone))
        override suspend fun login(phone: String, password: String): AuthResult<SessionInfo> =
            AuthResult.Success(SessionInfo("sid", phone))
        override suspend fun restoreSession(): SessionInfo? = restoredSession
        override suspend fun clearSession() {}
        override suspend fun healthCheck(): AuthResult<SessionInfo> =
            AuthResult.NotFound(message = "Health check deferred (no stored password)")
    }

    private class FakeChatApi(
        var result: ChatConversationDto? = null,
        var error: Throwable? = null,
    ) : ChatApi {
        var wasCalled: Boolean = false
        var lastSessionId: String? = null
        var lastContent: String? = null

        override suspend fun sendMessage(sessionId: String, content: String): ChatConversationDto {
            wasCalled = true
            lastSessionId = sessionId
            lastContent = content
            if (error != null) throw error!!
            return result!!
        }
    }

    private fun mockDiagnosis() = com.agrogem.app.ui.screens.analysis.DiagnosisResult(
        pestName = "Roya",
        confidence = 0.92f,
        severity = "Alta",
        affectedArea = "Hojas",
        cause = "Hongo",
        diagnosisText = "Infección detectada",
        treatmentSteps = listOf("Aplicar fungicida"),
    )
}
