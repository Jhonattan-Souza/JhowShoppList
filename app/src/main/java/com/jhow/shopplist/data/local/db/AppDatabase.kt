package com.jhow.shopplist.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jhow.shopplist.data.local.dao.ShoppingItemDao
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity

@Database(
    entities = [ShoppingItemEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        const val DATABASE_NAME: String = "shopping-list.db"
    }
}
