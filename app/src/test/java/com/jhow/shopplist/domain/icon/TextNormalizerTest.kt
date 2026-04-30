package com.jhow.shopplist.domain.icon

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    private val normalizer = DefaultTextNormalizer()

    // ── Trim + lowercase ──────────────────────────────────────────────────────

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
    fun `single character is lowercased`() {
        assertEquals("a", normalizer.normalize("A"))
    }

    // ── NFD diacritic stripping ───────────────────────────────────────────────

    @Test
    fun `strips cedilla from maca`() {
        assertEquals("maca", normalizer.normalize("maçã"))
    }

    @Test
    fun `strips acute accent from cafe`() {
        assertEquals("cafe", normalizer.normalize("café"))
    }

    @Test
    fun `strips tilde from feijao`() {
        assertEquals("feijao", normalizer.normalize("feijão"))
    }

    @Test
    fun `strips circumflex from pao`() {
        assertEquals("pao", normalizer.normalize("pão"))
    }

    @Test
    fun `strips accent from acucar`() {
        assertEquals("acucar", normalizer.normalize("açúcar"))
    }

    @Test
    fun `term without diacritics is unchanged`() {
        assertEquals("cenoura", normalizer.normalize("cenoura"))
    }

    @Test
    fun `mixed diacritics and plain chars stripped correctly`() {
        assertEquals("maca verde", normalizer.normalize("maçã verde"))
    }

    // ── Quantity and unit token removal ───────────────────────────────────────

    @Test
    fun `strips leading quantity kg`() {
        assertEquals("arroz", normalizer.normalize("2kg arroz"))
    }

    @Test
    fun `strips leading quantity ml`() {
        assertEquals("leite", normalizer.normalize("500ml leite"))
    }

    @Test
    fun `strips trailing quantity kg`() {
        assertEquals("arroz", normalizer.normalize("arroz 2kg"))
    }

    @Test
    fun `strips quantity g`() {
        assertEquals("queijo", normalizer.normalize("100g queijo"))
    }

    @Test
    fun `strips multiplier x`() {
        assertEquals("banana", normalizer.normalize("2x banana"))
    }

    @Test
    fun `strips standalone pacote`() {
        assertEquals("macarrao", normalizer.normalize("pacote macarrao"))
    }

    @Test
    fun `strips standalone pack`() {
        assertEquals("chips", normalizer.normalize("pack chips"))
    }

    @Test
    fun `strips quantity from multi-word item leaving rest intact`() {
        assertEquals("arroz integral", normalizer.normalize("2kg arroz integral"))
    }

    @Test
    fun `2kg arroz integral and bare arroz normalize to same prefix`() {
        val withQty = normalizer.normalize("2kg arroz integral")
        val bare = normalizer.normalize("arroz")
        assertEquals("arroz", bare)
        assertEquals(true, withQty.startsWith(bare))
    }

    // ── Stopword removal ──────────────────────────────────────────────────────

    @Test
    fun `removes PT preposition do`() {
        assertEquals("leite campo", normalizer.normalize("leite do campo"))
    }

    @Test
    fun `removes PT preposition de`() {
        assertEquals("iogurte morango", normalizer.normalize("iogurte de morango"))
    }

    @Test
    fun `removes PT preposition da`() {
        assertEquals("pao vovo", normalizer.normalize("pao da vovo"))
    }

    @Test
    fun `removes PT conjunction com`() {
        assertEquals("feijao arroz", normalizer.normalize("feijao com arroz"))
    }

    @Test
    fun `removes EN preposition of`() {
        assertEquals("bread life", normalizer.normalize("bread of life"))
    }

    @Test
    fun `removes EN preposition the`() {
        assertEquals("best cheese", normalizer.normalize("the best cheese"))
    }

    @Test
    fun `removes stopwords and strips diacritics combined`() {
        assertEquals("pao queijo", normalizer.normalize("pão de queijo"))
    }

    @Test
    fun `removes quantity and stopword combined`() {
        assertEquals("arroz feijao", normalizer.normalize("2kg arroz com feijao"))
    }

    // ── Decimal and space-separated quantity formats ───────────────────────────

    @Test
    fun `strips decimal dot quantity`() {
        assertEquals("arroz", normalizer.normalize("1.5kg arroz"))
    }

    @Test
    fun `strips decimal comma quantity (PT-BR style)`() {
        assertEquals("leite", normalizer.normalize("1,5l leite"))
    }

    @Test
    fun `strips space-separated number and unit`() {
        // "500 ml leite" → "500" is a bare number token, "ml" is a standalone unit
        assertEquals("leite", normalizer.normalize("500 ml leite"))
    }

    @Test
    fun `strips standalone unit ml`() {
        assertEquals("leite", normalizer.normalize("leite ml"))
    }

    // ── Space collapsing ──────────────────────────────────────────────────────

    @Test
    fun `collapses multiple internal spaces`() {
        assertEquals("arroz branco", normalizer.normalize("arroz  branco"))
    }
}
