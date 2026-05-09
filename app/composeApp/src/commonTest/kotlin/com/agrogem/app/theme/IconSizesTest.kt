package com.agrogem.app.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.unit.dp

class IconSizesTest {

    @Test
    fun `icon sizes have correct dp values`() {
        assertEquals(22.dp, AgroGemIconSizes.Sm)
        assertEquals(28.dp, AgroGemIconSizes.Md)
        assertEquals(36.dp, AgroGemIconSizes.Lg)
    }

    @Test
    fun `small is smaller than medium`() {
        assertEquals(true, AgroGemIconSizes.Sm < AgroGemIconSizes.Md)
    }

    @Test
    fun `medium is smaller than large`() {
        assertEquals(true, AgroGemIconSizes.Md < AgroGemIconSizes.Lg)
    }
}
