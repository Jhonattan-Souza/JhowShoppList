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
        repository.saveConfig(
            enabled = enabled,
            serverUrl = serverUrl,
            username = username,
            listName = listName,
            newPassword = password.ifBlank { null }
        )
        repository.setResolvedCollectionUrl(resolvedCollectionUrl)
    }
}
