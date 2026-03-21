package com.jhow.shopplist.data.repository

import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.domain.model.ShoppingItem

fun ShoppingItemEntity.toDomain(): ShoppingItem = ShoppingItem(
    id = id,
    name = name,
    isPurchased = isPurchased,
    purchaseCount = purchaseCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = syncStatus
)
