package com.jhow.shopplist.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingSearchTest {

    @Test
    fun `suggestionScore returns null for short query`() {
        assertNull(ShoppingSearch.suggestionScore(candidate = "Milk", normalizedQuery = "m"))
    }

    @Test
    fun `suggestionScore prioritizes exact prefix word prefix substring and fuzzy matches`() {
        assertEquals(0, ShoppingSearch.suggestionScore(candidate = "Milk", normalizedQuery = "milk"))
        assertEquals(100, ShoppingSearch.suggestionScore(candidate = "Milkshake", normalizedQuery = "milk"))
        assertEquals(200, ShoppingSearch.suggestionScore(candidate = "Almond Milk", normalizedQuery = "milk"))
        assertEquals(302, ShoppingSearch.suggestionScore(candidate = "Soymilk", normalizedQuery = "ym"))
        assertEquals(402, ShoppingSearch.suggestionScore(candidate = "Home", normalizedQuery = "hme"))
    }
}
