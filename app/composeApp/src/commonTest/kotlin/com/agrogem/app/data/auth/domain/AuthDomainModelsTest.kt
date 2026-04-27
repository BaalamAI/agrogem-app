package com.agrogem.app.data.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class SessionInfoTest {

    @Test
    fun `session info holds session id and phone`() {
        val info = SessionInfo(sessionId = "sess-abc", phone = "+50255550000")
        assertEquals("sess-abc", info.sessionId)
        assertEquals("+50255550000", info.phone)
    }

    @Test
    fun `session info equality based on values`() {
        val a = SessionInfo(sessionId = "x", phone = "+1")
        val b = SessionInfo(sessionId = "x", phone = "+1")
        val c = SessionInfo(sessionId = "y", phone = "+1")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}

class AuthResultTest {

    @Test
    fun `success holds data`() {
        val data = SessionInfo(sessionId = "s1", phone = "+502")
        val result: AuthResult<SessionInfo> = AuthResult.Success(data)
        assertIs<AuthResult.Success<SessionInfo>>(result)
        assertEquals("s1", result.data.sessionId)
    }

    @Test
    fun `duplicate is a failure subtype`() {
        val result: AuthResult<SessionInfo> = AuthResult.Duplicate()
        assertIs<AuthResult.Duplicate>(result)
    }

    @Test
    fun `not found is a failure subtype`() {
        val result: AuthResult<SessionInfo> = AuthResult.NotFound()
        assertIs<AuthResult.NotFound>(result)
    }

    @Test
    fun `server is a failure subtype`() {
        val result: AuthResult<SessionInfo> = AuthResult.Server()
        assertIs<AuthResult.Server>(result)
    }

    @Test
    fun `validation is a failure subtype`() {
        val result: AuthResult<SessionInfo> = AuthResult.Validation("Invalid input")
        assertIs<AuthResult.Validation>(result)
        assertEquals("Invalid input", result.message)
    }

    @Test
    fun `validation holds empty message`() {
        val result: AuthResult<SessionInfo> = AuthResult.Validation("")
        assertIs<AuthResult.Validation>(result)
        assertEquals("", result.message)
    }

    @Test
    fun `unauthorized is a failure subtype`() {
        val result: AuthResult<SessionInfo> = AuthResult.Unauthorized()
        assertIs<AuthResult.Unauthorized>(result)
    }

    @Test
    fun `unauthorized holds custom message`() {
        val result: AuthResult<SessionInfo> = AuthResult.Unauthorized("Session expired")
        assertIs<AuthResult.Unauthorized>(result)
        assertEquals("Session expired", result.message)
    }

    @Test
    fun `network holds cause`() {
        val cause = RuntimeException("timeout")
        val result: AuthResult<SessionInfo> = AuthResult.Network(cause)
        assertIs<AuthResult.Network>(result)
        assertEquals("timeout", result.cause.message)
    }

    @Test
    fun `success and failure are not equal`() {
        val success: AuthResult<SessionInfo> = AuthResult.Success(SessionInfo("s1", "+502"))
        val failure: AuthResult<SessionInfo> = AuthResult.Duplicate()
        assertNotEquals(success, failure)
    }
}
