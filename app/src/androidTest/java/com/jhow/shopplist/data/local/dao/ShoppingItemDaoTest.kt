package com.jhow.shopplist.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jhow.shopplist.data.local.db.AppDatabase
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingItemDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ShoppingItemDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.shoppingItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observePendingItems_ordersByPurchaseCountThenName() = runBlocking {
        dao.insertItems(
            listOf(
                entity(id = "bread", name = "Bread", purchaseCount = 2),
                entity(id = "apples", name = "Apples", purchaseCount = 4),
                entity(id = "zucchini", name = "Zucchini", purchaseCount = 4),
                entity(id = "coffee", name = "Coffee", isPurchased = true, purchaseCount = 6)
            )
        )

        val items = dao.observePendingItems().first()

        assertEquals(listOf("Apples", "Zucchini", "Bread"), items.map { it.name })
    }

    @Test
    fun observePurchasedItems_ordersByPurchaseCountThenMostRecentUpdate() = runBlocking {
        dao.insertItems(
            listOf(
                entity(id = "tea", name = "Tea", isPurchased = true, purchaseCount = 4, updatedAt = 20),
                entity(id = "rice", name = "Rice", isPurchased = true, purchaseCount = 5, updatedAt = 10),
                entity(id = "beans", name = "Beans", isPurchased = true, purchaseCount = 5, updatedAt = 30)
            )
        )

        val items = dao.observePurchasedItems().first()

        assertEquals(listOf("Beans", "Rice", "Tea"), items.map { it.name })
    }

    @Test
    fun markItemsPurchased_updatesFlagsAndCounters() = runBlocking {
        dao.insertItems(
            listOf(
                entity(id = "milk", name = "Milk", purchaseCount = 1, syncStatus = SyncStatus.SYNCED),
                entity(id = "eggs", name = "Eggs", purchaseCount = 0, syncStatus = SyncStatus.PENDING_INSERT)
            )
        )

        dao.markItemsPurchased(ids = listOf("milk", "eggs"), updatedAt = 55)

        val updatedItems = dao.getAllItems().associateBy { it.id }
        assertTrue(updatedItems.getValue("milk").isPurchased)
        assertEquals(2, updatedItems.getValue("milk").purchaseCount)
        assertEquals(SyncStatus.PENDING_UPDATE, updatedItems.getValue("milk").syncStatus)
        assertTrue(updatedItems.getValue("eggs").isPurchased)
        assertEquals(1, updatedItems.getValue("eggs").purchaseCount)
        assertEquals(SyncStatus.PENDING_INSERT, updatedItems.getValue("eggs").syncStatus)
    }

    @Test
    fun markItemPending_returnsPurchasedItemToPending() = runBlocking {
        dao.insertItem(
            entity(
                id = "coffee",
                name = "Coffee",
                isPurchased = true,
                purchaseCount = 6,
                updatedAt = 10,
                syncStatus = SyncStatus.SYNCED
            )
        )

        dao.markItemPending(id = "coffee", updatedAt = 50)

        val updatedItem = dao.getAllItems().single()
        assertFalse(updatedItem.isPurchased)
        assertEquals(6, updatedItem.purchaseCount)
        assertEquals(50, updatedItem.updatedAt)
        assertEquals(SyncStatus.PENDING_UPDATE, updatedItem.syncStatus)
    }

    @Test
    fun softDeleteItem_hidesItemAndMarksPendingDelete() = runBlocking {
        dao.insertItem(entity(id = "spinach", name = "Spinach", syncStatus = SyncStatus.SYNCED))

        dao.softDeleteItem(id = "spinach", updatedAt = 77)

        assertTrue(dao.observePendingItems().first().isEmpty())
        val deletedItem = dao.getAllItems().single()
        assertTrue(deletedItem.isDeleted)
        assertEquals(77, deletedItem.updatedAt)
        assertEquals(SyncStatus.PENDING_DELETE, deletedItem.syncStatus)
    }

    @Test
    fun getPendingSyncItems_returnsOnlyUnsyncedRows() = runBlocking {
        dao.insertItems(
            listOf(
                entity(id = "milk", name = "Milk", syncStatus = SyncStatus.PENDING_INSERT),
                entity(id = "bread", name = "Bread", syncStatus = SyncStatus.PENDING_UPDATE),
                entity(id = "rice", name = "Rice", syncStatus = SyncStatus.SYNCED)
            )
        )

        val pendingSyncItems = dao.getPendingSyncItems()

        assertEquals(listOf("milk", "bread"), pendingSyncItems.map { it.id })
    }

    @Test
    fun markItemsSynced_updatesUpdatedAtAndSyncStatus() = runBlocking {
        dao.insertItems(
            listOf(
                entity(id = "beans", name = "Beans", syncStatus = SyncStatus.PENDING_INSERT),
                entity(id = "tea", name = "Tea", syncStatus = SyncStatus.PENDING_DELETE)
            )
        )

        dao.markItemsSynced(mapOf("beans" to 101L, "tea" to 102L))

        val items = dao.getAllItems().associateBy { it.id }
        assertEquals(SyncStatus.SYNCED, items.getValue("beans").syncStatus)
        assertEquals(101L, items.getValue("beans").updatedAt)
        assertEquals(SyncStatus.SYNCED, items.getValue("tea").syncStatus)
        assertEquals(102L, items.getValue("tea").updatedAt)
    }

    private fun entity(
        id: String,
        name: String,
        isPurchased: Boolean = false,
        purchaseCount: Int = 0,
        updatedAt: Long = 1L,
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): ShoppingItemEntity = ShoppingItemEntity(
        id = id,
        name = name,
        isPurchased = isPurchased,
        purchaseCount = purchaseCount,
        createdAt = 1L,
        updatedAt = updatedAt,
        isDeleted = false,
        syncStatus = syncStatus
    )
}
