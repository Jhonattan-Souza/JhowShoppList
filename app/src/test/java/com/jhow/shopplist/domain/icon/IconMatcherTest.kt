package com.jhow.shopplist.domain.icon

import org.junit.Assert.assertEquals
import org.junit.Test

class IconMatcherTest {

    private val dictionary = mapOf(
        "leite" to IconBucket.DAIRY,
        "milk" to IconBucket.DAIRY,
        "iogurte" to IconBucket.DAIRY,
        "yogurt" to IconBucket.DAIRY,
        "maçã" to IconBucket.FRUIT,
        "maca" to IconBucket.FRUIT,
        "apple" to IconBucket.FRUIT,
        "banana" to IconBucket.FRUIT,
        "pão" to IconBucket.BREAD,
        "pao" to IconBucket.BREAD,
        "bread" to IconBucket.BREAD,
        "arroz" to IconBucket.PANTRY_CANNED,
        "rice" to IconBucket.PANTRY_CANNED,
        "feijão" to IconBucket.PANTRY_CANNED,
        "feijao" to IconBucket.PANTRY_CANNED,
        "beans" to IconBucket.PANTRY_CANNED
    )

    private val normalizer = DefaultTextNormalizer()
    private val matcher = DefaultIconMatcher(dictionary, normalizer)

    @Test
    fun `exact match returns corresponding bucket`() {
        assertEquals(IconBucket.DAIRY, matcher.match("leite"))
    }

    @Test
    fun `exact match with different casing returns bucket`() {
        assertEquals(IconBucket.DAIRY, matcher.match("Leite"))
    }

    @Test
    fun `exact match with whitespace returns bucket`() {
        assertEquals(IconBucket.DAIRY, matcher.match("  leite  "))
    }

    @Test
    fun `unknown term returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("parafuso"))
    }

    @Test
    fun `unknown english term returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match("screwdriver"))
    }

    @Test
    fun `empty string returns generic`() {
        assertEquals(IconBucket.GENERIC, matcher.match(""))
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

    @Test
    fun `mixed case pantry term resolves correctly`() {
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("Arroz"))
        assertEquals(IconBucket.PANTRY_CANNED, matcher.match("  Feijao  "))
    }
}
