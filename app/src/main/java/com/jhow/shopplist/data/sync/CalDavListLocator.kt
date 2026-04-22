package com.jhow.shopplist.data.sync

import javax.inject.Inject

class CalDavListLocator @Inject constructor(
    private val service: CalDavDiscoveryService
) {
    suspend fun locate(serverUrl: String, username: String, password: String, listName: String): Result {
        val matches = service.findTaskCollections(serverUrl, username, password)
            .filter { it.displayName == listName }

        return when (matches.size) {
            0 -> Result.Missing
            1 -> Result.Found(matches.single().href)
            else -> Result.Ambiguous
        }
    }

    sealed interface Result {
        data class Found(val href: String) : Result
        data object Missing : Result
        data object Ambiguous : Result
    }
}
