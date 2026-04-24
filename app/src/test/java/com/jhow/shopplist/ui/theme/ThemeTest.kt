package com.jhow.shopplist.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun `light color scheme has MossGreen secondaryContainer and Cream onSecondaryContainer`() {
        val scheme = lightColorScheme()
        assertEquals(MossGreen, scheme.secondaryContainer)
        assertEquals(Cream, scheme.onSecondaryContainer)
    }

    @Test
    fun `dark color scheme has MossGreen secondaryContainer and Cream onSecondaryContainer`() {
        val scheme = darkColorScheme()
        assertEquals(MossGreen, scheme.secondaryContainer)
        assertEquals(Cream, scheme.onSecondaryContainer)
    }
}
