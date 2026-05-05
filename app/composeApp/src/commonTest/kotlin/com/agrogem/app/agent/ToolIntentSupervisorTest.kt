package com.agrogem.app.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolIntentSupervisorTest {

    @Test
    fun `weather question expects weather tool`() {
        val expected = ToolIntentSupervisor.expectedToolsFor("¿Va a llover hoy?")

        assertEquals(setOf(ToolIntentSupervisor.WEATHER_TOOL), expected)
    }

    @Test
    fun `soil question expects soil tool`() {
        val expected = ToolIntentSupervisor.expectedToolsFor("¿Cómo está el pH del suelo?")

        assertEquals(setOf(ToolIntentSupervisor.SOIL_TOOL), expected)
    }

    @Test
    fun `pest question expects pest tool`() {
        val expected = ToolIntentSupervisor.expectedToolsFor("¿Hay riesgo de plagas?")

        assertEquals(setOf(ToolIntentSupervisor.PEST_TOOL), expected)
    }

    @Test
    fun `general crop question does not expect tools`() {
        val expected = ToolIntentSupervisor.expectedToolsFor("¿Cómo puedo mejorar mi manejo del maíz?")

        assertEquals(emptySet(), expected)
    }

    @Test
    fun `supervisor retries only when no expected tool was called`() {
        assertTrue(
            ToolIntentSupervisor.shouldRetry(
                expectedTools = setOf(ToolIntentSupervisor.WEATHER_TOOL),
                calledTools = emptySet(),
            )
        )
        assertFalse(
            ToolIntentSupervisor.shouldRetry(
                expectedTools = setOf(ToolIntentSupervisor.WEATHER_TOOL),
                calledTools = setOf(ToolIntentSupervisor.WEATHER_TOOL),
            )
        )
        assertFalse(
            ToolIntentSupervisor.shouldRetry(
                expectedTools = emptySet(),
                calledTools = emptySet(),
            )
        )
    }
}
