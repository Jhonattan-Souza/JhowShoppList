package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.runtime.Immutable
import com.jhow.shopplist.domain.model.ShoppingItem

@Immutable
data class ShoppingListUiState(
    val inputValue: String = "",
    val pendingItems: List<ShoppingItem> = emptyList(),
    val purchasedItems: List<ShoppingItem> = emptyList(),
    val selectedIds: Set<String> = emptySet()
) {
    val isBulkActionVisible: Boolean
        get() = selectedIds.isNotEmpty()
}
