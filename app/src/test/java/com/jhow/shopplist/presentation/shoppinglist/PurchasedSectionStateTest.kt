package com.jhow.shopplist.presentation.shoppinglist

import org.junit.Assert.assertEquals
import org.junit.Test

class PurchasedSectionStateTest {
    @Test
    fun `empty list expands purchased section by default`() {
        assertEquals(
            PurchasedSectionVisibility.Expanded,
            PurchasedSectionState.resolve(pendingCount = 0, purchasedCount = 0, userExpanded = null)
        )
    }

    @Test
    fun `pending only list collapses purchased section by default`() {
        assertEquals(
            PurchasedSectionVisibility.Collapsed,
            PurchasedSectionState.resolve(pendingCount = 3, purchasedCount = 0, userExpanded = null)
        )
    }

    @Test
    fun `purchased only list expands purchased section by default`() {
        assertEquals(
            PurchasedSectionVisibility.Expanded,
            PurchasedSectionState.resolve(pendingCount = 0, purchasedCount = 2, userExpanded = null)
        )
    }

    @Test
    fun `mixed list collapses purchased section by default`() {
        assertEquals(
            PurchasedSectionVisibility.Collapsed,
            PurchasedSectionState.resolve(pendingCount = 4, purchasedCount = 2, userExpanded = null)
        )
    }

    @Test
    fun `user expanded choice overrides the default collapse rule`() {
        assertEquals(
            PurchasedSectionVisibility.Expanded,
            PurchasedSectionState.resolve(pendingCount = 4, purchasedCount = 2, userExpanded = true)
        )
    }

    @Test
    fun `user collapsed choice overrides the default expand rule`() {
        assertEquals(
            PurchasedSectionVisibility.Collapsed,
            PurchasedSectionState.resolve(pendingCount = 0, purchasedCount = 2, userExpanded = false)
        )
    }
}
