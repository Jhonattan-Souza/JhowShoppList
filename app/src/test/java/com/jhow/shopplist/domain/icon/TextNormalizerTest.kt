package com.jhow.shopplist.domain.icon

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    private val normalizer = DefaultTextNormalizer()

    @Test
    fun `lowercases simple term`() {
        assertEquals("leite", normalizer.normalize("Leite"))
    }

    @Test
    fun `trims leading and trailing whitespace`() {
        assertEquals("leite", normalizer.normalize("  leite  "))
    }

    @Test
    fun `trims and lowercases combined`() {
        assertEquals("leite integral", normalizer.normalize("  Leite Integral  "))
    }

    @Test
    fun `empty string returns empty string`() {
        assertEquals("", normalizer.normalize(""))
    }

    @Test
    fun `whitespace-only string returns empty string`() {
        assertEquals("", normalizer.normalize("   "))
    }

    @Test
    fun `already normalized term stays unchanged`() {
        assertEquals("arroz", normalizer.normalize("arroz"))
    }

    @Test
    fun `mixed casing is fully lowercased`() {
        assertEquals("coca-cola", normalizer.normalize("CoCa-CoLa"))
    }

    @Test
    fun `internal whitespace is preserved`() {
        assertEquals("pao de queijo", normalizer.normalize("Pao de Queijo"))
    }

    @Test
    fun `single character is lowercased`() {
        assertEquals("a", normalizer.normalize("A"))
    }

    @Test
    fun `numeric characters are preserved`() {
        assertEquals("2kg arroz", normalizer.normalize("2kg Arroz"))
    }
}
