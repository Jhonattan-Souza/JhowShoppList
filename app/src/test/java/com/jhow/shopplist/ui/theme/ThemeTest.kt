package com.jhow.shopplist.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun `light color scheme has MossGreen secondaryContainer and Cream onSecondaryContainer`() {
        val scheme = lightAppColorScheme()
        assertEquals(MossGreen, scheme.secondaryContainer)
        assertEquals(Cream, scheme.onSecondaryContainer)
    }

    @Test
    fun `dark color scheme has MossGreen secondaryContainer and Cream onSecondaryContainer`() {
        val scheme = darkAppColorScheme()
        assertEquals(MossGreen, scheme.secondaryContainer)
        assertEquals(Cream, scheme.onSecondaryContainer)
    }

    @Test
    fun `light swipe delete inactive colors keep label visible`() {
        val scheme = lightAppColorScheme()

        assertEquals(Fog, scheme.surfaceContainerHighest)
        assertEquals(Slate, scheme.onSurfaceVariant)
    }
}
