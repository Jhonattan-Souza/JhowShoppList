package com.jhow.shopplist.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jhow.shopplist.core.search.ShoppingSearch
import com.jhow.shopplist.domain.model.SyncStatus

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["isPurchased", "isDeleted", "purchaseCount"]),
        Index(value = ["isPurchased", "isDeleted", "purchaseCount", "updatedAt"]),
        Index(value = ["normalizedName", "isDeleted"])
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String = ShoppingSearch.normalize(name),
    val isPurchased: Boolean,
    val purchaseCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val syncStatus: SyncStatus,
    val remoteUid: String? = null,
    val remoteHref: String? = null,
    val remoteEtag: String? = null,
    val remoteLastModifiedAt: Long? = null,
    val lastSyncedAt: Long? = null
)
