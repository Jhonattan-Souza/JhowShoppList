package com.jhow.shopplist.data.repository

import com.jhow.shopplist.core.dispatchers.IoDispatcher
import com.jhow.shopplist.data.local.dao.ShoppingItemDao
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemSyncResult
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ShoppingListRepositoryImpl @Inject constructor(
    private val shoppingItemDao: ShoppingItemDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ShoppingListRepository {

    override fun observePendingItems(): Flow<List<ShoppingItem>> =
        shoppingItemDao.observePendingItems().map { items -> items.map { it.toDomain() } }

    override fun observePurchasedItems(): Flow<List<ShoppingItem>> =
        shoppingItemDao.observePurchasedItems().map { items -> items.map { it.toDomain() } }

    override suspend fun addItem(name: String) {
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            shoppingItemDao.insertItem(
                ShoppingItemEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    isPurchased = false,
                    purchaseCount = 0,
                    createdAt = now,
                    updatedAt = now,
                    isDeleted = false,
                    syncStatus = SyncStatus.PENDING_INSERT
                )
            )
        }
    }

    override suspend fun markItemsPurchased(ids: Set<String>) {
        if (ids.isEmpty()) return
        withContext(ioDispatcher) {
            shoppingItemDao.markItemsPurchased(
                ids = ids.toList(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun markItemPending(id: String) {
        withContext(ioDispatcher) {
            shoppingItemDao.markItemPending(
                id = id,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun softDeleteItem(id: String) {
        withContext(ioDispatcher) {
            shoppingItemDao.softDeleteItem(
                id = id,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun getPendingSyncItems(): List<ShoppingItem> = withContext(ioDispatcher) {
        shoppingItemDao.getPendingSyncItems().map { it.toDomain() }
    }

    override suspend fun markItemsSynced(results: List<ShoppingItemSyncResult>) {
        if (results.isEmpty()) return
        withContext(ioDispatcher) {
            shoppingItemDao.markItemsSynced(
                items = results.associate { result -> result.id to result.serverUpdatedAt }
            )
        }
    }
}
