package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemRemoteMetadata
import com.jhow.shopplist.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CalDavSyncPlannerTest {

    @Test
    fun `planner prefers local changes but applies remote deletes`() {
        val planner = CalDavSyncPlanner()
        val localItems = listOf(
            sampleLocal(id = "milk", remoteUid = "uid-milk", syncStatus = SyncStatus.PENDING_UPDATE, name = "Oat Milk"),
            sampleLocal(id = "bread", remoteUid = "uid-bread", syncStatus = SyncStatus.SYNCED, name = "Bread")
        )
        val remoteItems = listOf(
            sampleRemote(remoteUid = "uid-milk", summary = "Milk", isCompleted = false),
            sampleRemote(remoteUid = "uid-apples", summary = "Apples", isCompleted = true)
        )

        val plan = planner.plan(localItems = localItems, remoteItems = remoteItems)

        assertEquals(listOf("milk"), plan.itemsToPush.map { it.id })
        assertEquals(emptyList<String>(), plan.itemsToDeleteRemotely.map { it.id })
        assertEquals(listOf("Apples"), plan.itemsToImport.map { it.summary })
        assertEquals(emptyList<String>(), plan.itemsToUpdateLocally.map { it.summary })
        assertEquals(emptyList<String>(), plan.itemsToMarkSynced.map { it.id })
        assertEquals(setOf("uid-bread"), plan.remoteUidsToDeleteLocally)
    }

    @Test
    fun `PENDING_INSERT with null remoteUid goes to itemsToPush and not to remoteUidsToDeleteLocally`() {
        val planner = CalDavSyncPlanner()
        val localItems = listOf(
            sampleLocal(id = "new-item", remoteUid = null, syncStatus = SyncStatus.PENDING_INSERT)
        )

        val plan = planner.plan(localItems = localItems, remoteItems = emptyList())

        assertEquals(listOf("new-item"), plan.itemsToPush.map { it.id })
        assertEquals(emptyList<String>(), plan.itemsToDeleteRemotely.map { it.id })
        assertEquals(emptyList<String>(), plan.itemsToUpdateLocally.map { it.summary })
        assertEquals(emptyList<String>(), plan.itemsToMarkSynced.map { it.id })
        assertEquals(emptySet<String>(), plan.remoteUidsToDeleteLocally)
    }

    @Test
    fun `PENDING_DELETE item whose UID still exists remotely is deleted remotely and not deleted locally`() {
        val planner = CalDavSyncPlanner()
        val localItems = listOf(
            sampleLocal(id = "stale-item", remoteUid = "uid-stale", syncStatus = SyncStatus.PENDING_DELETE)
        )
        val remoteItems = listOf(
            sampleRemote(remoteUid = "uid-stale", summary = "Stale Item", isCompleted = false)
        )

        val plan = planner.plan(localItems = localItems, remoteItems = remoteItems)

        assertEquals(emptyList<String>(), plan.itemsToPush.map { it.id })
        assertEquals(listOf("stale-item"), plan.itemsToDeleteRemotely.map { it.id })
        assertEquals(emptyList<String>(), plan.itemsToMarkSynced.map { it.id })
        assertEquals(emptySet<String>(), plan.remoteUidsToDeleteLocally)
    }

    @Test
    fun `PENDING_DELETE item whose UID is already absent remotely is marked synced locally`() {
        val planner = CalDavSyncPlanner()
        val localItems = listOf(
            sampleLocal(id = "stale-item", remoteUid = "uid-stale", syncStatus = SyncStatus.PENDING_DELETE)
        )

        val plan = planner.plan(localItems = localItems, remoteItems = emptyList())

        assertEquals(emptyList<String>(), plan.itemsToPush.map { it.id })
        assertEquals(emptyList<String>(), plan.itemsToDeleteRemotely.map { it.id })
        assertEquals(listOf("stale-item"), plan.itemsToMarkSynced.map { it.id })
        assertEquals(emptySet<String>(), plan.remoteUidsToDeleteLocally)
    }

    @Test
    fun `empty inputs produce an all-empty plan`() {
        val planner = CalDavSyncPlanner()

        val plan = planner.plan(localItems = emptyList(), remoteItems = emptyList())

        assertEquals(emptyList<ShoppingItem>(), plan.itemsToPush)
        assertEquals(emptyList<ShoppingItem>(), plan.itemsToDeleteRemotely)
        assertEquals(emptyList<RemoteShoppingItemSnapshot>(), plan.itemsToImport)
        assertEquals(emptyList<RemoteShoppingItemSnapshot>(), plan.itemsToUpdateLocally)
        assertEquals(emptyList<ShoppingItem>(), plan.itemsToMarkSynced)
        assertEquals(emptySet<String>(), plan.remoteUidsToDeleteLocally)
    }

    @Test
    fun `all-remote-no-local imports every remote item`() {
        val planner = CalDavSyncPlanner()
        val remoteItems = listOf(
            sampleRemote(remoteUid = "uid-1", summary = "One", isCompleted = false),
            sampleRemote(remoteUid = "uid-2", summary = "Two", isCompleted = true)
        )

        val plan = planner.plan(localItems = emptyList(), remoteItems = remoteItems)

        assertEquals(emptyList<String>(), plan.itemsToPush)
        assertEquals(emptyList<String>(), plan.itemsToDeleteRemotely.map { it.id })
        assertEquals(listOf("One", "Two"), plan.itemsToImport.map { it.summary })
        assertEquals(emptyList<String>(), plan.itemsToUpdateLocally.map { it.summary })
        assertEquals(emptyList<String>(), plan.itemsToMarkSynced.map { it.id })
        assertEquals(emptySet<String>(), plan.remoteUidsToDeleteLocally)
    }

    @Test
    fun `all-local-synced-no-remote deletes every synced UID locally`() {
        val planner = CalDavSyncPlanner()
        val localItems = listOf(
            sampleLocal(id = "milk", remoteUid = "uid-milk", syncStatus = SyncStatus.SYNCED),
            sampleLocal(id = "bread", remoteUid = "uid-bread", syncStatus = SyncStatus.SYNCED)
        )

        val plan = planner.plan(localItems = localItems, remoteItems = emptyList())

        assertEquals(emptyList<ShoppingItem>(), plan.itemsToPush)
        assertEquals(emptyList<ShoppingItem>(), plan.itemsToDeleteRemotely)
        assertEquals(emptyList<RemoteShoppingItemSnapshot>(), plan.itemsToImport)
        assertEquals(emptyList<RemoteShoppingItemSnapshot>(), plan.itemsToUpdateLocally)
        assertEquals(emptyList<ShoppingItem>(), plan.itemsToMarkSynced)
        assertEquals(setOf("uid-milk", "uid-bread"), plan.remoteUidsToDeleteLocally)
    }

    @Test
    fun `remote update for synced linked item goes to itemsToUpdateLocally`() {
        val planner = CalDavSyncPlanner()
        val localItems = listOf(
            sampleLocal(
                id = "milk",
                remoteUid = "uid-milk",
                syncStatus = SyncStatus.SYNCED,
                name = "Milk",
                remoteLastModifiedAt = 100L
            )
        )
        val remoteItems = listOf(
            sampleRemote(remoteUid = "uid-milk", summary = "Oat Milk", isCompleted = true, lastModifiedAt = 200L)
        )

        val plan = planner.plan(localItems = localItems, remoteItems = remoteItems)

        assertEquals(listOf("Oat Milk"), plan.itemsToUpdateLocally.map { it.summary })
        assertEquals(emptyList<String>(), plan.itemsToPush.map { it.id })
    }

    @Test
    fun `newer remote update wins over pending local update`() {
        val planner = CalDavSyncPlanner()
        val localItems = listOf(
            sampleLocal(
                id = "milk",
                remoteUid = "uid-milk",
                syncStatus = SyncStatus.PENDING_UPDATE,
                name = "Local Milk",
                updatedAt = 150L,
                remoteLastModifiedAt = 100L,
                lastSyncedAt = 100L
            )
        )
        val remoteItems = listOf(
            sampleRemote(remoteUid = "uid-milk", summary = "Remote Milk", isCompleted = true, lastModifiedAt = 250L)
        )

        val plan = planner.plan(localItems = localItems, remoteItems = remoteItems)

        assertEquals(emptyList<String>(), plan.itemsToPush.map { it.id })
        assertEquals(listOf("Remote Milk"), plan.itemsToUpdateLocally.map { it.summary })
    }

    private fun sampleLocal(
        id: String,
        remoteUid: String? = null,
        syncStatus: SyncStatus,
        name: String = id,
        updatedAt: Long = 2L,
        remoteLastModifiedAt: Long? = null,
        lastSyncedAt: Long? = null
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = false,
        purchaseCount = 0,
        createdAt = 1L,
        updatedAt = updatedAt,
        isDeleted = false,
        syncStatus = syncStatus,
        remoteMetadata = ShoppingItemRemoteMetadata(
            remoteUid = remoteUid,
            remoteHref = remoteUid?.let { "/lists/groceries/$it.ics" },
            remoteEtag = remoteUid?.let { "etag-$it" },
            remoteLastModifiedAt = remoteLastModifiedAt,
            lastSyncedAt = lastSyncedAt
        )
    )

    private fun sampleRemote(
        remoteUid: String,
        summary: String,
        isCompleted: Boolean,
        lastModifiedAt: Long = 100L
    ): RemoteShoppingItemSnapshot = RemoteShoppingItemSnapshot(
        remoteUid = remoteUid,
        summary = summary,
        isCompleted = isCompleted,
        href = "/lists/groceries/$remoteUid.ics",
        eTag = "etag-$remoteUid",
        lastModifiedAt = lastModifiedAt
    )
}
