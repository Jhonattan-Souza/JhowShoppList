package com.jhow.shopplist.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhow.shopplist.core.search.ShoppingSearch
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.usecase.AddOrReclaimShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.DeleteShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.MarkPurchasedItemPendingUseCase
import com.jhow.shopplist.domain.usecase.MarkSelectedItemsPurchasedUseCase
import com.jhow.shopplist.domain.usecase.ObserveItemNamesUseCase
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

private data class ShoppingListIntermediateState(
    val pendingItems: List<ShoppingItem>,
    val purchasedItems: List<ShoppingItem>,
    val currentInput: String,
    val currentSuggestions: List<String>,
    val selectedIds: Set<String>,
    val itemPendingDeletion: ShoppingItem?
)

@HiltViewModel
@Suppress("LongParameterList")
class ShoppingListViewModel @Inject constructor(
    observePendingItemsUseCase: ObservePendingItemsUseCase,
    observePurchasedItemsUseCase: ObservePurchasedItemsUseCase,
    observeItemNamesUseCase: ObserveItemNamesUseCase,
    private val addOrReclaimShoppingItemUseCase: AddOrReclaimShoppingItemUseCase,
    private val deleteShoppingItemUseCase: DeleteShoppingItemUseCase,
    private val markSelectedItemsPurchasedUseCase: MarkSelectedItemsPurchasedUseCase,
    private val markPurchasedItemPendingUseCase: MarkPurchasedItemPendingUseCase,
    private val requestShoppingSyncUseCase: RequestShoppingSyncUseCase
) : ViewModel() {

    private val inputValue = MutableStateFlow("")
    private val selectedIds = MutableStateFlow(emptySet<String>())
    private val itemPendingDeletion = MutableStateFlow<ShoppingItem?>(null)
    private val syncMenuExpanded = MutableStateFlow(false)
    private val inputWithSuggestions = combine(observeItemNamesUseCase(), inputValue) { allItemNames, currentInput ->
        currentInput to buildSuggestions(allItemNames = allItemNames, currentInput = currentInput)
    }

    private val combinedState = combine(
        observePendingItemsUseCase(),
        observePurchasedItemsUseCase(),
        inputWithSuggestions,
        selectedIds,
        itemPendingDeletion
    ) { pendingItems, purchasedItems, inputSuggestions, selectedIds, itemPendingDeletion ->
        ShoppingListIntermediateState(
            pendingItems = pendingItems,
            purchasedItems = purchasedItems,
            currentInput = inputSuggestions.first,
            currentSuggestions = inputSuggestions.second,
            selectedIds = selectedIds,
            itemPendingDeletion = itemPendingDeletion
        )
    }

    val uiState: StateFlow<ShoppingListUiState> = combinedState
        .combine(syncMenuExpanded) { intermediate, isSyncMenuExpanded ->
            val pendingIds = intermediate.pendingItems.mapTo(linkedSetOf()) { it.id }
            val distinctPurchasedItems = intermediate.purchasedItems.filterNot { it.id in pendingIds }
            val visibleItems = intermediate.pendingItems + distinctPurchasedItems

            ShoppingListUiState(
                inputValue = intermediate.currentInput,
                suggestions = intermediate.currentSuggestions,
                pendingItems = intermediate.pendingItems,
                purchasedItems = distinctPurchasedItems,
                selectedIds = intermediate.selectedIds.intersect(pendingIds),
                itemPendingDeletion = visibleItems.firstOrNull { it.id == intermediate.itemPendingDeletion?.id },
                isSyncMenuExpanded = isSyncMenuExpanded
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ShoppingListUiState()
        )

    fun onInputValueChange(value: String) {
        inputValue.value = value
    }

    fun onSuggestionSelected(name: String) {
        inputValue.value = name
        onAddItem()
    }

    fun onAddItem() {
        val currentValue = inputValue.value
        if (currentValue.isBlank()) return

        viewModelScope.launch {
            addOrReclaimShoppingItemUseCase(currentValue)
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

    fun onDeleteItemRequested(item: ShoppingItem) {
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

    fun onSyncMenuClicked() {
        syncMenuExpanded.value = true
    }

    fun onSyncMenuDismissed() {
        syncMenuExpanded.value = false
    }

    fun onSyncNowRequested() {
        syncMenuExpanded.value = false
        viewModelScope.launch {
            requestShoppingSyncUseCase()
        }
    }

    fun onSyncSettingsRequested() {
        syncMenuExpanded.value = false
    }

    private companion object {
        const val MIN_SUGGESTION_QUERY_LENGTH = 2
        const val MAX_SUGGESTION_COUNT = 5

        fun buildSuggestions(allItemNames: List<String>, currentInput: String): List<String> {
            val normalizedInput = ShoppingSearch.normalize(currentInput)
            if (normalizedInput.length < MIN_SUGGESTION_QUERY_LENGTH) {
                return emptyList()
            }

            return allItemNames
                .asSequence()
                .mapIndexedNotNull { index, name ->
                    ShoppingSearch.suggestionScore(
                        candidate = name,
                        normalizedQuery = normalizedInput
                    )?.let { score ->
                        SuggestionCandidate(
                            name = name,
                            originalIndex = index,
                            score = score
                        )
                    }
                }
                .sortedWith(compareBy<SuggestionCandidate> { it.score }.thenBy { it.originalIndex })
                .take(MAX_SUGGESTION_COUNT)
                .map(SuggestionCandidate::name)
                .toList()
        }

        private data class SuggestionCandidate(
            val name: String,
            val originalIndex: Int,
            val score: Int
        )
    }
}
