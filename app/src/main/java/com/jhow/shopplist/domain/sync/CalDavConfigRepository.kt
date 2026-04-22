package com.jhow.shopplist.domain.sync

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import kotlinx.coroutines.flow.Flow

data class CalDavSyncConfig(
    val enabled: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val listName: String = "",
    val syncState: CalDavSyncState = CalDavSyncState.Disabled,
    val statusMessage: String? = null,
    val pendingAction: CalDavPendingAction = CalDavPendingAction.None,
    val lastSyncAt: Long? = null,
    val lastResolvedCollectionUrl: String? = null,
    val createListRequested: Boolean = false
) {
    val isReadyToSync: Boolean
        get() = enabled && serverUrl.isNotBlank() && username.isNotBlank() && listName.isNotBlank()
}

interface CalDavConfigRepository {
    fun observeConfig(): Flow<CalDavSyncConfig>

    suspend fun saveConfig(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    )

    suspend fun updateSyncState(
        state: CalDavSyncState,
        message: String?,
        pendingAction: CalDavPendingAction = CalDavPendingAction.None,
        lastSyncAt: Long? = null
    )

    suspend fun getPassword(): String?

    suspend fun setCreateListRequested(requested: Boolean)

    suspend fun setResolvedCollectionUrl(url: String?)

    suspend fun confirmCreateList()
}
