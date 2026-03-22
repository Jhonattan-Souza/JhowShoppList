package com.jhow.shopplist.testing

import com.jhow.shopplist.core.search.ShoppingSearch
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeShoppingListRepository : ShoppingListRepository {
    private val items = MutableStateFlow<List<ShoppingItem>>(emptyList())

    val addedNames = mutableListOf<String>()
    val purchasedRequests = mutableListOf<Set<String>>()
    val pendingRequests = mutableListOf<String>()
    val deletedRequests = mutableListOf<String>()
    val syncedResults = mutableListOf<List<ShoppingItemSyncResult>>()

    override fun observePendingItems(): Flow<List<ShoppingItem>> =
        items.map { currentItems -> currentItems.filter { !it.isPurchased && !it.isDeleted } }

    override fun observePurchasedItems(): Flow<List<ShoppingItem>> =
        items.map { currentItems -> currentItems.filter { it.isPurchased && !it.isDeleted } }

    override fun observeAllItemNames(): Flow<List<String>> = items.map { currentItems ->
        currentItems
            .asSequence()
            .filter { !it.isDeleted }
            .sortedWith(compareByDescending<ShoppingItem> { it.purchaseCount }.thenBy { it.name.lowercase() })
            .distinctBy { ShoppingSearch.normalize(it.name) }
            .map { it.name }
            .toList()
    }

    override suspend fun addItem(name: String) {
        addedNames += name
        val now = 1_000L + items.value.size
        items.value = items.value + ShoppingItem(
            id = "generated-${items.value.size}",
            name = name,
            isPurchased = false,
            purchaseCount = 0,
            createdAt = now,
            updatedAt = now,
            isDeleted = false,
            syncStatus = SyncStatus.PENDING_INSERT
        )
    }

    override suspend fun findItemByName(name: String): ShoppingItem? =
        items.value
            .asSequence()
            .filter { !it.isDeleted && ShoppingSearch.normalize(it.name) == ShoppingSearch.normalize(name) }
            .sortedWith(compareBy<ShoppingItem> { it.isPurchased }.thenByDescending { it.purchaseCount }.thenByDescending { it.updatedAt })
            .firstOrNull()

    override suspend fun markItemsPurchased(ids: Set<String>) {
        purchasedRequests += ids
        items.value = items.value.map { item ->
            if (item.id in ids && !item.isPurchased) {
                item.copy(
                    isPurchased = true,
                    purchaseCount = item.purchaseCount + 1,
                    updatedAt = item.updatedAt + 100,
                    syncStatus = if (item.syncStatus == SyncStatus.PENDING_INSERT) {
                        SyncStatus.PENDING_INSERT
                    } else {
                        SyncStatus.PENDING_UPDATE
                    }
                )
            } else {
                item
            }
        }
    }

    override suspend fun markItemPending(id: String) {
        pendingRequests += id
        items.value = items.value.map { item ->
            if (item.id == id && item.isPurchased) {
                item.copy(
                    isPurchased = false,
                    updatedAt = item.updatedAt + 100,
                    syncStatus = if (item.syncStatus == SyncStatus.PENDING_INSERT) {
                        SyncStatus.PENDING_INSERT
                    } else {
                        SyncStatus.PENDING_UPDATE
                    }
                )
            } else {
                item
            }
        }
    }

    override suspend fun softDeleteItem(id: String) {
        deletedRequests += id
        items.value = items.value.map { item ->
            if (item.id == id && !item.isDeleted) {
                item.copy(
                    isDeleted = true,
                    updatedAt = item.updatedAt + 100,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
            } else {
                item
            }
        }
    }

    override suspend fun getPendingSyncItems(): List<ShoppingItem> =
        items.value.filter { it.syncStatus != SyncStatus.SYNCED }

    override suspend fun markItemsSynced(results: List<ShoppingItemSyncResult>) {
        syncedResults += results
        val resultMap = results.associateBy(ShoppingItemSyncResult::id)
        items.value = items.value.map { item ->
            val result = resultMap[item.id] ?: return@map item
            item.copy(
                updatedAt = result.serverUpdatedAt,
                syncStatus = SyncStatus.SYNCED
            )
        }
    }

    fun seedItems(newItems: List<ShoppingItem>) {
        items.value = newItems
    }
}
