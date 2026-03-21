package com.jhow.shopplist.domain.model

data class ShoppingItem(
    val id: String,
    val name: String,
    val isPurchased: Boolean,
    val purchaseCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val syncStatus: SyncStatus
)
