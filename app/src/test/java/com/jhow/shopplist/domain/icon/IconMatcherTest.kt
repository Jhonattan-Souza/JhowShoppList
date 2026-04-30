package com.jhow.shopplist.domain.icon

import org.junit.Assert.assertEquals
import org.junit.Test

class IconMatcherTest {

    private val dictionary = mapOf(
        "leite" to IconBucket.DAIRY,
        "milk" to IconBucket.DAIRY,
        "iogurte" to IconBucket.DAIRY,
        "yogurt" to IconBucket.DAIRY,
        "queijo" to IconBucket.DAIRY,
        "cheese" to IconBucket.DAIRY,
        "maca" to IconBucket.FRUIT,
        "apple" to IconBucket.FRUIT,
        "banana" to IconBucket.FRUIT,
        "laranja" to IconBucket.FRUIT,
        "orange" to IconBucket.FRUIT,
        "uva" to IconBucket.FRUIT,
        "pao" to IconBucket.BREAD,
        "bread" to IconBucket.BREAD,
        "bisnaguinha" to IconBucket.BREAD,
        "arroz" to IconBucket.PANTRY_CANNED,
        "rice" to IconBucket.PANTRY_CANNED,
        "feijao" to IconBucket.PANTRY_CANNED,
        "beans" to IconBucket.PANTRY_CANNED,
        "macarrao" to IconBucket.PANTRY_CANNED,
        "pasta" to IconBucket.PANTRY_CANNED,
    )

    private val aliasMap = mapOf(
        "leites" to IconBucket.DAIRY,
        "bananas" to IconBucket.FRUIT,
    )

    private val normalizer = DefaultTextNormalizer()
    private val matcher = DefaultIconMatcher(dictionary, normalizer, aliasMap)

    // ── Exact match ───────────────────────────────────────────────────────────

    @Test
    fun `exact match returns corresponding bucket`() {
        assertEquals(IconBucket.DAIRY, matcher.match("leite"))
    }

    @Test
    fun `exact match is case-insensitive`() {
        assertEquals(IconBucket.DAIRY, matcher.match("Leite"))
    }

    @Test
    fun `exact match trims whitespace`() {
        assertEquals(IconBucket.DAIRY, matcher.match("  leite  "))
    }

    @Test
    fun `exact match after diacritic normalization`() {
        // maçã normalizes to maca, which is in the dictionary
        assertEquals(IconBucket.FRUIT, matcher.match("maçã"))
    }

    @Test
    fun `exact match after diacritic normalization pao`() {
        assertEquals(IconBucket.BREAD, matcher.match("pão"))
    }

    @Test
    fun `exact match after diacritic normalization feijao`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("feijão"))
    }

    // ── Alias match ───────────────────────────────────────────────────────────

    @Test
    fun `plural form resolved via alias map`() {
        assertEquals(IconBucket.DAIRY, matcher.match("leites"))
    }

    @Test
    fun `plural form resolved via alias map fruit`() {
        assertEquals(IconBucket.FRUIT, matcher.match("bananas"))
    }

    // ── Token-head match ──────────────────────────────────────────────────────

    @Test
    fun `multi-word item resolved via token match`() {
        assertEquals(IconBucket.DAIRY, matcher.match("leite integral"))
    }

    @Test
    fun `multi-word item with diacritics resolved via token match`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("feijão preto"))
    }

    @Test
    fun `leading quantity stripped before token match`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("2kg arroz"))
    }

    @Test
    fun `leading quantity and adjective stripped for token match`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("2kg arroz integral"))
    }

    @Test
    fun `2kg arroz integral resolves same bucket as bare arroz`() {
        assertEquals(matcher.match("arroz"), matcher.match("2kg arroz integral"))
    }

    @Test
    fun `trailing quantity stripped before token match`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("feijao 500g"))
    }

    @Test
    fun `500ml leite resolves same bucket as bare leite`() {
        assertEquals(matcher.match("leite"), matcher.match("500ml leite"))
    }

    @Test
    fun `leite integral resolved via token match`() {
        assertEquals(IconBucket.DAIRY, matcher.match("leite integral"))
    }

    @Test
    fun `leite desnatado resolved via token match`() {
        assertEquals(IconBucket.DAIRY, matcher.match("leite desnatado"))
    }

    @Test
    fun `arroz branco resolved via token match`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("arroz branco"))
    }

    @Test
    fun `arroz integral resolved via token match`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("arroz integral"))
    }

    @Test
    fun `feijao carioca resolved via token match`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("feijao carioca"))
    }

    @Test
    fun `banana nanica resolved via token match`() {
        assertEquals(IconBucket.FRUIT, matcher.match("banana nanica"))
    }

    @Test
    fun `banana prata resolved via token match`() {
        assertEquals(IconBucket.FRUIT, matcher.match("banana prata"))
    }

    @Test
    fun `iogurte grego resolved via token match`() {
        assertEquals(IconBucket.DAIRY, matcher.match("iogurte grego"))
    }

    @Test
    fun `queijo mussarela resolved via token match`() {
        assertEquals(IconBucket.DAIRY, matcher.match("queijo mussarela"))
    }

    @Test
    fun `pao de queijo resolved via token match after stopword removal`() {
        // "pao de queijo" → stopword "de" removed → "pao queijo" → token "pao" → BREAD
        assertEquals(IconBucket.BREAD, matcher.match("pão de queijo"))
    }

    @Test
    fun `iogurte de morango resolved via token match after stopword removal`() {
        assertEquals(IconBucket.DAIRY, matcher.match("iogurte de morango"))
    }

    @Test
    fun `leite do campo resolved via token match after stopword removal`() {
        assertEquals(IconBucket.DAIRY, matcher.match("leite do campo"))
    }

    @Test
    fun `maca verde resolved via token match`() {
        assertEquals(IconBucket.FRUIT, matcher.match("maçã verde"))
    }

    @Test
    fun `non-head token match — first token misses, later token hits`() {
        // "embalagem leite" → "embalagem" not in dict, "leite" is → DAIRY
        assertEquals(IconBucket.DAIRY, matcher.match("embalagem leite"))
    }

    // ── Mixed-language inputs ─────────────────────────────────────────────────

    @Test
    fun `milk powder resolved via token match`() {
        // "milk powder" → token "milk" → DAIRY
        assertEquals(IconBucket.DAIRY, matcher.match("milk powder"))
    }

    @Test
    fun `apple juice resolved via token match`() {
        assertEquals(IconBucket.FRUIT, matcher.match("apple juice"))
    }

    @Test
    fun `shampoo Pantene resolves to generic when not in dictionary`() {
        assertEquals(IconBucket.GENERIC, matcher.match("shampoo Pantene"))
    }

    @Test
    fun `racao Whiskas resolves to generic when not in dictionary`() {
        assertEquals(IconBucket.GENERIC, matcher.match("ração Whiskas"))
    }

    @Test
    fun `whey protein resolves to generic when not in dictionary`() {
        assertEquals(IconBucket.GENERIC, matcher.match("whey protein"))
    }

    // ── Near-miss and ambiguity cases ─────────────────────────────────────────

    @Test
    fun `pimenta alone resolves to generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("pimenta"))
    }

    @Test
    fun `pimenta do reino resolves to generic after stopword removal`() {
        // "pimenta do reino" → "pimenta reino" → token "pimenta" not in dict → GENERIC
        assertEquals(IconBucket.GENERIC, matcher.match("pimenta do reino"))
    }

    @Test
    fun `sal resolves to generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("sal"))
    }

    @Test
    fun `aipim resolves to generic (regional, not in slice-1 dict)`() {
        assertEquals(IconBucket.GENERIC, matcher.match("aipim"))
    }

    @Test
    fun `macaxeira resolves to generic (regional, not in slice-1 dict)`() {
        assertEquals(IconBucket.GENERIC, matcher.match("macaxeira"))
    }

    @Test
    fun `polenta resolves to generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("polenta"))
    }

    // ── GENERIC fallback cases ────────────────────────────────────────────────

    @Test
    fun `unknown term returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("parafuso"))
    }

    @Test
    fun `hardware item returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("cabo USB"))
    }

    @Test
    fun `clothing item returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("camisa"))
    }

    @Test
    fun `empty string returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match(""))
    }

    @Test
    fun `whitespace-only string returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("   "))
    }

    // ── Dictionary update ─────────────────────────────────────────────────────

    @Test
    fun `updating dictionary changes match results`() {
        val emptyMatcher = DefaultIconMatcher(emptyMap(), normalizer)
        assertEquals(IconBucket.GENERIC, emptyMatcher.match("leite"))

        assertEquals(true, emptyMatcher.updateDictionary(mapOf("leite" to IconBucket.DAIRY)))
        assertEquals(IconBucket.DAIRY, emptyMatcher.match("leite"))
    }

    @Test
    fun `updating dictionary with same contents reports unchanged`() {
        val sameMatcher = DefaultIconMatcher(dictionary, normalizer)
        assertEquals(false, sameMatcher.updateDictionary(dictionary.toMap()))
    }

    @Test
    fun `updating dictionary with accented keys normalizes them`() {
        val m = DefaultIconMatcher(emptyMap(), normalizer)
        m.updateDictionary(mapOf("maçã" to IconBucket.FRUIT))
        assertEquals(IconBucket.FRUIT, m.match("maca"))
    }

    @Test
    fun `alias map keys are normalized at construction so accented aliases work`() {
        // "maçãs" key → normalized to "macas"; query "maçãs" also normalizes to "macas"
        val m = DefaultIconMatcher(emptyMap(), normalizer, mapOf("maçãs" to IconBucket.FRUIT))
        assertEquals(IconBucket.FRUIT, m.match("maçãs"))
    }

    @Test
    fun `fruit terms resolve to fruit bucket`() {
        assertEquals(IconBucket.FRUIT, matcher.match("maçã"))
        assertEquals(IconBucket.FRUIT, matcher.match("maca"))
        assertEquals(IconBucket.FRUIT, matcher.match("apple"))
    }

    @Test
    fun `bread terms resolve to bread bucket`() {
        assertEquals(IconBucket.BREAD, matcher.match("pão"))
        assertEquals(IconBucket.BREAD, matcher.match("pao"))
        assertEquals(IconBucket.BREAD, matcher.match("bread"))
    }

    @Test
    fun `pantry terms resolve to pantry canned bucket`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("arroz"))
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("rice"))
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("feijão"))
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("feijao"))
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("beans"))
    }
}
