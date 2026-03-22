package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.sync.ShoppingListSyncGateway
import javax.inject.Inject

class FakeShoppingListSyncGateway @Inject constructor() : ShoppingListSyncGateway {
    override suspend fun sync(items: List<ShoppingItem>): List<ShoppingItemSyncResult> {
        val baseTimestamp = System.currentTimeMillis()
        return items.mapIndexed { index, item ->
            ShoppingItemSyncResult(
                id = item.id,
                serverUpdatedAt = baseTimestamp + index
            )
        }
    }
}
