package com.agrogem.app.data.session

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class SessionSnapshotTest {

    @Test
    fun `default snapshot has all nulls and false`() {
        val snapshot = SessionSnapshot()
        assertEquals(false, snapshot.onboardingDone)
        assertNull(snapshot.phone)
        assertNull(snapshot.sessionId)
    }

    @Test
    fun `snapshot holds provided values`() {
        val snapshot = SessionSnapshot(
            onboardingDone = true,
            phone = "+50255550000",
            sessionId = "abc-123"
        )
        assertEquals(true, snapshot.onboardingDone)
        assertEquals("+50255550000", snapshot.phone)
        assertEquals("abc-123", snapshot.sessionId)
    }

    @Test
    fun `snapshots with same values are equal`() {
        val a = SessionSnapshot(onboardingDone = true, phone = "+1", sessionId = "x")
        val b = SessionSnapshot(onboardingDone = true, phone = "+1", sessionId = "x")
        assertEquals(a, b)
    }

    @Test
    fun `snapshots with different values are not equal`() {
        val a = SessionSnapshot(onboardingDone = true, phone = "+1", sessionId = "x")
        val b = SessionSnapshot(onboardingDone = false, phone = "+1", sessionId = "x")
        assertNotEquals(a, b)
    }

    @Test
    fun `copy changes single field`() {
        val original = SessionSnapshot(onboardingDone = true, phone = "+1", sessionId = "x")
        val copied = original.copy(sessionId = "y")
        assertEquals("y", copied.sessionId)
        assertEquals("+1", copied.phone)
        assertEquals(true, copied.onboardingDone)
    }
}

class SessionLocalStoreContractTest {

    @Test
    fun `read returns default snapshot when nothing written`() = runTest {
        val store = SessionLocalStore()
        store.clearSession()
        val result = store.read()
        assertEquals(SessionSnapshot(), result)
    }

    @Test
    fun `write then read returns same snapshot`() = runTest {
        val store = SessionLocalStore()
        val expected = SessionSnapshot(
            onboardingDone = true,
            phone = "+50255550000",
            sessionId = "session-abc"
        )
        store.write(expected)
        val result = store.read()
        assertEquals(expected, result)
    }

    @Test
    fun `clearSession resets to defaults`() = runTest {
        val store = SessionLocalStore()
        store.write(SessionSnapshot(onboardingDone = true, phone = "+1", sessionId = "x"))
        store.clearSession()
        val result = store.read()
        assertEquals(SessionSnapshot(), result)
    }

    @Test
    fun `overwrite replaces previous snapshot`() = runTest {
        val store = SessionLocalStore()
        store.write(SessionSnapshot(onboardingDone = true, phone = "+1", sessionId = "x"))
        store.write(SessionSnapshot(onboardingDone = false, phone = "+2", sessionId = "y"))
        val result = store.read()
        assertEquals("+2", result.phone)
        assertEquals("y", result.sessionId)
        assertEquals(false, result.onboardingDone)
    }
}
