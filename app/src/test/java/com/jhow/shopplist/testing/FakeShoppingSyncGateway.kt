package com.jhow.shopplist.testing

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.sync.ShoppingListSyncGateway

class FakeShoppingSyncGateway : ShoppingListSyncGateway {
    val syncedBatches = mutableListOf<List<ShoppingItem>>()

    override suspend fun sync(items: List<ShoppingItem>): List<ShoppingItemSyncResult> {
        syncedBatches += items
        return items.mapIndexed { index, item ->
            ShoppingItemSyncResult(
                id = item.id,
                serverUpdatedAt = 10_000L + index
            )
        }
    }
}
