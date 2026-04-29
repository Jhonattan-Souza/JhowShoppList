package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemRemoteMetadata
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import com.jhow.shopplist.testing.FakeShoppingListRepository
import com.jhow.shopplist.testing.FakeShoppingSyncGateway
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPendingShoppingItemsUseCaseTest {
    private val repository = FakeShoppingListRepository()
    private val gateway = FakeShoppingSyncGateway()
    private val configRepository = FakeCalDavConfigRepository()
    private val useCase = SyncPendingShoppingItemsUseCase(repository, gateway, configRepository)

    @Test
    fun `returns zero when gateway returns empty success`() = runTest {
        gateway.nextOutcome = CalDavSyncOutcome.Success(emptyList(), 0)

        val syncedCount = useCase()

        assertEquals(0, syncedCount)
    }

    @Test
    fun `syncs pending items and marks them as synced`() = runTest {
        repository.seedItems(
            listOf(
                sampleItem(id = "milk", syncStatus = SyncStatus.PENDING_INSERT),
                sampleItem(id = "bread", syncStatus = SyncStatus.PENDING_DELETE, isDeleted = true)
            )
        )
        gateway.nextOutcome = CalDavSyncOutcome.Success(
            syncedResults = listOf(
                ShoppingItemSyncResult(id = "milk", serverUpdatedAt = 10_000L),
                ShoppingItemSyncResult(id = "bread", serverUpdatedAt = 10_001L)
            ),
            importedCount = 0
        )

        val syncedCount = useCase()

        assertEquals(2, syncedCount)
        assertEquals(1, repository.syncedResults.size)
        assertEquals(emptyList<ShoppingItem>(), repository.getPendingSyncItems())
    }

    @Test
    fun `missing remote list stores user action required instead of retrying`() = runTest {
        configRepository.saveConfig(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            newPassword = "secret"
        )
        gateway.nextOutcome = CalDavSyncOutcome.UserActionRequired(
            message = "Create remote list Groceries",
            pendingAction = CalDavPendingAction.CreateMissingList
        )

        val syncedCount = useCase()

        assertEquals(0, syncedCount)
        assertEquals(CalDavSyncState.UserActionRequired, configRepository.currentConfig.syncState)
        assertEquals(CalDavPendingAction.CreateMissingList, configRepository.currentConfig.pendingAction)
    }

    @Test
    fun `repository reconciliation imports remote only items and deletes remote deletions locally`() = runTest {
        repository.seedItems(
            listOf(
                sampleItem(id = "local-milk", syncStatus = SyncStatus.SYNCED, remoteUid = "uid-milk"),
                sampleItem(id = "local-bread", syncStatus = SyncStatus.SYNCED, remoteUid = "uid-bread")
            )
        )
        repository.importRemoteItems(
            listOf(
                RemoteShoppingItemSnapshot(
                    remoteUid = "uid-apples",
                    summary = "Apples",
                    isCompleted = true,
                    href = "/lists/groceries/apples.ics",
                    eTag = "etag-apples",
                    lastModifiedAt = 110L
                )
            )
        )
        repository.applyRemoteDeletes(setOf("uid-bread"))

        assertEquals(listOf("Apples"), repository.importedItems.map { it.summary })
        assertEquals(listOf("local-bread"), repository.remoteDeletedRequests)
        val remaining = repository.getAllItems().filter { !it.isDeleted }
        assertEquals(listOf("imported-uid-apples", "local-milk"), remaining.map { it.id }.sorted())
    }

    @Test
    fun `importRemoteItems merges known remote uids and imports new ones`() = runTest {
        repository.seedItems(
            listOf(
                sampleItem(id = "local-milk", syncStatus = SyncStatus.SYNCED, remoteUid = "uid-milk")
            )
        )
        repository.importRemoteItems(
            listOf(
                RemoteShoppingItemSnapshot(
                    remoteUid = "uid-milk",
                    summary = "Milk Updated",
                    isCompleted = true,
                    href = "/lists/groceries/milk.ics",
                    eTag = "etag-milk-2",
                    lastModifiedAt = 200L
                ),
                RemoteShoppingItemSnapshot(
                    remoteUid = "uid-apples",
                    summary = "Apples",
                    isCompleted = false,
                    href = "/lists/groceries/apples.ics",
                    eTag = "etag-apples",
                    lastModifiedAt = 200L
                )
            )
        )

        assertEquals(listOf("uid-apples"), repository.importedItems.map { it.remoteUid })
        val allItems = repository.getAllItems()
        assertEquals(2, allItems.size)
        val milk = allItems.single { it.remoteMetadata.remoteUid == "uid-milk" }
        assertEquals("local-milk", milk.id)
        assertEquals("Milk Updated", milk.name)
        assertEquals(true, milk.isPurchased)
        assertEquals("etag-milk-2", milk.remoteMetadata.remoteEtag)
    }

    @Test
    fun `applyRemoteDeletes tracks separately from user initiated deletes`() = runTest {
        repository.seedItems(
            listOf(
                sampleItem(id = "local-milk", syncStatus = SyncStatus.SYNCED, remoteUid = "uid-milk"),
                sampleItem(id = "local-bread", syncStatus = SyncStatus.SYNCED, remoteUid = "uid-bread")
            )
        )
        repository.softDeleteItem("local-milk")
        repository.applyRemoteDeletes(setOf("uid-bread"))

        assertEquals(listOf("local-milk"), repository.deletedRequests)
        assertEquals(listOf("local-bread"), repository.remoteDeletedRequests)
    }

    private fun sampleItem(
        id: String,
        syncStatus: SyncStatus,
        isDeleted: Boolean = false,
        remoteUid: String? = null
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = id,
        isPurchased = false,
        purchaseCount = 0,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = isDeleted,
        syncStatus = syncStatus,
        remoteMetadata = ShoppingItemRemoteMetadata(remoteUid = remoteUid)
    )
}
