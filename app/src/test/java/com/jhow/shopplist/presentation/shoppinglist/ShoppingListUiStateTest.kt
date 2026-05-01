package com.jhow.shopplist.presentation.shoppinglist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListUiStateTest {
    @Test
    fun `bulk action is visible when selection exists`() {
        val uiState = ShoppingListUiState(selectedIds = setOf("item-1"), isSelectionMode = true)

        assertTrue(uiState.isBulkActionVisible)
    }

    @Test
    fun `bulk action is hidden without selection`() {
        val uiState = ShoppingListUiState()

        assertFalse(uiState.isBulkActionVisible)
    }
}
