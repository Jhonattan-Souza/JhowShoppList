package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemRemoteMetadata
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.testing.FakeShoppingListRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CalDavSyncExecutorTest {
    private val repository = FakeShoppingListRepository()
    private val planner = CalDavSyncPlanner()
    private val mapper = VTodoMapper()
    private val executor = CalDavSyncExecutor(repository, planner, mapper)

    @Test
    fun `execute does not call markItemsSynced`() = runTest {
        repository.seedItems(
            listOf(
                sampleItem(id = "milk", syncStatus = SyncStatus.PENDING_INSERT)
            )
        )

        executor.execute(collectionHref = "/lists/groceries/")

        assertEquals(0, repository.syncedResults.size)
    }

    @Test
    fun `execute does not apply remote deletes when remote set is empty`() = runTest {
        repository.seedItems(
            listOf(
                sampleItem(id = "local-milk", syncStatus = SyncStatus.SYNCED, remoteUid = "uid-milk")
            )
        )

        executor.execute(collectionHref = "/lists/groceries/")

        assertEquals(0, repository.applyRemoteDeletesCallCount)
    }

    private fun sampleItem(
        id: String,
        syncStatus: SyncStatus,
        remoteUid: String? = null
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = id,
        isPurchased = false,
        purchaseCount = 0,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = false,
        syncStatus = syncStatus,
        remoteMetadata = ShoppingItemRemoteMetadata(remoteUid = remoteUid)
    )
}
