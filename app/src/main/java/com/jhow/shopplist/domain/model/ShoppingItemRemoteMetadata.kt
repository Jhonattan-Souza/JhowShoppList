package com.jhow.shopplist.domain.model

data class ShoppingItemRemoteMetadata(
    val remoteUid: String? = null,
    val remoteHref: String? = null,
    val remoteEtag: String? = null,
    val remoteLastModifiedAt: Long? = null,
    val lastSyncedAt: Long? = null
)
