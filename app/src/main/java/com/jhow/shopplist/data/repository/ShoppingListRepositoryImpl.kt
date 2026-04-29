package com.jhow.shopplist.data.repository

import com.jhow.shopplist.core.dispatchers.IoDispatcher
import com.jhow.shopplist.core.search.ShoppingSearch
import com.jhow.shopplist.data.local.dao.ShoppingItemDao
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
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

    override fun observeAllItemNames(): Flow<List<String>> = shoppingItemDao.observeAllItemNames()

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

    override suspend fun findItemByName(name: String): ShoppingItem? = withContext(ioDispatcher) {
        shoppingItemDao.findItemByNormalizedName(ShoppingSearch.normalize(name))?.toDomain()
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

    override suspend fun getAllItems(): List<ShoppingItem> = withContext(ioDispatcher) {
        shoppingItemDao.getAllItems().map { it.toDomain() }
    }

    override suspend fun importRemoteItems(items: List<RemoteShoppingItemSnapshot>) {
        if (items.isEmpty()) return
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val existingByRemoteUid = shoppingItemDao.getAllItems()
                .mapNotNull { existing ->
                    existing.remoteUid?.let { remoteUid -> remoteUid to existing }
                }
                .toMap()
            shoppingItemDao.insertItems(items.map { remote -> remote.toEntity(existingByRemoteUid[remote.remoteUid], now) })
        }
    }

    override suspend fun applyRemoteDeletes(remoteUids: Set<String>) {
        if (remoteUids.isEmpty()) return
        withContext(ioDispatcher) {
            shoppingItemDao.softDeleteItemsByRemoteUid(remoteUids.toList(), System.currentTimeMillis())
        }
    }

    override suspend fun markItemsSynced(results: List<ShoppingItemSyncResult>) {
        if (results.isEmpty()) return
        withContext(ioDispatcher) {
            shoppingItemDao.markItemsSynced(
                items = results.associateBy { it.id }
            )
        }
    }

    private fun RemoteShoppingItemSnapshot.toEntity(
        existing: ShoppingItemEntity?,
        now: Long
    ): ShoppingItemEntity = existing?.mergeRemote(this, now) ?: ShoppingItemEntity(
        id = UUID.randomUUID().toString(),
        name = summary,
        isPurchased = isCompleted,
        purchaseCount = if (isCompleted) 1 else 0,
        createdAt = lastModifiedAt ?: now,
        updatedAt = lastModifiedAt ?: now,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED,
        remoteUid = remoteUid,
        remoteHref = href,
        remoteEtag = eTag,
        remoteLastModifiedAt = lastModifiedAt,
        lastSyncedAt = now
    )

    private fun ShoppingItemEntity.mergeRemote(
        remote: RemoteShoppingItemSnapshot,
        now: Long
    ): ShoppingItemEntity = copy(
        name = remote.summary,
        normalizedName = ShoppingSearch.normalize(remote.summary),
        isPurchased = remote.isCompleted,
        purchaseCount = updatedPurchaseCount(remote.isCompleted),
        updatedAt = remote.lastModifiedAt ?: now,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED,
        remoteHref = remote.href,
        remoteEtag = remote.eTag,
        remoteLastModifiedAt = remote.lastModifiedAt,
        lastSyncedAt = now
    )

    private fun ShoppingItemEntity.updatedPurchaseCount(isCompletedRemotely: Boolean): Int =
        if (isCompletedRemotely && !isPurchased) purchaseCount + 1 else purchaseCount
}
