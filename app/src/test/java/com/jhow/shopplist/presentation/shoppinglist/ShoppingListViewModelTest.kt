package com.jhow.shopplist.presentation.shoppinglist

import app.cash.turbine.test
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.domain.usecase.AddShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.MarkPurchasedItemPendingUseCase
import com.jhow.shopplist.domain.usecase.MarkSelectedItemsPurchasedUseCase
import com.jhow.shopplist.domain.usecase.ObservePendingItemsUseCase
import com.jhow.shopplist.domain.usecase.ObservePurchasedItemsUseCase
import com.jhow.shopplist.testing.FakeShoppingListRepository
import com.jhow.shopplist.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeShoppingListRepository
    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        repository = FakeShoppingListRepository()
        viewModel = ShoppingListViewModel(
            observePendingItemsUseCase = ObservePendingItemsUseCase(repository),
            observePurchasedItemsUseCase = ObservePurchasedItemsUseCase(repository),
            addShoppingItemUseCase = AddShoppingItemUseCase(repository),
            markSelectedItemsPurchasedUseCase = MarkSelectedItemsPurchasedUseCase(repository),
            markPurchasedItemPendingUseCase = MarkPurchasedItemPendingUseCase(repository)
        )
    }

    @Test
    fun `adding an item trims input and clears the field`() = runTest {
        viewModel.onInputValueChange("  Oats  ")

        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("Oats"), repository.addedNames)
        assertEquals("", viewModel.uiState.value.inputValue)
    }

    @Test
    fun `pending item click toggles selection`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "rice")))
        advanceUntilIdle()

        viewModel.onPendingItemClicked("rice")
        advanceUntilIdle()
        assertEquals(setOf("rice"), viewModel.uiState.value.selectedIds)

        viewModel.onPendingItemClicked("rice")
        advanceUntilIdle()
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
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

        viewModel.onPendingItemClicked("milk")
        viewModel.onPendingItemClicked("tea")
        advanceUntilIdle()

        viewModel.onPurchaseSelectedItems()
        advanceUntilIdle()

        assertEquals(listOf(setOf("milk", "tea")), repository.purchasedRequests)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
        assertEquals(listOf("milk", "tea"), viewModel.uiState.value.purchasedItems.map { it.id })
    }

    @Test
    fun `selected ids are dropped when items leave the pending section`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "eggs")))
        advanceUntilIdle()

        viewModel.onPendingItemClicked("eggs")
        advanceUntilIdle()
        repository.seedItems(listOf(samplePurchasedItem(id = "eggs")))
        advanceUntilIdle()

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

    private fun samplePendingItem(id: String, purchaseCount: Int = 0): ShoppingItem = ShoppingItem(
        id = id,
        name = id,
        isPurchased = false,
        purchaseCount = purchaseCount,
        createdAt = 1L,
        updatedAt = 1L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )

    private fun samplePurchasedItem(id: String): ShoppingItem = ShoppingItem(
        id = id,
        name = id,
        isPurchased = true,
        purchaseCount = 3,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )
}
