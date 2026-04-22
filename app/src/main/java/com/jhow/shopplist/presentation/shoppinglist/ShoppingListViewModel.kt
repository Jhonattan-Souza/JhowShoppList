package com.jhow.shopplist.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhow.shopplist.core.search.ShoppingSearch
import com.jhow.shopplist.domain.usecase.AddOrReclaimShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.ClearCalDavPendingActionUseCase
import com.jhow.shopplist.domain.usecase.ConfirmCreateCalDavListUseCase
import com.jhow.shopplist.domain.usecase.DeleteShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.GetCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.MarkPurchasedItemPendingUseCase
import com.jhow.shopplist.domain.usecase.MarkSelectedItemsPurchasedUseCase
import com.jhow.shopplist.domain.usecase.ObserveItemNamesUseCase
import com.jhow.shopplist.domain.usecase.ObservePendingItemsUseCase
import com.jhow.shopplist.domain.usecase.ObservePurchasedItemsUseCase
import com.jhow.shopplist.domain.usecase.RequestShoppingSyncUseCase
import com.jhow.shopplist.domain.usecase.SaveCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.ValidateCalDavSyncSettingsUseCase
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavValidationResult
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
    observeItemNamesUseCase: ObserveItemNamesUseCase,
    private val addOrReclaimShoppingItemUseCase: AddOrReclaimShoppingItemUseCase,
    private val deleteShoppingItemUseCase: DeleteShoppingItemUseCase,
    private val markSelectedItemsPurchasedUseCase: MarkSelectedItemsPurchasedUseCase,
    private val markPurchasedItemPendingUseCase: MarkPurchasedItemPendingUseCase,
    private val requestShoppingSyncUseCase: RequestShoppingSyncUseCase,
    private val getCalDavSyncConfigUseCase: GetCalDavSyncConfigUseCase,
    private val validateCalDavSyncSettingsUseCase: ValidateCalDavSyncSettingsUseCase,
    private val saveCalDavSyncConfigUseCase: SaveCalDavSyncConfigUseCase,
    private val confirmCreateCalDavListUseCase: ConfirmCreateCalDavListUseCase,
    private val clearCalDavPendingActionUseCase: ClearCalDavPendingActionUseCase
) : ViewModel() {

    private val inputValue = MutableStateFlow("")
    private val selectedIds = MutableStateFlow(emptySet<String>())
    private val itemPendingDeletion = MutableStateFlow<com.jhow.shopplist.domain.model.ShoppingItem?>(null)
    private val syncMenuExpanded = MutableStateFlow(false)
    private val syncSettingsVisible = MutableStateFlow(false)
    private val syncSettingsForm = MutableStateFlow(ShoppingListSyncSettingsUiState())
    private val configFlow = getCalDavSyncConfigUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = com.jhow.shopplist.domain.sync.CalDavSyncConfig()
    )
    private val inputWithSuggestions = combine(observeItemNamesUseCase(), inputValue) { allItemNames, currentInput ->
        currentInput to buildSuggestions(allItemNames = allItemNames, currentInput = currentInput)
    }

    private val shoppingState = combine(
        observePendingItemsUseCase(),
        observePurchasedItemsUseCase(),
        inputWithSuggestions,
        selectedIds,
        itemPendingDeletion
    ) { pendingItems, purchasedItems, currentInputWithSuggestions, currentSelection, pendingDeletion ->
        val pendingIds = pendingItems.mapTo(linkedSetOf(), transform = { it.id })
        val distinctPurchasedItems = purchasedItems.filterNot { it.id in pendingIds }
        val visibleItems = pendingItems + distinctPurchasedItems
        val (currentInput, currentSuggestions) = currentInputWithSuggestions
        ShoppingListUiState(
            inputValue = currentInput,
            suggestions = currentSuggestions,
            pendingItems = pendingItems,
            purchasedItems = distinctPurchasedItems,
            selectedIds = currentSelection.intersect(pendingIds),
            itemPendingDeletion = visibleItems.firstOrNull { it.id == pendingDeletion?.id }
        )
    }

    val uiState: StateFlow<ShoppingListUiState> = combine(
        shoppingState,
        syncMenuExpanded,
        syncSettingsVisible,
        configFlow,
        syncSettingsForm
    ) { shopping, isMenuExpanded, isSettingsVisible, config, form ->
        val hydratedForm = if (!isSettingsVisible) {
            form.copy(
                enabled = config.enabled,
                serverUrl = config.serverUrl,
                username = config.username,
                listName = config.listName,
                hasStoredPassword = config.hasStoredPassword,
                syncState = config.syncState,
                statusMessage = config.statusMessage,
                pendingAction = config.pendingAction
            )
        } else {
            form.copy(
                syncState = config.syncState,
                hasStoredPassword = if (form.password.isBlank()) config.hasStoredPassword else false
            )
        }
        shopping.copy(
            isSyncMenuExpanded = isMenuExpanded,
            isSyncSettingsVisible = isSettingsVisible,
            syncSettings = hydratedForm
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

    fun onSyncMenuClicked() {
        syncMenuExpanded.value = true
    }

    fun onSyncMenuDismissed() {
        syncMenuExpanded.value = false
    }

    fun onSyncSettingsRequested() {
        syncMenuExpanded.value = false
        val config = configFlow.value
        syncSettingsForm.value = ShoppingListSyncSettingsUiState(
            enabled = config.enabled,
            serverUrl = config.serverUrl,
            username = config.username,
            listName = config.listName,
            hasStoredPassword = config.hasStoredPassword,
            password = "",
            syncState = config.syncState,
            statusMessage = config.statusMessage,
            pendingAction = config.pendingAction
        )
        syncSettingsVisible.value = true
    }

    fun onSyncServerUrlChanged(value: String) {
        syncSettingsForm.update { it.copy(serverUrl = value) }
    }

    fun onSyncUsernameChanged(value: String) {
        syncSettingsForm.update { it.copy(username = value) }
    }

    fun onSyncPasswordChanged(value: String) {
        syncSettingsForm.update { it.copy(password = value, hasStoredPassword = it.hasStoredPassword && value.isBlank()) }
    }

    fun onSyncListNameChanged(value: String) {
        syncSettingsForm.update { it.copy(listName = value) }
    }

    fun onSyncEnabledChanged(value: Boolean) {
        syncSettingsForm.update { it.copy(enabled = value) }
    }

    fun onSyncSettingsDismissed() {
        syncSettingsForm.update { it.copy(password = "") }
        syncSettingsVisible.value = false
    }

    fun onSyncSettingsSaved() {
        viewModelScope.launch {
            syncSettingsForm.update {
                it.copy(
                    isSaving = true,
                    statusMessage = null,
                    pendingAction = CalDavPendingAction.None
                )
            }
            val current = syncSettingsForm.value
            when (
                val result = validateCalDavSyncSettingsUseCase(
                    enabled = current.enabled,
                    serverUrl = current.serverUrl,
                    username = current.username,
                    listName = current.listName,
                    password = current.password
                )
            ) {
                is CalDavValidationResult.Success -> {
                    saveCalDavSyncConfigUseCase(
                        enabled = current.enabled,
                        serverUrl = current.serverUrl,
                        username = current.username,
                        listName = current.listName,
                        password = current.password,
                        resolvedCollectionUrl = result.resolvedCollectionUrl.ifBlank { null }
                    )
                    syncSettingsForm.value = current.copy(
                        password = "",
                        hasStoredPassword = current.password.isNotBlank() || current.hasStoredPassword,
                        isSaving = false,
                        statusMessage = null,
                        pendingAction = CalDavPendingAction.None
                    )
                    syncSettingsVisible.value = false
                    requestShoppingSyncUseCase()
                }
                is CalDavValidationResult.MissingList -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Remote list ${result.listName} does not exist yet",
                            pendingAction = CalDavPendingAction.CreateMissingList
                        )
                    }
                }
                is CalDavValidationResult.AuthError -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = result.message,
                            pendingAction = CalDavPendingAction.None
                        )
                    }
                }
                is CalDavValidationResult.NetworkError -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = result.message,
                            pendingAction = CalDavPendingAction.None
                        )
                    }
                }
                is CalDavValidationResult.ConfigurationError -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = result.message,
                            pendingAction = CalDavPendingAction.None
                        )
                    }
                }
            }
        }
    }

    fun onSyncNowRequested() {
        syncMenuExpanded.value = false
        viewModelScope.launch {
            requestShoppingSyncUseCase()
        }
    }

    fun onConfirmCreateMissingList() {
        viewModelScope.launch {
            val current = syncSettingsForm.value
            syncSettingsForm.update {
                it.copy(
                    isSaving = true,
                    statusMessage = null,
                    pendingAction = CalDavPendingAction.None
                )
            }
            when (
                val result = confirmCreateCalDavListUseCase(
                    serverUrl = current.serverUrl,
                    username = current.username,
                    listName = current.listName,
                    password = current.password
                )
            ) {
                is CalDavValidationResult.Success -> {
                    saveCalDavSyncConfigUseCase(
                        enabled = current.enabled,
                        serverUrl = current.serverUrl,
                        username = current.username,
                        listName = current.listName,
                        password = current.password,
                        resolvedCollectionUrl = result.resolvedCollectionUrl.ifBlank { null }
                    )
                    syncSettingsForm.value = current.copy(
                        password = "",
                        hasStoredPassword = current.password.isNotBlank() || current.hasStoredPassword,
                        isSaving = false,
                        statusMessage = null,
                        pendingAction = CalDavPendingAction.None
                    )
                    syncSettingsVisible.value = false
                    requestShoppingSyncUseCase()
                }
                is CalDavValidationResult.MissingList -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Remote list ${result.listName} does not exist yet",
                            pendingAction = CalDavPendingAction.CreateMissingList
                        )
                    }
                }
                is CalDavValidationResult.AuthError -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = result.message,
                            pendingAction = CalDavPendingAction.None
                        )
                    }
                }
                is CalDavValidationResult.NetworkError -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = result.message,
                            pendingAction = CalDavPendingAction.None
                        )
                    }
                }
                is CalDavValidationResult.ConfigurationError -> {
                    syncSettingsForm.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = result.message,
                            pendingAction = CalDavPendingAction.None
                        )
                    }
                }
            }
        }
    }

    fun onClearPendingAction() {
        viewModelScope.launch {
            clearCalDavPendingActionUseCase()
        }
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
