package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.runtime.Immutable
import com.jhow.shopplist.domain.model.ShoppingItem

@Immutable
data class ShoppingListUiState(
    val inputValue: String = "",
    val suggestions: List<String> = emptyList(),
    val pendingItems: List<ShoppingItem> = emptyList(),
    val purchasedItems: List<ShoppingItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val itemPendingDeletion: ShoppingItem? = null,
    val isSyncing: Boolean = false,
    val isSyncConfigured: Boolean = false
) {
    val isBulkActionVisible: Boolean
        get() = selectedIds.isNotEmpty()
}
