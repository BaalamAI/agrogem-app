package com.agrogem.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Phase 6.5 — Route coverage for Chat route with/without analysisId param.
 * Verifies backward compatibility and seeded-initialization routing.
 */
class ChatRouteTest {

    @Test
    fun `Chat createRoute without analysisId returns base route`() {
        val route = AgroGemRoute.Chat.createRoute(null)
        assertEquals("chat", route)
    }

    @Test
    fun `Chat createRoute with analysisId includes query param`() {
        val route = AgroGemRoute.Chat.createRoute("analysis_abc123")
        assertEquals("chat?analysisId=analysis_abc123", route)
    }

    @Test
    fun `Chat createRoute with empty analysisId uses base route`() {
        val route = AgroGemRoute.Chat.createRoute("")
        assertEquals("chat", route)
    }

    @Test
    fun `Chat createRoute with blank analysisId uses base route`() {
        val route = AgroGemRoute.Chat.createRoute("   ")
        assertEquals("chat", route)
    }

    @Test
    fun `Chat BASE_ROUTE constant is chat without params`() {
        assertEquals("chat", AgroGemRoute.Chat.BASE_ROUTE)
    }

    @Test
    fun `Chat NAV_ROUTE constant defines optional analysisId query`() {
        assertEquals("chat?analysisId={analysisId}", AgroGemRoute.Chat.NAV_ROUTE)
    }

    @Test
    fun `fromRoute with chat base route returns Chat route`() {
        val result = AgroGemRoute.fromRoute("chat")
        assertEquals(AgroGemRoute.Chat, result)
    }

    @Test
    fun `fromRoute with chat query param returns Chat route`() {
        val result = AgroGemRoute.fromRoute("chat?analysisId=xyz")
        assertEquals(AgroGemRoute.Chat, result)
    }

    @Test
    fun `fromRoute preserves other routes unaffected`() {
        assertEquals(AgroGemRoute.Analysis, AgroGemRoute.fromRoute("analysis"))
        assertEquals(AgroGemRoute.Diagnosis, AgroGemRoute.fromRoute("diagnosis"))
        assertEquals(AgroGemRoute.VoiceReady, AgroGemRoute.fromRoute("voice_ready"))
    }

    @Test
    fun `fromRoute strips query params before matching`() {
        // Verify that chat route with any query param still matches Chat
        assertEquals(AgroGemRoute.Chat, AgroGemRoute.fromRoute("chat?analysisId=123&other=456"))
        assertEquals(AgroGemRoute.Chat, AgroGemRoute.fromRoute("chat?analysisId="))
        assertEquals(AgroGemRoute.Chat, AgroGemRoute.fromRoute("chat?"))
    }
}
