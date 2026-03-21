package com.jhow.shopplist.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhow.shopplist.domain.usecase.AddShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.MarkPurchasedItemPendingUseCase
import com.jhow.shopplist.domain.usecase.MarkSelectedItemsPurchasedUseCase
import com.jhow.shopplist.domain.usecase.ObservePendingItemsUseCase
import com.jhow.shopplist.domain.usecase.ObservePurchasedItemsUseCase
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
    private val markSelectedItemsPurchasedUseCase: MarkSelectedItemsPurchasedUseCase,
    private val markPurchasedItemPendingUseCase: MarkPurchasedItemPendingUseCase
) : ViewModel() {

    private val inputValue = MutableStateFlow("")
    private val selectedIds = MutableStateFlow(emptySet<String>())

    val uiState: StateFlow<ShoppingListUiState> = combine(
        observePendingItemsUseCase(),
        observePurchasedItemsUseCase(),
        inputValue,
        selectedIds
    ) { pendingItems, purchasedItems, currentInput, currentSelection ->
        val pendingIds = pendingItems.mapTo(linkedSetOf(), transform = { it.id })
        val distinctPurchasedItems = purchasedItems.filterNot { it.id in pendingIds }
        ShoppingListUiState(
            inputValue = currentInput,
            pendingItems = pendingItems,
            purchasedItems = distinctPurchasedItems,
            selectedIds = currentSelection.intersect(pendingIds)
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
        }
    }

    fun onPurchasedItemClicked(id: String) {
        viewModelScope.launch {
            markPurchasedItemPendingUseCase(id)
            selectedIds.update { it - id }
        }
    }
}
