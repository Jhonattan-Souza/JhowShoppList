package com.jhow.shopplist.presentation.shoppinglist

import app.cash.turbine.test
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
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
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import com.jhow.shopplist.testing.FakeShoppingListRepository
import com.jhow.shopplist.testing.FakeShoppingSyncScheduler
import com.jhow.shopplist.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeShoppingListRepository
    private lateinit var syncScheduler: FakeShoppingSyncScheduler
    private lateinit var calDavConfigRepository: FakeCalDavConfigRepository
    private lateinit var selectionController: SelectionController
    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        repository = FakeShoppingListRepository()
        syncScheduler = FakeShoppingSyncScheduler()
        calDavConfigRepository = FakeCalDavConfigRepository()
        selectionController = SelectionController()
        viewModel = ShoppingListViewModel(
            observePendingItemsUseCase = ObservePendingItemsUseCase(repository),
            observePurchasedItemsUseCase = ObservePurchasedItemsUseCase(repository),
            observeItemNamesUseCase = ObserveItemNamesUseCase(repository),
            addOrReclaimShoppingItemUseCase = AddOrReclaimShoppingItemUseCase(repository),
            deleteShoppingItemUseCase = DeleteShoppingItemUseCase(repository),
            restoreDeletedShoppingItemUseCase = RestoreDeletedShoppingItemUseCase(repository),
            markSelectedItemsPurchasedUseCase = MarkSelectedItemsPurchasedUseCase(repository),
            markPurchasedItemPendingUseCase = MarkPurchasedItemPendingUseCase(repository),
            requestShoppingSyncUseCase = RequestShoppingSyncUseCase(syncScheduler),
            observeSyncStateUseCase = ObserveSyncStateUseCase(syncScheduler),
            getCalDavSyncConfigUseCase = GetCalDavSyncConfigUseCase(calDavConfigRepository),
            selectionController = selectionController
        )
    }

    @Test
    fun `adding an item trims input and clears the field`() = runTest {
        viewModel.onInputValueChange("  Oats  ")

        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("Oats"), repository.addedNames)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `typing shows fuzzy matched suggestions`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "milk", name = "Milk"),
                samplePurchasedItem(id = "almond-milk", name = "Almond Milk"),
                samplePendingItem(id = "bread", name = "Bread")
            )
        )
        advanceUntilIdle()

        viewModel.onInputValueChange("mil")
        advanceUntilIdle()

        assertEquals(listOf("Milk", "Almond Milk"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `typing shows accent insensitive suggestions`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "cafe", name = "Café"),
                samplePendingItem(id = "tea", name = "Tea")
            )
        )
        advanceUntilIdle()

        viewModel.onInputValueChange("cafe")
        advanceUntilIdle()

        assertEquals(listOf("Café"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `typing shows subsequence fuzzy suggestions`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "home", name = "Home"),
                samplePendingItem(id = "honey", name = "Honey"),
                samplePendingItem(id = "bread", name = "Bread")
            )
        )
        advanceUntilIdle()

        viewModel.onInputValueChange("hme")
        advanceUntilIdle()

        assertEquals(listOf("Home"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `adding a duplicate pending item clears input without adding`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "oats", name = "Oats")))
        advanceUntilIdle()

        viewModel.onInputValueChange(" oats ")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.addedNames)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `adding a purchased item reclaims it`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "beans", name = "Beans")))
        advanceUntilIdle()

        viewModel.onInputValueChange(" beans ")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("beans"), repository.pendingRequests)
        assertEquals(listOf("beans"), viewModel.uiState.value.pendingItems.map { it.id })
        assertEquals("", viewModel.uiState.value.inputValue)
    }

    @Test
    fun `adding an accentless duplicate pending item clears input without adding`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "cafe", name = "Café")))
        advanceUntilIdle()

        viewModel.onInputValueChange("cafe")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.addedNames)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `adding an accentless purchased item reclaims it`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "cafe", name = "Café")))
        advanceUntilIdle()

        viewModel.onInputValueChange("cafe")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("cafe"), repository.pendingRequests)
        assertEquals(listOf("cafe"), viewModel.uiState.value.pendingItems.map { it.id })
        assertEquals("", viewModel.uiState.value.inputValue)
    }

    @Test
    fun `selecting a suggestion submits it`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "coffee", name = "Coffee")))
        advanceUntilIdle()

        viewModel.onSuggestionSelected("Coffee")
        advanceUntilIdle()

        assertEquals(listOf("coffee"), repository.pendingRequests)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `pending item click marks item purchased immediately`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "rice")))
        advanceUntilIdle()

        viewModel.onPendingItemClicked("rice")
        advanceUntilIdle()

        assertEquals(listOf(setOf("rice")), repository.purchasedRequests)
        assertEquals(listOf("rice"), viewModel.uiState.value.purchasedItems.map { it.id })
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `purchase selected items clears selection and updates repository`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "milk"),
                samplePendingItem(id = "tea", purchaseCount = 2)
            )
        )
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("milk")
        viewModel.onPendingItemClicked("tea")
        advanceUntilIdle()

        viewModel.onPurchaseSelectedItems()
        advanceUntilIdle()

        assertEquals(listOf(setOf("milk", "tea")), repository.purchasedRequests)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
        assertEquals(listOf("milk", "tea"), viewModel.uiState.value.purchasedItems.map { it.id })
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `selected ids are dropped when items leave the pending section`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "eggs")))
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("eggs")
        advanceUntilIdle()
        repository.seedItems(listOf(samplePurchasedItem(id = "eggs")))
        advanceUntilIdle()

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `clicking pending item after stale selection disappears marks it purchased`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "eggs"),
                samplePendingItem(id = "rice")
            )
        )
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("eggs")
        advanceUntilIdle()
        repository.seedItems(
            listOf(
                samplePurchasedItem(id = "eggs"),
                samplePendingItem(id = "rice")
            )
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)

        viewModel.onPendingItemClicked("rice")
        advanceUntilIdle()

        assertEquals(listOf(setOf("rice")), repository.purchasedRequests)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `long pressing pending item enters multi select with item selected`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "rice")))
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("rice")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(setOf("rice"), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `pending item click toggles selection while multi select is active`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "rice"),
                samplePendingItem(id = "beans")
            )
        )
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("rice")
        advanceUntilIdle()
        viewModel.onPendingItemClicked("beans")
        advanceUntilIdle()

        assertEquals(setOf("rice", "beans"), viewModel.uiState.value.selectedIds)
        assertTrue(repository.purchasedRequests.isEmpty())
    }

    @Test
    fun `selection mode exits when toggling leaves selection empty`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "rice")))
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("rice")
        advanceUntilIdle()
        viewModel.onPendingItemClicked("rice")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `exiting selection mode clears selected ids`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "rice"),
                samplePendingItem(id = "beans")
            )
        )
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("rice")
        advanceUntilIdle()
        viewModel.onPendingItemClicked("beans")
        advanceUntilIdle()

        viewModel.onSelectionModeExited()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `delete selected items removes selected rows and exits selection mode`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "rice"),
                samplePendingItem(id = "beans")
            )
        )
        advanceUntilIdle()

        viewModel.onPendingItemLongPressed("rice")
        advanceUntilIdle()
        viewModel.onPendingItemClicked("beans")
        advanceUntilIdle()

        viewModel.onDeleteSelectedItems()
        advanceUntilIdle()

        assertEquals(listOf("rice", "beans"), repository.deletedRequests)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `purchased item click moves item back to pending`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "coffee")))
        advanceUntilIdle()

        viewModel.onPurchasedItemClicked("coffee")
        advanceUntilIdle()

        assertEquals(listOf("coffee"), repository.pendingRequests)
        assertEquals(listOf("coffee"), viewModel.uiState.value.pendingItems.map { it.id })
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `delete confirmation soft deletes the item and requests sync`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "tomatoes")))
        advanceUntilIdle()

        viewModel.onDeleteItemRequested(samplePendingItem(id = "tomatoes"))
        runCurrent()

        assertEquals(listOf("tomatoes"), repository.deletedRequests)
        assertEquals(DeleteUndoSnackbarState(count = 1), viewModel.uiState.value.deleteUndoSnackbar)
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `delete timeout commits deletion and requests sync`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "tomatoes")))
        advanceUntilIdle()

        viewModel.onDeleteItemRequested(samplePendingItem(id = "tomatoes"))
        runCurrent()
        advanceTimeBy(4_000)
        runCurrent()

        assertEquals(null, viewModel.uiState.value.deleteUndoSnackbar)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `delete undo restores item without requesting sync`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "olive-oil")))
        advanceUntilIdle()

        viewModel.onDeleteItemRequested(samplePurchasedItem(id = "olive-oil"))
        runCurrent()

        viewModel.onDeleteUndoRequested()
        runCurrent()

        assertEquals(listOf("olive-oil"), repository.restoredRequests)
        assertEquals(null, viewModel.uiState.value.deleteUndoSnackbar)
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `ui state emits pending and purchased sections`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "bananas"),
                samplePurchasedItem(id = "beans")
            )
        )

        viewModel.uiState.test {
            val firstState = awaitItem()
            val state = if (firstState.pendingItems.isEmpty() && firstState.purchasedItems.isEmpty()) {
                awaitItem()
            } else {
                firstState
            }
            assertEquals(listOf("bananas"), state.pendingItems.map { it.id })
            assertEquals(listOf("beans"), state.purchasedItems.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `background sync without manual request exposes isBackgroundSync only`() = runTest {
        syncScheduler.setSyncing(true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isManualSync)
        assertTrue(viewModel.uiState.value.isBackgroundSync)

        syncScheduler.setSyncing(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isManualSync)
        assertFalse(viewModel.uiState.value.isBackgroundSync)
    }

    @Test
    fun `mutation during manual sync keeps isManualSync true until queue drains`() = runTest {
        calDavConfigRepository.seed(
            enabled = true,
            serverUrl = "https://example.com",
            username = "user",
            listName = "Shopping"
        )
        advanceUntilIdle()

        syncScheduler.setSyncing(true)
        viewModel.onManualSyncRequested()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isManualSync)

        // Simulate mutation-triggered background sync overlapping with manual sync
        viewModel.onInputValueChange("Milk")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("Milk"), repository.addedNames)
        assertTrue(viewModel.uiState.value.isManualSync)

        syncScheduler.setSyncing(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isManualSync)
    }

    @Test
    fun `manual sync after local mutation request does not enqueue duplicate sync`() = runTest {
        calDavConfigRepository.seed(
            enabled = true,
            serverUrl = "https://example.com",
            username = "user",
            listName = "Shopping"
        )
        advanceUntilIdle()

        viewModel.onInputValueChange("Milk")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(1, syncScheduler.requestCount)

        viewModel.onManualSyncRequested()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isManualSync)
        assertEquals(1, syncScheduler.requestCount)

        syncScheduler.setSyncing(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isManualSync)
        assertFalse(viewModel.uiState.value.isBackgroundSync)
    }

    @Test
    fun `manual sync during background sync promotes to manual without duplicate request`() = runTest {
        calDavConfigRepository.seed(
            enabled = true,
            serverUrl = "https://example.com",
            username = "user",
            listName = "Shopping"
        )
        advanceUntilIdle()

        syncScheduler.setSyncing(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBackgroundSync)
        assertFalse(viewModel.uiState.value.isManualSync)
        assertEquals(0, syncScheduler.requestCount)

        viewModel.onManualSyncRequested()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isManualSync)
        assertFalse(viewModel.uiState.value.isBackgroundSync)
        assertEquals(0, syncScheduler.requestCount)

        syncScheduler.setSyncing(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isManualSync)
        assertFalse(viewModel.uiState.value.isBackgroundSync)
    }

    @Test
    fun `isManualSync reflects manual sync request while scheduler is syncing`() = runTest {
        calDavConfigRepository.seed(
            enabled = true,
            serverUrl = "https://example.com",
            username = "user",
            listName = "Shopping"
        )
        advanceUntilIdle()

        syncScheduler.setSyncing(true)
        viewModel.onManualSyncRequested()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isManualSync)
        assertFalse(viewModel.uiState.value.isBackgroundSync)

        syncScheduler.setSyncing(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isManualSync)
        assertFalse(viewModel.uiState.value.isBackgroundSync)
    }

    @Test
    fun `isSyncConfigured is true when caldav is ready to sync`() = runTest {
        calDavConfigRepository.seed(
            enabled = true,
            serverUrl = "https://example.com",
            username = "user",
            listName = "Shopping"
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSyncConfigured)
    }

    @Test
    fun `isSyncConfigured is false when caldav is not configured`() = runTest {
        calDavConfigRepository.seed(enabled = false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncConfigured)
    }

    @Test
    fun `onManualSyncRequested when configured invokes sync`() = runTest {
        calDavConfigRepository.seed(
            enabled = true,
            serverUrl = "https://example.com",
            username = "user",
            listName = "Shopping"
        )
        advanceUntilIdle()

        viewModel.onManualSyncRequested()
        advanceUntilIdle()

        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `onManualSyncRequested when not configured still invokes sync`() = runTest {
        calDavConfigRepository.seed(enabled = false)
        advanceUntilIdle()

        viewModel.onManualSyncRequested()
        advanceUntilIdle()

        assertEquals(1, syncScheduler.requestCount)
    }

    private fun samplePendingItem(
        id: String,
        name: String = id,
        purchaseCount: Int = 0
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = false,
        purchaseCount = purchaseCount,
        createdAt = 1L,
        updatedAt = 1L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )

    private fun samplePurchasedItem(id: String, name: String = id): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = true,
        purchaseCount = 3,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )
}
