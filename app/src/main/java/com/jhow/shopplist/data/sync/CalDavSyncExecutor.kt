package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class CalDavSyncExecutor @Inject constructor(
    private val repository: ShoppingListRepository,
    private val planner: CalDavSyncPlanner,
    private val discoveryService: CalDavDiscoveryService
) {
    suspend fun execute(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String
    ): CalDavSyncOutcome {
        val localItems = repository.getAllItems()
        val remoteItems = discoveryService.fetchTaskItems(
            serverUrl = serverUrl,
            username = username,
            password = password,
            collectionHref = collectionHref
        )
        val plan = planner.plan(localItems, remoteItems)
        applyRemoteChangesLocally(plan)

        val now = System.currentTimeMillis()
        val remoteByUid = remoteItems.associateBy { it.remoteUid }
        val syncedResults = buildList {
            addAll(markDeletesAlreadySynced(plan.itemsToMarkSynced, now))
            addAll(pushPendingItems(plan.itemsToPush, serverUrl, username, password, collectionHref, now))
            addAll(deletePendingItems(plan.itemsToDeleteRemotely, remoteByUid, serverUrl, username, password, now))
        }

        return CalDavSyncOutcome.Success(
            syncedResults = syncedResults,
            importedCount = plan.itemsToImport.size
        )
    }

    private suspend fun applyRemoteChangesLocally(plan: CalDavSyncPlan) {
        val remoteItemsToApplyLocally = plan.itemsToImport + plan.itemsToUpdateLocally
        if (remoteItemsToApplyLocally.isNotEmpty()) {
            repository.importRemoteItems(remoteItemsToApplyLocally)
        }

        if (plan.remoteUidsToDeleteLocally.isNotEmpty()) {
            repository.applyRemoteDeletes(plan.remoteUidsToDeleteLocally)
        }
    }

    private fun markDeletesAlreadySynced(
        items: List<com.jhow.shopplist.domain.model.ShoppingItem>,
        now: Long
    ): List<ShoppingItemSyncResult> =
        items.map { item ->
            ShoppingItemSyncResult(
                id = item.id,
                serverUpdatedAt = now,
                remoteUid = item.remoteMetadata.remoteUid,
                remoteHref = item.remoteMetadata.remoteHref,
                remoteEtag = item.remoteMetadata.remoteEtag,
                remoteLastModifiedAt = item.remoteMetadata.remoteLastModifiedAt,
                lastSyncedAt = now
            )
        }

    private suspend fun pushPendingItems(
        items: List<com.jhow.shopplist.domain.model.ShoppingItem>,
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String,
        now: Long
    ): List<ShoppingItemSyncResult> = items.map { item ->
        val result = discoveryService.upsertTaskItem(
            serverUrl = serverUrl,
            username = username,
            password = password,
            collectionHref = collectionHref,
            item = item
        )
        ShoppingItemSyncResult(
            id = item.id,
            serverUpdatedAt = result.lastModifiedAt ?: now,
            remoteUid = result.remoteUid,
            remoteHref = result.href,
            remoteEtag = result.eTag,
            remoteLastModifiedAt = result.lastModifiedAt,
            lastSyncedAt = now
        )
    }

    private suspend fun deletePendingItems(
        items: List<com.jhow.shopplist.domain.model.ShoppingItem>,
        remoteByUid: Map<String, com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot>,
        serverUrl: String,
        username: String,
        password: String,
        now: Long
    ): List<ShoppingItemSyncResult> = items.map { item ->
        val remote = item.remoteMetadata.remoteUid?.let(remoteByUid::get)
        val href = item.remoteMetadata.remoteHref ?: remote?.href
            ?: error("Cannot delete remote item without href for ${item.id}")
        val eTag = remote?.eTag ?: item.remoteMetadata.remoteEtag
        val result = discoveryService.deleteTaskItem(
            serverUrl = serverUrl,
            username = username,
            password = password,
            href = href,
            eTag = eTag
        )
        ShoppingItemSyncResult(
            id = item.id,
            serverUpdatedAt = result.deletedAt ?: now,
            remoteUid = item.remoteMetadata.remoteUid,
            remoteHref = href,
            remoteEtag = null,
            remoteLastModifiedAt = null,
            lastSyncedAt = now,
            deletedRemotely = true
        )
    }
}
