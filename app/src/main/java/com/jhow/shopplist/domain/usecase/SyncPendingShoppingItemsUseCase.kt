package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import com.jhow.shopplist.domain.sync.ShoppingListSyncGateway
import javax.inject.Inject

class SyncPendingShoppingItemsUseCase @Inject constructor(
    private val repository: ShoppingListRepository,
    private val syncGateway: ShoppingListSyncGateway
) {
    suspend operator fun invoke(): Int {
        val pendingItems = repository.getPendingSyncItems()
        if (pendingItems.isEmpty()) return 0

        val syncResults = syncGateway.sync(pendingItems)
        repository.markItemsSynced(syncResults)
        return syncResults.size
    }
}
