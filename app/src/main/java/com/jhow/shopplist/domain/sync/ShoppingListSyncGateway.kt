package com.jhow.shopplist.domain.sync

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult

interface ShoppingListSyncGateway {
    suspend fun sync(items: List<ShoppingItem>): List<ShoppingItemSyncResult>
}
