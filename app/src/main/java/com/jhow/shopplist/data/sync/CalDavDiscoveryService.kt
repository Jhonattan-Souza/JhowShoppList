package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem

data class CalDavTaskUpsertResult(
    val remoteUid: String,
    val href: String,
    val eTag: String?,
    val lastModifiedAt: Long?
)

data class CalDavTaskDeleteResult(
    val deletedAt: Long?
)

interface CalDavDiscoveryService {
    suspend fun findTaskCollections(
        serverUrl: String,
        username: String,
        password: String
    ): List<CalDavCollectionCandidate>

    suspend fun createTaskCollection(
        serverUrl: String,
        username: String,
        password: String,
        listName: String
    ): String

    suspend fun fetchTaskItems(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String
    ): List<RemoteShoppingItemSnapshot>

    suspend fun upsertTaskItem(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String,
        item: ShoppingItem
    ): CalDavTaskUpsertResult

    suspend fun deleteTaskItem(
        serverUrl: String,
        username: String,
        password: String,
        href: String,
        eTag: String?
    ): CalDavTaskDeleteResult
}
