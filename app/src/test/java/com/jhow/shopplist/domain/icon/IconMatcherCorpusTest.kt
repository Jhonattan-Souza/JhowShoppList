package com.jhow.shopplist.domain.icon

import com.jhow.shopplist.data.icon.parseDictionaryJson
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class IconMatcherCorpusTest {

    companion object {
        private lateinit var matcher: DefaultIconMatcher

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val normalizer = DefaultTextNormalizer()
            val ptJson = java.io.File("src/main/assets/icons/dictionary-pt.json").readText()
            val enJson = java.io.File("src/main/assets/icons/dictionary-en.json").readText()
            val ptDict = parseDictionaryJson(ptJson)
            val enDict = parseDictionaryJson(enJson)
            val merged = ptDict + enDict
            matcher = DefaultIconMatcher(merged, normalizer)
        }
    }

    // ── PT-BR fruit with diacritics ──────────────────────────────────────────────

    @Test
    fun `corpus - maçã resolves to fruit`() {
        assertEquals(IconBucket.FRUIT, matcher.match("maçã"))
    }

    @Test
    fun `corpus - morango resolves to fruit`() {
        assertEquals(IconBucket.FRUIT, matcher.match("morango"))
    }

    @Test
    fun `corpus - abacaxi resolves to fruit`() {
        assertEquals(IconBucket.FRUIT, matcher.match("abacaxi"))
    }

    // ── PT-BR regional terms that were previously generic ──────────────────────

    @Test
    fun `corpus - aipim resolves to vegetable`() {
        assertEquals(IconBucket.VEGETABLE, matcher.match("aipim"))
    }

    @Test
    fun `corpus - macaxeira resolves to vegetable`() {
        assertEquals(IconBucket.VEGETABLE, matcher.match("macaxeira"))
    }

    @Test
    fun `corpus - chuchu resolves to vegetable`() {
        assertEquals(IconBucket.VEGETABLE, matcher.match("chuchu"))
    }

    @Test
    fun `corpus - mandioca resolves to vegetable`() {
        assertEquals(IconBucket.VEGETABLE, matcher.match("mandioca"))
    }

    // ── PT-BR staples and condiments that were previously generic ──────────────

    @Test
    fun `corpus - sal resolves to staples`() {
        assertEquals(IconBucket.STAPLES, matcher.match("sal"))
    }

    @Test
    fun `corpus - pimenta resolves to condiments`() {
        assertEquals(IconBucket.CONDIMENTS, matcher.match("pimenta"))
    }

    @Test
    fun `corpus - pimenta do reino resolves to condiments via token match`() {
        assertEquals(IconBucket.CONDIMENTS, matcher.match("pimenta do reino"))
    }

    @Test
    fun `corpus - azeite resolves to condiments`() {
        assertEquals(IconBucket.CONDIMENTS, matcher.match("azeite"))
    }

    // ── PT-BR personal care and pet items ────────────────────────────────────────

    @Test
    fun `corpus - shampoo resolves to personal care`() {
        assertEquals(IconBucket.PERSONAL_CARE, matcher.match("shampoo"))
    }

    @Test
    fun `corpus - ração resolves to pet`() {
        assertEquals(IconBucket.PET, matcher.match("ração"))
    }

    @Test
    fun `corpus - ração Whiskas resolves to pet via token match`() {
        assertEquals(IconBucket.PET, matcher.match("ração Whiskas"))
    }

    // ── PT-BR items with quantities still resolve correctly ────────────────────

    @Test
    fun `corpus - 2kg arroz resolves to pantry`() {
        assertEquals(IconBucket.PANTRY, matcher.match("2kg arroz"))
    }

    @Test
    fun `corpus - 500ml leite resolves to dairy`() {
        assertEquals(IconBucket.DAIRY, matcher.match("500ml leite"))
    }

    @Test
    fun `corpus - 1kg feijão resolves to pantry`() {
        assertEquals(IconBucket.PANTRY, matcher.match("1kg feijão"))
    }

    // ── PT-BR dairy and cheese specific bucket ───────────────────────────────────

    @Test
    fun `corpus - queijo mussarela resolves to cheese not dairy`() {
        assertEquals(IconBucket.CHEESE, matcher.match("queijo mussarela"))
    }

    @Test
    fun `corpus - mussarela resolves to cheese`() {
        assertEquals(IconBucket.CHEESE, matcher.match("mussarela"))
    }

    // ── Mixed-language items ──────────────────────────────────────────────────────

    @Test
    fun `corpus - whey protein resolves to staples via token match`() {
        assertEquals(IconBucket.STAPLES, matcher.match("whey protein"))
    }

    @Test
    fun `corpus - coca-cola resolves to beverages cold`() {
        assertEquals(IconBucket.BEVERAGES_COLD, matcher.match("coca-cola"))
    }

    @Test
    fun `corpus - coca resolves to beverages cold`() {
        assertEquals(IconBucket.BEVERAGES_COLD, matcher.match("coca"))
    }

    // ── EN items resolve correctly ───────────────────────────────────────────────

    @Test
    fun `corpus - milk resolves to dairy`() {
        assertEquals(IconBucket.DAIRY, matcher.match("milk"))
    }

    @Test
    fun `corpus - bread resolves to bread`() {
        assertEquals(IconBucket.BREAD, matcher.match("bread"))
    }

    @Test
    fun `corpus - rice resolves to pantry`() {
        assertEquals(IconBucket.PANTRY, matcher.match("rice"))
    }

    // ── PT-BR grain items ────────────────────────────────────────────────────────

    @Test
    fun `corpus - polenta resolves to grain`() {
        assertEquals(IconBucket.GRAIN, matcher.match("polenta"))
    }

    @Test
    fun `corpus - aveia resolves to grain`() {
        assertEquals(IconBucket.GRAIN, matcher.match("aveia"))
    }

    // ── PT-BR cleaning items ─────────────────────────────────────────────────────

    @Test
    fun `corpus - detergente resolves to cleaning`() {
        assertEquals(IconBucket.CLEANING, matcher.match("detergente"))
    }

    @Test
    fun `corpus - sabão resolves to cleaning`() {
        assertEquals(IconBucket.CLEANING, matcher.match("sabão"))
    }

    // ── PT-BR baby items ─────────────────────────────────────────────────────────

    @Test
    fun `corpus - fralda resolves to baby`() {
        assertEquals(IconBucket.BABY, matcher.match("fralda"))
    }

    // ── PT-BR alcohol items ──────────────────────────────────────────────────────

    @Test
    fun `corpus - cerveja resolves to alcohol`() {
        assertEquals(IconBucket.ALCOHOL, matcher.match("cerveja"))
    }

    // ── PT-BR frozen items ──────────────────────────────────────────────────────

    @Test
    fun `corpus - sorvete resolves to frozen`() {
        assertEquals(IconBucket.FROZEN, matcher.match("sorvete"))
    }

    // ── PT-BR deli items ─────────────────────────────────────────────────────────

    @Test
    fun `corpus - presunto resolves to deli cold cuts`() {
        assertEquals(IconBucket.DELI_COLD_CUTS, matcher.match("presunto"))
    }

    // ── PT-BR egg items ──────────────────────────────────────────────────────────

    @Test
    fun `corpus - ovo resolves to egg`() {
        assertEquals(IconBucket.EGG, matcher.match("ovo"))
    }

    // ── Near-miss and disambiguation cases from curation ────────────────────────

    @Test
    fun `corpus - macarrão resolves to pasta not pantry`() {
        assertEquals(IconBucket.PASTA, matcher.match("macarrão"))
    }

    @Test
    fun `corpus - carne moída resolves to meat via token match`() {
        assertEquals(IconBucket.MEAT, matcher.match("carne moída"))
    }

    @Test
    fun `corpus - café resolves to beverages hot`() {
        assertEquals(IconBucket.BEVERAGES_HOT, matcher.match("café"))
    }

    // ── Items that correctly stay generic ────────────────────────────────────────

    @Test
    fun `corpus - parafuso stays generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("parafuso"))
    }

    @Test
    fun `corpus - cabo USB stays generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("cabo USB"))
    }

    @Test
    fun `corpus - camisa stays generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("camisa"))
    }
}
