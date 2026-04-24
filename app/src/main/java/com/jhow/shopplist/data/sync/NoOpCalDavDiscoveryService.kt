package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
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
}
