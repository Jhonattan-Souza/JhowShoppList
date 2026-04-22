package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import javax.inject.Inject

data class CalDavSyncPlan(
    val itemsToPush: List<ShoppingItem>,
    val itemsToImport: List<RemoteShoppingItemSnapshot>,
    val remoteUidsToDeleteLocally: Set<String>
)

class CalDavSyncPlanner @Inject constructor() {
    fun plan(localItems: List<ShoppingItem>, remoteItems: List<RemoteShoppingItemSnapshot>): CalDavSyncPlan {
        val remoteByUid = remoteItems.associateBy { it.remoteUid }
        val localByUid = localItems.mapNotNull { local ->
            local.remoteMetadata.remoteUid?.let { remoteUid -> remoteUid to local }
        }.toMap()

        val itemsToPush = localItems.filter { local ->
            local.syncStatus != SyncStatus.SYNCED
        }

        val itemsToImport = remoteItems.filter { remote -> remote.remoteUid !in localByUid }
        val remoteUidsToDeleteLocally = localByUid.keys - remoteByUid.keys

        return CalDavSyncPlan(
            itemsToPush = itemsToPush,
            itemsToImport = itemsToImport,
            remoteUidsToDeleteLocally = remoteUidsToDeleteLocally
        )
    }
}
