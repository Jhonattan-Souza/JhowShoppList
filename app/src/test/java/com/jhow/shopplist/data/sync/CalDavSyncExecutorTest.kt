package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemRemoteMetadata
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.testing.FakeShoppingListRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalDavSyncExecutorTest {
    private val repository = FakeShoppingListRepository()
    private val planner = CalDavSyncPlanner()

    @Test
    fun `execute returns sync results for pushed local changes`() = runTest {
        val discoveryService = FakeDiscoveryService()
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = planner,
            discoveryService = discoveryService
        )
        repository.seedItems(
            listOf(
                sampleItem(id = "milk", syncStatus = SyncStatus.PENDING_INSERT)
            )
        )

        val outcome = executor.execute(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            collectionHref = "/lists/groceries/"
        ) as CalDavSyncOutcome.Success

        assertEquals(listOf("milk"), discoveryService.upsertedItems.map { it.id })
        assertEquals(1, outcome.syncedResults.size)
        assertEquals("uid-milk", outcome.syncedResults.single().remoteUid)
        assertEquals("/lists/groceries/milk.ics", outcome.syncedResults.single().remoteHref)
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
        assertEquals(1, (outcome as CalDavSyncOutcome.Success).importedCount)
    }

    @Test
    fun `execute applies remote updates to existing linked items`() = runTest {
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = planner,
            discoveryService = FakeDiscoveryService(
                remoteItems = listOf(
                    RemoteShoppingItemSnapshot(
                        remoteUid = "uid-milk",
                        summary = "Oat Milk",
                        isCompleted = true,
                        href = "/lists/groceries/uid-milk.ics",
                        eTag = "etag-milk-2",
                        lastModifiedAt = 5_000L
                    )
                )
            )
        )
        repository.seedItems(
            listOf(
                sampleItem(
                    id = "local-milk",
                    name = "Milk",
                    syncStatus = SyncStatus.SYNCED,
                    remoteUid = "uid-milk",
                    remoteHref = "/lists/groceries/uid-milk.ics",
                    remoteEtag = "etag-milk-1",
                    remoteLastModifiedAt = 1_000L
                )
            )
        )

        executor.execute(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            collectionHref = "/lists/groceries/"
        )

        val updatedItem = repository.getAllItems().single()
        assertEquals("Oat Milk", updatedItem.name)
        assertTrue(updatedItem.isPurchased)
        assertEquals(1, updatedItem.purchaseCount)
        assertEquals("etag-milk-2", updatedItem.remoteMetadata.remoteEtag)
        assertEquals(5_000L, updatedItem.remoteMetadata.remoteLastModifiedAt)
    }

    @Test
    fun `execute deletes remote item for pending local delete`() = runTest {
        val discoveryService = FakeDiscoveryService(
            remoteItems = listOf(
                RemoteShoppingItemSnapshot(
                    remoteUid = "uid-milk",
                    summary = "Milk",
                    isCompleted = false,
                    href = "/lists/groceries/uid-milk.ics",
                    eTag = "etag-milk",
                    lastModifiedAt = 5_000L
                )
            )
        )
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = planner,
            discoveryService = discoveryService
        )
        repository.seedItems(
            listOf(
                sampleItem(
                    id = "local-milk",
                    syncStatus = SyncStatus.PENDING_DELETE,
                    remoteUid = "uid-milk",
                    remoteHref = "/lists/groceries/uid-milk.ics",
                    remoteEtag = "etag-milk"
                ).copy(isDeleted = true)
            )
        )

        val outcome = executor.execute(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            collectionHref = "/lists/groceries/"
        ) as CalDavSyncOutcome.Success

        assertEquals(listOf("/lists/groceries/uid-milk.ics"), discoveryService.deletedHrefs)
        val deleteResult = outcome.syncedResults.single()
        assertEquals("local-milk", deleteResult.id)
        assertTrue(deleteResult.deletedRemotely)
        assertNull(deleteResult.remoteEtag)
    }

    private class FakeDiscoveryService(
        private val remoteItems: List<RemoteShoppingItemSnapshot> = emptyList()
    ) : CalDavDiscoveryService {
        val upsertedItems = mutableListOf<ShoppingItem>()
        val deletedHrefs = mutableListOf<String>()

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

        override suspend fun upsertTaskItem(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String,
            item: ShoppingItem
        ): CalDavTaskUpsertResult {
            upsertedItems += item
            return CalDavTaskUpsertResult(
                remoteUid = item.remoteMetadata.remoteUid ?: "uid-${item.id}",
                href = "$collectionHref${item.id}.ics",
                eTag = "etag-${item.id}",
                lastModifiedAt = 9_000L
            )
        }

        override suspend fun deleteTaskItem(
            serverUrl: String,
            username: String,
            password: String,
            href: String,
            eTag: String?
        ): CalDavTaskDeleteResult {
            deletedHrefs += href
            return CalDavTaskDeleteResult(deletedAt = 9_000L)
        }
    }

    private fun sampleItem(
        id: String,
        syncStatus: SyncStatus,
        name: String = id,
        remoteUid: String? = null,
        remoteHref: String? = null,
        remoteEtag: String? = null,
        remoteLastModifiedAt: Long? = null
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = false,
        purchaseCount = 0,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = false,
        syncStatus = syncStatus,
        remoteMetadata = ShoppingItemRemoteMetadata(
            remoteUid = remoteUid,
            remoteHref = remoteHref,
            remoteEtag = remoteEtag,
            remoteLastModifiedAt = remoteLastModifiedAt,
            lastSyncedAt = remoteLastModifiedAt
        )
    )
}
