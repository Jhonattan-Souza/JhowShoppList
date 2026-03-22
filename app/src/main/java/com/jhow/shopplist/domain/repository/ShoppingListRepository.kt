package com.jhow.shopplist.domain.repository

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    fun observePendingItems(): Flow<List<ShoppingItem>>

    fun observePurchasedItems(): Flow<List<ShoppingItem>>

    fun observeAllItemNames(): Flow<List<String>>

    suspend fun addItem(name: String)

    suspend fun findItemByName(name: String): ShoppingItem?

    suspend fun markItemsPurchased(ids: Set<String>)

    suspend fun markItemPending(id: String)

    suspend fun softDeleteItem(id: String)

    suspend fun getPendingSyncItems(): List<ShoppingItem>

    suspend fun markItemsSynced(results: List<ShoppingItemSyncResult>)
}
