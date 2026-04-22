package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import javax.inject.Inject

class SaveCalDavSyncConfigUseCase @Inject constructor(
    private val repository: CalDavConfigRepository
) {
    suspend operator fun invoke(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        password: String,
        resolvedCollectionUrl: String? = null
    ) {
        repository.saveConfig(enabled, serverUrl, username, listName, password)
        repository.setResolvedCollectionUrl(resolvedCollectionUrl)
    }
}
