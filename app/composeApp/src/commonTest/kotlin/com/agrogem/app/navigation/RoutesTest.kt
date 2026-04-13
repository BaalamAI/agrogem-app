package com.agrogem.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RoutesTest {

    @Test
    fun `all routes are registered`() {
        assertEquals(5, AgroGemRoute.all.size)
        assertEquals(
            listOf("dashboard", "camera", "map", "analysis", "report"),
            AgroGemRoute.all.map { it.route },
        )
    }

    @Test
    fun `fromRoute returns dashboard as safe fallback`() {
        assertSame(AgroGemRoute.Dashboard, AgroGemRoute.fromRoute("unknown"))
        assertSame(AgroGemRoute.Dashboard, AgroGemRoute.fromRoute(null))
    }
}
