package com.jhow.shopplist.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhow.shopplist.core.search.ShoppingSearch
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.usecase.AddOrReclaimShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.DeleteShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.GetCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.MarkPurchasedItemPendingUseCase
import com.jhow.shopplist.domain.usecase.MarkSelectedItemsPurchasedUseCase
import com.jhow.shopplist.domain.usecase.ObserveItemNamesUseCase
import com.jhow.shopplist.domain.usecase.ObservePendingItemsUseCase
import com.jhow.shopplist.domain.usecase.ObservePurchasedItemsUseCase
import com.jhow.shopplist.domain.usecase.ObserveSyncStateUseCase
import com.jhow.shopplist.domain.usecase.RequestShoppingSyncUseCase
import com.jhow.shopplist.domain.usecase.RestoreDeletedShoppingItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ShoppingListIntermediateState(
    val pendingItems: List<ShoppingItem>,
    val purchasedItems: List<ShoppingItem>,
    val currentInput: String,
    val currentSuggestions: List<String>,
    val selectedIds: Set<String>,
    val isSelectionMode: Boolean,
    val deleteUndoSnackbar: DeleteUndoSnackbarState?
)

@HiltViewModel
@Suppress("LongParameterList")
class ShoppingListViewModel @Inject constructor(
    observePendingItemsUseCase: ObservePendingItemsUseCase,
    observePurchasedItemsUseCase: ObservePurchasedItemsUseCase,
    observeItemNamesUseCase: ObserveItemNamesUseCase,
    private val addOrReclaimShoppingItemUseCase: AddOrReclaimShoppingItemUseCase,
    private val deleteShoppingItemUseCase: DeleteShoppingItemUseCase,
    private val restoreDeletedShoppingItemUseCase: RestoreDeletedShoppingItemUseCase,
    private val markSelectedItemsPurchasedUseCase: MarkSelectedItemsPurchasedUseCase,
    private val markPurchasedItemPendingUseCase: MarkPurchasedItemPendingUseCase,
    private val requestShoppingSyncUseCase: RequestShoppingSyncUseCase,
    observeSyncStateUseCase: ObserveSyncStateUseCase,
    getCalDavSyncConfigUseCase: GetCalDavSyncConfigUseCase,
    private val selectionController: SelectionController
) : ViewModel() {
    private val inputValue = MutableStateFlow("")
    private val undoCoordinator = UndoCoordinator(
        timeoutMillis = DELETE_UNDO_TIMEOUT_MILLIS,
        scope = viewModelScope,
        dispatcher = Dispatchers.Main.immediate,
        onOptimisticDelete = { item ->
            deleteShoppingItemUseCase(item.id)
            if (item.id in selectionController.selected.value) {
                selectionController.toggle(item.id)
            }
        },
        onRestore = { item ->
            restoreDeletedShoppingItemUseCase(item)
        },
        onCommit = {
            requestSync()
        }
    )
    private val inputWithSuggestions = combine(observeItemNamesUseCase(), inputValue) { allItemNames, currentInput ->
        currentInput to buildSuggestions(allItemNames = allItemNames, currentInput = currentInput)
    }

    private val viewSignals = combine(
        inputWithSuggestions,
        selectionController.selected,
        selectionController.isActive,
        undoCoordinator.snackbarState
    ) { inputSuggestions, selectedIds, isSelectionMode, deleteUndoSnackbar ->
        ViewSignals(
            currentInput = inputSuggestions.first,
            currentSuggestions = inputSuggestions.second,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            deleteUndoSnackbar = deleteUndoSnackbar
        )
    }

    private val combinedState = combine(
        observePendingItemsUseCase(),
        observePurchasedItemsUseCase(),
        viewSignals
    ) { pendingItems, purchasedItems, signals ->
        ShoppingListIntermediateState(
            pendingItems = pendingItems,
            purchasedItems = purchasedItems,
            currentInput = signals.currentInput,
            currentSuggestions = signals.currentSuggestions,
            selectedIds = signals.selectedIds,
            isSelectionMode = signals.isSelectionMode,
            deleteUndoSnackbar = signals.deleteUndoSnackbar
        )
    }

    private val isSyncing = observeSyncStateUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )
    private val isSyncConfigured = getCalDavSyncConfigUseCase()
        .map { it.isReadyToSync }
        .distinctUntilChanged()
    private val manualSyncLatch = MutableStateFlow(false)
    private val hasPendingSyncRequest = MutableStateFlow(false)

    init {
        isSyncing
            .scan(false to false) { prev, current -> prev.second to current }
            .onEach { (prev, current) ->
                if (current) {
                    hasPendingSyncRequest.value = false
                }
                if (prev && !current) {
                    manualSyncLatch.value = false
                    hasPendingSyncRequest.value = false
                }
            }
            .launchIn(viewModelScope)
    }

    val uiState: StateFlow<ShoppingListUiState> = combinedState
        .combine(isSyncing) { intermediate, syncing -> intermediate to syncing }
        .combine(manualSyncLatch) { (intermediate, syncing), latch ->
            Triple(intermediate, syncing, latch)
        }
        .combine(isSyncConfigured) { (intermediate, syncing, latch), configured ->
            val pendingIds = intermediate.pendingItems.mapTo(linkedSetOf()) { it.id }
            val distinctPurchasedItems = intermediate.purchasedItems.filterNot { it.id in pendingIds }
            val visibleSelectedIds = intermediate.selectedIds.intersect(pendingIds)

            if (visibleSelectedIds != intermediate.selectedIds) {
                selectionController.retainOnly(pendingIds)
            }

            ShoppingListUiState(
                inputValue = intermediate.currentInput,
                suggestions = intermediate.currentSuggestions,
                pendingItems = intermediate.pendingItems,
                purchasedItems = distinctPurchasedItems,
                selectedIds = visibleSelectedIds,
                isSelectionMode = intermediate.isSelectionMode && visibleSelectedIds.isNotEmpty(),
                deleteUndoSnackbar = intermediate.deleteUndoSnackbar,
                isManualSync = syncing && latch,
                isBackgroundSync = syncing && !latch,
                isSyncConfigured = configured
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
            requestSync()
        }
    }

    fun onPendingItemClicked(id: String) {
        if (selectionController.isActive.value) {
            selectionController.toggle(id)
            return
        }

        markPendingItemPurchased(id)
    }

    fun onPendingItemMarkPurchased(id: String) {
        markPendingItemPurchased(id)
    }

    private fun markPendingItemPurchased(id: String) {
        viewModelScope.launch {
            markSelectedItemsPurchasedUseCase(setOf(id))
            requestSync()
        }
    }

    fun onPendingItemLongPressed(id: String) {
        selectionController.enter(id)
    }

    fun onPurchaseSelectedItems() {
        val ids = selectionController.selected.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            markSelectedItemsPurchasedUseCase(ids)
            selectionController.exit()
            requestSync()
        }
    }

    fun onDeleteSelectedItems() {
        val ids = selectionController.selected.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            ids.forEach { id ->
                deleteShoppingItemUseCase(id)
            }
            selectionController.exit()
            requestSync()
        }
    }

    fun onSelectionModeExited() {
        selectionController.exit()
    }

    fun onPurchasedItemClicked(id: String) {
        viewModelScope.launch {
            markPurchasedItemPendingUseCase(id)
            if (id in selectionController.selected.value) {
                selectionController.toggle(id)
            }
            requestSync()
        }
    }

    fun onDeleteItemRequested(item: ShoppingItem) {
        undoCoordinator.onDelete(item)
    }

    fun onDeleteUndoRequested() {
        undoCoordinator.onUndo()
    }

    fun onManualSyncRequested() {
        manualSyncLatch.value = true
        if (!isSyncing.value && !hasPendingSyncRequest.value) {
            viewModelScope.launch {
                requestSync()
            }
        }
    }

    private fun requestSync() {
        hasPendingSyncRequest.value = true
        requestShoppingSyncUseCase()
    }

    private companion object {
        const val MIN_SUGGESTION_QUERY_LENGTH = 2
        const val MAX_SUGGESTION_COUNT = 5
        const val DELETE_UNDO_TIMEOUT_MILLIS = 4_000L

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

    private data class ViewSignals(
        val currentInput: String,
        val currentSuggestions: List<String>,
        val selectedIds: Set<String>,
        val isSelectionMode: Boolean,
        val deleteUndoSnackbar: DeleteUndoSnackbarState?
    )
}
