package com.agrogem.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests verifying that BottomNavigationBar uses Res.drawable resources
 * instead of Unicode placeholder strings.
 *
 * These tests validate the icon mapping constants that drive the component.
 */
class BottomNavigationBarIconTest {

    @Test
    fun `home tab uses navigation home drawable`() {
        // Verify the mapping constant points to the correct resource name
        assertEquals("ic_navigation_home", BottomTabIcons.Home.resourceName)
    }

    @Test
    fun `fields tab uses navigation fields drawable`() {
        assertEquals("ic_navigation_fields", BottomTabIcons.Fields.resourceName)
    }

    @Test
    fun `maps tab uses navigation maps drawable`() {
        assertEquals("ic_navigation_maps", BottomTabIcons.Maps.resourceName)
    }

    @Test
    fun `chat tab uses navigation chat drawable`() {
        assertEquals("ic_navigation_chat", BottomTabIcons.Chat.resourceName)
    }

    @Test
    fun `scan tab uses navigation scan drawable`() {
        assertEquals("ic_navigation_scan", BottomTabIcons.Scan.resourceName)
    }

    @Test
    fun `all tab icons have unique resource names`() {
        val names = BottomTabIcons.entries.map { it.resourceName }
        assertEquals(names.size, names.distinct().size, "Duplicate icon resource names found")
    }
}
