package com.jhow.shopplist.data.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpCalDavDiscoveryService @Inject constructor() : CalDavDiscoveryService {
    override suspend fun findTaskCollections(
        serverUrl: String,
        username: String,
        password: String
    ): List<CalDavCollectionCandidate> = emptyList()
}
