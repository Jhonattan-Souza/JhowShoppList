package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
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

    @Test
    fun `execute does not call markItemsSynced`() = runTest {
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = planner,
            discoveryService = FakeDiscoveryService()
        )
        repository.seedItems(
            listOf(
                sampleItem(id = "milk", syncStatus = SyncStatus.PENDING_INSERT)
            )
        )

        executor.execute(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            collectionHref = "/lists/groceries/"
        )

        assertEquals(0, repository.syncedResults.size)
    }

    @Test
    fun `execute imports remote items and applies remote deletes from fetched collection items`() = runTest {
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = planner,
            discoveryService = FakeDiscoveryService(
                remoteItems = listOf(
                    RemoteShoppingItemSnapshot(
                        remoteUid = "uid-apples",
                        summary = "Apples",
                        isCompleted = true,
                        href = "/lists/groceries/uid-apples.ics",
                        eTag = "etag-apples",
                        lastModifiedAt = 5_000L
                    )
                )
            )
        )
        repository.seedItems(
            listOf(
                sampleItem(id = "local-bread", syncStatus = SyncStatus.SYNCED, remoteUid = "uid-bread")
            )
        )

        val outcome = executor.execute(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            collectionHref = "/lists/groceries/"
        )

        assertEquals(1, repository.importedItems.size)
        assertEquals("uid-apples", repository.importedItems.single().remoteUid)
        assertEquals(1, repository.applyRemoteDeletesCallCount)
        assertEquals(listOf("local-bread"), repository.remoteDeletedRequests)
        assertEquals(1, (outcome as com.jhow.shopplist.domain.model.CalDavSyncOutcome.Success).importedCount)
    }

    private class FakeDiscoveryService(
        private val remoteItems: List<RemoteShoppingItemSnapshot> = emptyList()
    ) : CalDavDiscoveryService {
        override suspend fun findTaskCollections(
            serverUrl: String,
            username: String,
            password: String
        ): List<CalDavCollectionCandidate> = emptyList()

        override suspend fun createTaskCollection(
            serverUrl: String,
            username: String,
            password: String,
            listName: String
        ): String = "$serverUrl/$listName/"

        override suspend fun fetchTaskItems(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String
        ): List<RemoteShoppingItemSnapshot> = remoteItems
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
