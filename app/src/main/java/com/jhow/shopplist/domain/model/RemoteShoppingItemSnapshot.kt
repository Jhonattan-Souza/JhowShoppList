package com.jhow.shopplist.domain.model

data class RemoteShoppingItemSnapshot(
    val remoteUid: String,
    val summary: String,
    val isCompleted: Boolean,
    val href: String,
    val eTag: String?,
    val lastModifiedAt: Long?
)
