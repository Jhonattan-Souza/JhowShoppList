package com.jhow.shopplist.data.sync

interface CalDavDiscoveryService {
    suspend fun findTaskCollections(
        serverUrl: String,
        username: String,
        password: String
    ): List<CalDavCollectionCandidate>
}
