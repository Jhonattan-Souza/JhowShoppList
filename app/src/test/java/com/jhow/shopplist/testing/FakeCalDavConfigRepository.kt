package com.jhow.shopplist.testing

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import com.jhow.shopplist.domain.sync.CalDavSyncConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCalDavConfigRepository : CalDavConfigRepository {
    private val _config = MutableStateFlow(CalDavSyncConfig())

    val currentConfig: CalDavSyncConfig get() = _config.value

    fun seed(
        enabled: Boolean = false,
        serverUrl: String = "",
        username: String = "",
        listName: String = "",
        syncState: CalDavSyncState = CalDavSyncState.Disabled,
        statusMessage: String? = null,
        pendingAction: CalDavPendingAction = CalDavPendingAction.None,
        lastSyncAt: Long? = null
    ) {
        _config.value = CalDavSyncConfig(
            enabled = enabled,
            serverUrl = serverUrl,
            username = username,
            listName = listName,
            syncState = syncState,
            statusMessage = statusMessage,
            pendingAction = pendingAction,
            lastSyncAt = lastSyncAt
        )
    }

    override fun observeConfig(): Flow<CalDavSyncConfig> = _config

    override suspend fun saveConfig(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    ) {
        _config.value = _config.value.copy(
            enabled = enabled,
            serverUrl = serverUrl,
            username = username,
            listName = listName,
            syncState = if (enabled) CalDavSyncState.Idle else CalDavSyncState.Disabled,
            pendingAction = CalDavPendingAction.None,
            statusMessage = null
        )
    }

    override suspend fun updateSyncState(
        state: CalDavSyncState,
        message: String?,
        pendingAction: CalDavPendingAction,
        lastSyncAt: Long?
    ) {
        _config.value = _config.value.copy(
            syncState = state,
            statusMessage = message,
            pendingAction = pendingAction,
            lastSyncAt = lastSyncAt
        )
    }

    override suspend fun getPassword(): String? = "fake-password"
}
