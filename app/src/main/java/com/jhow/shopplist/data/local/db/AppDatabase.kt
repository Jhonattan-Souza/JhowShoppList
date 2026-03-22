package com.jhow.shopplist.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jhow.shopplist.data.local.dao.ShoppingItemDao
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.core.search.ShoppingSearch

@Database(
    entities = [ShoppingItemEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        const val DATABASE_NAME: String = "shopping-list.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE items
                    ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )

                db.query("SELECT id, name FROM items").use { cursor ->
                    val idColumnIndex = cursor.getColumnIndexOrThrow("id")
                    val nameColumnIndex = cursor.getColumnIndexOrThrow("name")

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idColumnIndex)
                        val name = cursor.getString(nameColumnIndex)
                        db.execSQL(
                            "UPDATE items SET normalizedName = ? WHERE id = ?",
                            arrayOf(ShoppingSearch.normalize(name), id)
                        )
                    }
                }

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_items_normalizedName_isDeleted
                    ON items(normalizedName, isDeleted)
                    """.trimIndent()
                )
            }
        }
    }
}
