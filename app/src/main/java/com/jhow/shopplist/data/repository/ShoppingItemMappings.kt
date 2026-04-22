package com.jhow.shopplist.data.repository

import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemRemoteMetadata

fun ShoppingItemEntity.toDomain(): ShoppingItem = ShoppingItem(
    id = id,
    name = name,
    isPurchased = isPurchased,
    purchaseCount = purchaseCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    syncStatus = syncStatus,
    remoteMetadata = ShoppingItemRemoteMetadata(
        remoteUid = remoteUid,
        remoteHref = remoteHref,
        remoteEtag = remoteEtag,
        remoteLastModifiedAt = remoteLastModifiedAt,
        lastSyncedAt = lastSyncedAt
    )
)
