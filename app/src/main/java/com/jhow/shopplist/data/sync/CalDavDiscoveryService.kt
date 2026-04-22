package com.jhow.shopplist.data.sync

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
}
