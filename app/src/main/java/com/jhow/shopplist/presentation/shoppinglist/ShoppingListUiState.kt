package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.runtime.Immutable
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.model.ShoppingItem

data class ShoppingListSyncSettingsUiState(
    val enabled: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val listName: String = "",
    val syncState: CalDavSyncState = CalDavSyncState.Disabled,
    val statusMessage: String? = null,
    val pendingAction: CalDavPendingAction = CalDavPendingAction.None
)

@Immutable
data class ShoppingListUiState(
    val inputValue: String = "",
    val suggestions: List<String> = emptyList(),
    val pendingItems: List<ShoppingItem> = emptyList(),
    val purchasedItems: List<ShoppingItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val itemPendingDeletion: ShoppingItem? = null,
    val isSyncMenuExpanded: Boolean = false,
    val isSyncSettingsVisible: Boolean = false,
    val syncSettings: ShoppingListSyncSettingsUiState = ShoppingListSyncSettingsUiState()
) {
    val isBulkActionVisible: Boolean
        get() = selectedIds.isNotEmpty()
}
