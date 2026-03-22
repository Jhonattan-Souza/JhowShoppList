package com.jhow.shopplist.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhow.shopplist.domain.usecase.AddShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.DeleteShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.MarkPurchasedItemPendingUseCase
import com.jhow.shopplist.domain.usecase.MarkSelectedItemsPurchasedUseCase
import com.jhow.shopplist.domain.usecase.ObservePendingItemsUseCase
import com.jhow.shopplist.domain.usecase.ObservePurchasedItemsUseCase
import com.jhow.shopplist.domain.usecase.RequestShoppingSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    observePendingItemsUseCase: ObservePendingItemsUseCase,
    observePurchasedItemsUseCase: ObservePurchasedItemsUseCase,
    private val addShoppingItemUseCase: AddShoppingItemUseCase,
    private val deleteShoppingItemUseCase: DeleteShoppingItemUseCase,
    private val markSelectedItemsPurchasedUseCase: MarkSelectedItemsPurchasedUseCase,
    private val markPurchasedItemPendingUseCase: MarkPurchasedItemPendingUseCase,
    private val requestShoppingSyncUseCase: RequestShoppingSyncUseCase
) : ViewModel() {

    private val inputValue = MutableStateFlow("")
    private val selectedIds = MutableStateFlow(emptySet<String>())
    private val itemPendingDeletion = MutableStateFlow<com.jhow.shopplist.domain.model.ShoppingItem?>(null)

    val uiState: StateFlow<ShoppingListUiState> = combine(
        observePendingItemsUseCase(),
        observePurchasedItemsUseCase(),
        inputValue,
        selectedIds,
        itemPendingDeletion
    ) { pendingItems, purchasedItems, currentInput, currentSelection, pendingDeletion ->
        val pendingIds = pendingItems.mapTo(linkedSetOf(), transform = { it.id })
        val distinctPurchasedItems = purchasedItems.filterNot { it.id in pendingIds }
        val visibleItems = pendingItems + distinctPurchasedItems
        ShoppingListUiState(
            inputValue = currentInput,
            pendingItems = pendingItems,
            purchasedItems = distinctPurchasedItems,
            selectedIds = currentSelection.intersect(pendingIds),
            itemPendingDeletion = visibleItems.firstOrNull { it.id == pendingDeletion?.id }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ShoppingListUiState()
    )

    fun onInputValueChange(value: String) {
        inputValue.value = value
    }

    fun onAddItem() {
        val currentValue = inputValue.value
        if (currentValue.isBlank()) return

        viewModelScope.launch {
            addShoppingItemUseCase(currentValue)
            inputValue.value = ""
            requestShoppingSyncUseCase()
        }
    }

    fun onPendingItemClicked(id: String) {
        selectedIds.update { currentIds ->
            if (id in currentIds) currentIds - id else currentIds + id
        }
    }

    fun onPurchaseSelectedItems() {
        val ids = selectedIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            markSelectedItemsPurchasedUseCase(ids)
            selectedIds.value = emptySet()
            requestShoppingSyncUseCase()
        }
    }

    fun onPurchasedItemClicked(id: String) {
        viewModelScope.launch {
            markPurchasedItemPendingUseCase(id)
            selectedIds.update { it - id }
            requestShoppingSyncUseCase()
        }
    }

    fun onDeleteItemRequested(item: com.jhow.shopplist.domain.model.ShoppingItem) {
        itemPendingDeletion.value = item
    }

    fun onDeleteItemDismissed() {
        itemPendingDeletion.value = null
    }

    fun onDeleteItemConfirmed() {
        val item = itemPendingDeletion.value ?: return

        viewModelScope.launch {
            deleteShoppingItemUseCase(item.id)
            selectedIds.update { it - item.id }
            itemPendingDeletion.value = null
            requestShoppingSyncUseCase()
        }
    }
}
