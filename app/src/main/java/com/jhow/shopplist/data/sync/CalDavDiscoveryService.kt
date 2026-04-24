package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot

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
}
