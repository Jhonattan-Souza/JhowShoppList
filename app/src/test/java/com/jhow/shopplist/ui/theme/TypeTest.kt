package com.jhow.shopplist.ui.theme

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TypeTest {

    @Test
    fun `appFontFamily is not the default sans-serif`() {
        assertNotEquals(FontFamily.SansSerif, appFontFamily())
    }
}
