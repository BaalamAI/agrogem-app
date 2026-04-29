package com.agrogem.app.ui.screens.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeScreenLabelFormatTest {

    @Test
    fun `normalizeLocationRegionLabel removes known prefixes`() {
        assertEquals("Guatemala", normalizeLocationRegionLabel("Departamento de Guatemala"))
        assertEquals("Jalisco", normalizeLocationRegionLabel("Estado de Jalisco"))
    }

    @Test
    fun `normalizeLocationRegionLabel keeps raw value when no prefix`() {
        assertEquals("San Salvador", normalizeLocationRegionLabel("San Salvador"))
    }
}
