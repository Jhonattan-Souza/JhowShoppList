package com.jhow.shopplist.presentation.shoppinglist

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UndoCoordinatorTest {
    private val dispatcher = StandardTestDispatcher()
    private val deletedItems = mutableListOf<ShoppingItem>()
    private val restoredItems = mutableListOf<ShoppingItem>()
    private val committedBatches = mutableListOf<List<ShoppingItem>>()

    @Test
    fun `single delete times out and commits`() = runTest(dispatcher) {
        val coordinator = coordinator(backgroundScope)
        val item = sampleItem(id = "milk")

        coordinator.onDelete(item)
        runCurrent()

        assertEquals(listOf(item), deletedItems)
        assertEquals(DeleteUndoSnackbarState(count = 1), coordinator.snackbarState.value)

        advanceTimeBy(4_000)
        runCurrent()

        assertEquals(listOf(listOf(item)), committedBatches)
        assertEquals(emptyList<ShoppingItem>(), restoredItems)
        assertNull(coordinator.snackbarState.value)
    }

    @Test
    fun `single delete undo restores item without committing`() = runTest(dispatcher) {
        val coordinator = coordinator(backgroundScope)
        val item = sampleItem(id = "bread")

        coordinator.onDelete(item)
        runCurrent()

        coordinator.onUndo()
        runCurrent()
        advanceTimeBy(4_000)
        runCurrent()

        assertEquals(listOf(item), restoredItems)
        assertEquals(emptyList<List<ShoppingItem>>(), committedBatches)
        assertNull(coordinator.snackbarState.value)
    }

    @Test
    fun `multiple deletes inside active window coalesce into one snackbar and commit batch`() = runTest(dispatcher) {
        val coordinator = coordinator(backgroundScope)
        val milk = sampleItem(id = "milk")
        val bread = sampleItem(id = "bread")

        coordinator.onDelete(milk)
        runCurrent()
        advanceTimeBy(2_000)
        coordinator.onDelete(bread)
        runCurrent()

        assertEquals(listOf(milk, bread), deletedItems)
        assertEquals(DeleteUndoSnackbarState(count = 2), coordinator.snackbarState.value)

        advanceTimeBy(3_999)
        runCurrent()
        assertEquals(emptyList<List<ShoppingItem>>(), committedBatches)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(listOf(listOf(milk, bread)), committedBatches)
        assertNull(coordinator.snackbarState.value)
    }

    @Test
    fun `undo restores all coalesced deletes without committing`() = runTest(dispatcher) {
        val coordinator = coordinator(backgroundScope)
        val milk = sampleItem(id = "milk")
        val bread = sampleItem(id = "bread")

        coordinator.onDelete(milk)
        runCurrent()
        coordinator.onDelete(bread)
        runCurrent()

        coordinator.onUndo()
        runCurrent()
        advanceTimeBy(4_000)
        runCurrent()

        assertEquals(listOf(milk, bread), restoredItems)
        assertEquals(emptyList<List<ShoppingItem>>(), committedBatches)
        assertNull(coordinator.snackbarState.value)
    }

    private fun coordinator(scope: CoroutineScope): UndoCoordinator = UndoCoordinator(
        timeoutMillis = 4_000,
        scope = scope,
        dispatcher = dispatcher,
        onOptimisticDelete = { deletedItems += it },
        onRestore = { restoredItems += it },
        onCommit = { committedBatches += it }
    )

    private fun sampleItem(id: String): ShoppingItem = ShoppingItem(
        id = id,
        name = id.replaceFirstChar { it.uppercase() },
        isPurchased = false,
        purchaseCount = 1,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )
}
