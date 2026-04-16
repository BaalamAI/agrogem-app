package com.agrogem.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RoutesTest {

    @Test
    fun `all routes are registered`() {
        assertEquals(11, AgroGemRoute.all.size)
        assertEquals(
            listOf(
                "home",
                "camera",
                "analysis",
                "diagnosis",
                "treatment_plan",
                "treatment_products",
                "conversation_summary",
                "chat",
                "chat_confirm",
                "history",
                "voice_ready",
            ),
            AgroGemRoute.all.map { it.route },
        )
    }

    @Test
    fun `fromRoute returns home as safe fallback`() {
        assertSame(AgroGemRoute.Home, AgroGemRoute.fromRoute("unknown"))
        assertSame(AgroGemRoute.Home, AgroGemRoute.fromRoute(null))
    }
}
