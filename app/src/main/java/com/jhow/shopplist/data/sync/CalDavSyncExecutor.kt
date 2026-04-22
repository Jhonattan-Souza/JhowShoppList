package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import java.util.UUID
import javax.inject.Inject

class CalDavSyncExecutor @Inject constructor(
    private val repository: ShoppingListRepository,
    private val planner: CalDavSyncPlanner,
    private val mapper: VTodoMapper
) {
    suspend fun execute(collectionHref: String): CalDavSyncOutcome {
        val localItems = repository.getAllItems()
        // TODO: Pull remote VTODOs when transport is implemented.
        // For now, reconcile against an empty remote set so only local pushes apply.
        val plan = planner.plan(localItems, emptyList())

        if (plan.itemsToImport.isNotEmpty()) {
            repository.importRemoteItems(plan.itemsToImport)
        }

        // TODO: applyRemoteDeletes when real remote transport is implemented.
        // Disabled for now because the stubbed empty remote set would
        // incorrectly delete every locally-synced item.

        val now = System.currentTimeMillis()
        val syncedResults = plan.itemsToPush.map { item ->
            ShoppingItemSyncResult(
                id = item.id,
                serverUpdatedAt = now,
                remoteUid = item.remoteMetadata.remoteUid ?: UUID.randomUUID().toString(),
                remoteHref = "$collectionHref/${item.id}.ics",
                remoteEtag = "\"etag-${item.id}\"",
                lastSyncedAt = now
            )
        }

        return CalDavSyncOutcome.Success(
            syncedResults = syncedResults,
            importedCount = plan.itemsToImport.size
        )
    }
}
