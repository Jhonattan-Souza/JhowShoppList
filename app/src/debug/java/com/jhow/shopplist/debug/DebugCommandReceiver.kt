package com.jhow.shopplist.debug

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jhow.shopplist.data.local.db.AppDatabase
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.domain.model.SyncStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DebugCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val database = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DebugDatabaseEntryPoint::class.java
            ).database()

            val response = when (intent.action) {
                ACTION_RESET_DB -> {
                    database.shoppingItemDao().deleteAll()
                    "Database reset"
                }

                ACTION_SEED_SAMPLE -> {
                    database.shoppingItemDao().replaceAll(sampleItems())
                    "Sample items seeded"
                }

                ACTION_DUMP_STATE -> buildDump(database)
                else -> "Unknown action: ${intent.action}"
            }

            Log.i(LOG_TAG, response)
            pendingResult.resultCode = Activity.RESULT_OK
            pendingResult.resultData = response
            pendingResult.finish()
        }
    }

    private suspend fun buildDump(database: AppDatabase): String {
        val items = database.shoppingItemDao().getAllItems()
        if (items.isEmpty()) return "[]"

        return items.joinToString(prefix = "[", postfix = "]") { item ->
            "{id=${item.id},name=${item.name},isPurchased=${item.isPurchased},purchaseCount=${item.purchaseCount},syncStatus=${item.syncStatus}}"
        }
    }

    private fun sampleItems(): List<ShoppingItemEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ShoppingItemEntity(
                id = "pending-apples",
                name = "Apples",
                isPurchased = false,
                purchaseCount = 4,
                createdAt = now - 4_000,
                updatedAt = now - 4_000,
                isDeleted = false,
                syncStatus = SyncStatus.SYNCED
            ),
            ShoppingItemEntity(
                id = "pending-bread",
                name = "Bread",
                isPurchased = false,
                purchaseCount = 2,
                createdAt = now - 3_000,
                updatedAt = now - 3_000,
                isDeleted = false,
                syncStatus = SyncStatus.SYNCED
            ),
            ShoppingItemEntity(
                id = "purchased-coffee",
                name = "Coffee",
                isPurchased = true,
                purchaseCount = 7,
                createdAt = now - 8_000,
                updatedAt = now - 1_000,
                isDeleted = false,
                syncStatus = SyncStatus.SYNCED
            )
        )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DebugDatabaseEntryPoint {
        fun database(): AppDatabase
    }

    companion object {
        const val ACTION_RESET_DB: String = "com.jhow.shopplist.debug.RESET_DB"
        const val ACTION_SEED_SAMPLE: String = "com.jhow.shopplist.debug.SEED_SAMPLE"
        const val ACTION_DUMP_STATE: String = "com.jhow.shopplist.debug.DUMP_STATE"
        private const val LOG_TAG: String = "DebugCommandReceiver"
    }
}
