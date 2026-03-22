package com.jhow.shopplist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query(
        """
        SELECT * FROM items
        WHERE isPurchased = 0 AND isDeleted = 0
        ORDER BY purchaseCount DESC, name ASC
        """
    )
    fun observePendingItems(): Flow<List<ShoppingItemEntity>>

    @Query(
        """
        SELECT * FROM items
        WHERE isPurchased = 1 AND isDeleted = 0
        ORDER BY purchaseCount DESC, updatedAt DESC
        """
    )
    fun observePurchasedItems(): Flow<List<ShoppingItemEntity>>

    @Query(
        """
        SELECT * FROM items
        ORDER BY isPurchased ASC, purchaseCount DESC, updatedAt DESC, name ASC
        """
    )
    suspend fun getAllItems(): List<ShoppingItemEntity>

    @Query(
        """
        SELECT * FROM items
        WHERE syncStatus != 'SYNCED'
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getPendingSyncItems(): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingItemEntity>)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE items
        SET isPurchased = 1,
            purchaseCount = purchaseCount + 1,
            updatedAt = :updatedAt,
            syncStatus = CASE
                WHEN syncStatus = 'PENDING_INSERT' THEN 'PENDING_INSERT'
                ELSE 'PENDING_UPDATE'
            END
        WHERE id IN (:ids) AND isPurchased = 0 AND isDeleted = 0
        """
    )
    suspend fun markItemsPurchased(ids: List<String>, updatedAt: Long): Int

    @Query(
        """
        UPDATE items
        SET isPurchased = 0,
            updatedAt = :updatedAt,
            syncStatus = CASE
                WHEN syncStatus = 'PENDING_INSERT' THEN 'PENDING_INSERT'
                ELSE 'PENDING_UPDATE'
            END
        WHERE id = :id AND isPurchased = 1 AND isDeleted = 0
        """
    )
    suspend fun markItemPending(id: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE items
        SET isDeleted = 1,
            updatedAt = :updatedAt,
            syncStatus = 'PENDING_DELETE'
        WHERE id = :id AND isDeleted = 0
        """
    )
    suspend fun softDeleteItem(id: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE items
        SET updatedAt = :updatedAt,
            syncStatus = 'SYNCED'
        WHERE id = :id
        """
    )
    suspend fun markItemSynced(id: String, updatedAt: Long): Int

    @Transaction
    suspend fun replaceAll(items: List<ShoppingItemEntity>) {
        deleteAll()
        insertItems(items)
    }

    @Transaction
    suspend fun markItemsSynced(items: Map<String, Long>) {
        items.forEach { (id, updatedAt) ->
            markItemSynced(id = id, updatedAt = updatedAt)
        }
    }
}
