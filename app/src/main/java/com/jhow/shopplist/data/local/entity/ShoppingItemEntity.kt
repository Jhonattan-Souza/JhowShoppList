package com.jhow.shopplist.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jhow.shopplist.domain.model.SyncStatus

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["isPurchased", "isDeleted", "purchaseCount"]),
        Index(value = ["isPurchased", "isDeleted", "purchaseCount", "updatedAt"])
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isPurchased: Boolean,
    val purchaseCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val syncStatus: SyncStatus
)
