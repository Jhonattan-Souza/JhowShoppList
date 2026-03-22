package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.testing.FakeShoppingListRepository
import com.jhow.shopplist.testing.FakeShoppingSyncGateway
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPendingShoppingItemsUseCaseTest {
    private val repository = FakeShoppingListRepository()
    private val gateway = FakeShoppingSyncGateway()
    private val useCase = SyncPendingShoppingItemsUseCase(repository, gateway)

    @Test
    fun `returns zero when there is nothing to sync`() = runTest {
        val syncedCount = useCase()

        assertEquals(0, syncedCount)
        assertEquals(emptyList<List<ShoppingItem>>(), gateway.syncedBatches)
    }

    @Test
    fun `syncs pending items and marks them as synced`() = runTest {
        repository.seedItems(
            listOf(
                sampleItem(id = "milk", syncStatus = SyncStatus.PENDING_INSERT),
                sampleItem(id = "bread", syncStatus = SyncStatus.PENDING_DELETE, isDeleted = true)
            )
        )

        val syncedCount = useCase()

        assertEquals(2, syncedCount)
        assertEquals(listOf(listOf("milk", "bread")), gateway.syncedBatches.map { batch -> batch.map { it.id } })
        assertEquals(emptyList<ShoppingItem>(), repository.getPendingSyncItems())
        assertEquals(1, repository.syncedResults.size)
    }

    private fun sampleItem(
        id: String,
        syncStatus: SyncStatus,
        isDeleted: Boolean = false
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = id,
        isPurchased = false,
        purchaseCount = 0,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = isDeleted,
        syncStatus = syncStatus
    )
}
