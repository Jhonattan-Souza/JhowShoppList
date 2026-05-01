package com.jhow.shopplist.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jhow.shopplist.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TypeTest {

    @Test
    fun `appFontFamily is not the default sans-serif`() {
        assertNotEquals(FontFamily.SansSerif, appFontFamily())
    }

    @Test
    fun `appFontFamily uses bundled nunito resources for required weights`() {
        val expected = FontFamily(
            Font(resId = R.font.nunito_regular, weight = FontWeight.Normal),
            Font(resId = R.font.nunito_medium, weight = FontWeight.Medium),
            Font(resId = R.font.nunito_semibold, weight = FontWeight.SemiBold),
            Font(resId = R.font.nunito_bold, weight = FontWeight.Bold)
        )

        assertEquals(expected, appFontFamily())
    }
}
