package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpCalDavDiscoveryService @Inject constructor() : CalDavDiscoveryService {
    override suspend fun findTaskCollections(
        serverUrl: String,
        username: String,
        password: String
    ): List<CalDavCollectionCandidate> = emptyList()

    override suspend fun createTaskCollection(
        serverUrl: String,
        username: String,
        password: String,
        listName: String
    ): String = "$serverUrl/$listName/"

    override suspend fun fetchTaskItems(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String
    ): List<RemoteShoppingItemSnapshot> = emptyList()

    override suspend fun upsertTaskItem(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String,
        item: ShoppingItem
    ): CalDavTaskUpsertResult = CalDavTaskUpsertResult(
        remoteUid = item.remoteMetadata.remoteUid ?: item.id,
        href = item.remoteMetadata.remoteHref ?: "$collectionHref${item.id}.ics",
        eTag = null,
        lastModifiedAt = item.updatedAt
    )

    override suspend fun deleteTaskItem(
        serverUrl: String,
        username: String,
        password: String,
        href: String,
        eTag: String?
    ): CalDavTaskDeleteResult = CalDavTaskDeleteResult(deletedAt = null)
}
