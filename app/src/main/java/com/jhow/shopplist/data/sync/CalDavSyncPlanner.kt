package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import javax.inject.Inject

data class CalDavSyncPlan(
    val itemsToPush: List<ShoppingItem>,
    val itemsToDeleteRemotely: List<ShoppingItem>,
    val itemsToImport: List<RemoteShoppingItemSnapshot>,
    val itemsToUpdateLocally: List<RemoteShoppingItemSnapshot>,
    val itemsToMarkSynced: List<ShoppingItem>,
    val remoteUidsToDeleteLocally: Set<String>
)

class CalDavSyncPlanner @Inject constructor() {
    fun plan(localItems: List<ShoppingItem>, remoteItems: List<RemoteShoppingItemSnapshot>): CalDavSyncPlan {
        val remoteByUid = remoteItems.associateBy { it.remoteUid }
        val localByUid = localItems.mapNotNull { local ->
            local.remoteMetadata.remoteUid?.let { remoteUid -> remoteUid to local }
        }.toMap()

        return CalDavSyncPlan(
            itemsToPush = pendingItemsToPush(localItems, remoteByUid),
            itemsToDeleteRemotely = pendingItemsToDeleteRemotely(localItems, remoteByUid),
            itemsToImport = remoteItems.filter { remote -> remote.remoteUid !in localByUid },
            itemsToUpdateLocally = remoteItemsToUpdateLocally(remoteItems, localByUid),
            itemsToMarkSynced = pendingDeletesToMarkSynced(localItems, remoteByUid),
            remoteUidsToDeleteLocally = remoteDeletesToApplyLocally(localItems, remoteByUid)
        )
    }

    private fun pendingItemsToPush(
        localItems: List<ShoppingItem>,
        remoteByUid: Map<String, RemoteShoppingItemSnapshot>
    ): List<ShoppingItem> = localItems.filter { local ->
        when (local.syncStatus) {
            SyncStatus.PENDING_INSERT,
            SyncStatus.PENDING_UPDATE -> {
                val remote = local.remoteMetadata.remoteUid?.let(remoteByUid::get)
                remote == null || !shouldApplyRemoteUpdate(local, remote)
            }

            SyncStatus.PENDING_DELETE,
            SyncStatus.SYNCED -> false
        }
    }

    private fun remoteItemsToUpdateLocally(
        remoteItems: List<RemoteShoppingItemSnapshot>,
        localByUid: Map<String, ShoppingItem>
    ): List<RemoteShoppingItemSnapshot> = remoteItems.filter { remote ->
        val local = localByUid[remote.remoteUid] ?: return@filter false
        when (local.syncStatus) {
            SyncStatus.SYNCED -> remoteDiffersFromLocal(local, remote)
            SyncStatus.PENDING_INSERT,
            SyncStatus.PENDING_UPDATE -> shouldApplyRemoteUpdate(local, remote)
            SyncStatus.PENDING_DELETE -> false
        }
    }

    private fun pendingItemsToDeleteRemotely(
        localItems: List<ShoppingItem>,
        remoteByUid: Map<String, RemoteShoppingItemSnapshot>
    ): List<ShoppingItem> = localItems.filter { local ->
        local.syncStatus == SyncStatus.PENDING_DELETE &&
            local.remoteMetadata.remoteUid?.let(remoteByUid::containsKey) == true
    }

    private fun pendingDeletesToMarkSynced(
        localItems: List<ShoppingItem>,
        remoteByUid: Map<String, RemoteShoppingItemSnapshot>
    ): List<ShoppingItem> = localItems.filter { local ->
        local.syncStatus == SyncStatus.PENDING_DELETE &&
            local.remoteMetadata.remoteUid?.let(remoteByUid::containsKey) != true
    }

    private fun remoteDeletesToApplyLocally(
        localItems: List<ShoppingItem>,
        remoteByUid: Map<String, RemoteShoppingItemSnapshot>
    ): Set<String> = localItems.mapNotNull { local ->
        val remoteUid = local.remoteMetadata.remoteUid ?: return@mapNotNull null
        remoteUid.takeIf {
            local.syncStatus == SyncStatus.SYNCED &&
                remoteUid !in remoteByUid
        }
    }.toSet()

    private fun shouldApplyRemoteUpdate(
        local: ShoppingItem,
        remote: RemoteShoppingItemSnapshot
    ): Boolean {
        val remoteLastModifiedAt = remote.lastModifiedAt ?: return false
        val localKnownRemoteAt = local.remoteMetadata.remoteLastModifiedAt
            ?: local.remoteMetadata.lastSyncedAt
            ?: return false
        return remoteLastModifiedAt > localKnownRemoteAt && remoteLastModifiedAt > local.updatedAt
    }

    private fun remoteDiffersFromLocal(
        local: ShoppingItem,
        remote: RemoteShoppingItemSnapshot
    ): Boolean =
        local.name != remote.summary ||
            local.isPurchased != remote.isCompleted ||
            local.remoteMetadata.remoteHref != remote.href ||
            local.remoteMetadata.remoteEtag != remote.eTag ||
            local.remoteMetadata.remoteLastModifiedAt != remote.lastModifiedAt
}
