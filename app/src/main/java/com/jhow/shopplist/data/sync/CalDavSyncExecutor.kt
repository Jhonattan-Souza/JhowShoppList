package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import java.util.UUID
import javax.inject.Inject

class CalDavSyncExecutor @Inject constructor(
    private val repository: ShoppingListRepository,
    private val planner: CalDavSyncPlanner,
    private val mapper: VTodoMapper,
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

        if (plan.itemsToImport.isNotEmpty()) {
            repository.importRemoteItems(plan.itemsToImport)
        }

        if (plan.remoteUidsToDeleteLocally.isNotEmpty()) {
            repository.applyRemoteDeletes(plan.remoteUidsToDeleteLocally)
        }

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
