package com.agrogem.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for RoundIconButton helper logic.
 */
class RoundIconButtonTest {

    @Test
    fun `resolveIconContentDescription returns explicit when provided`() {
        assertEquals(
            "Notifications",
            resolveIconContentDescription(label = "🔔", explicit = "Notifications"),
        )
    }

    @Test
    fun `resolveIconContentDescription falls back to label when explicit is null`() {
        assertEquals(
            "Send",
            resolveIconContentDescription(label = "Send", explicit = null),
        )
    }

    @Test
    fun `resolveIconContentDescription prefers explicit over label`() {
        assertEquals(
            "Back",
            resolveIconContentDescription(label = "‹", explicit = "Back"),
        )
    }
}
