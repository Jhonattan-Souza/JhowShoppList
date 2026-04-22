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
    var atomicWriteCount = 0
        private set

    private fun atomicWrite(block: () -> Unit) {
        block()
        atomicWriteCount++
    }

    fun seed(
        enabled: Boolean = false,
        serverUrl: String = "",
        username: String = "",
        listName: String = "",
        syncState: CalDavSyncState = CalDavSyncState.Disabled,
        statusMessage: String? = null,
        pendingAction: CalDavPendingAction = CalDavPendingAction.None,
        lastSyncAt: Long? = null,
        lastResolvedCollectionUrl: String? = null,
        createListRequested: Boolean = false
    ) {
        _config.value = CalDavSyncConfig(
            enabled = enabled,
            serverUrl = serverUrl,
            username = username,
            listName = listName,
            syncState = syncState,
            statusMessage = statusMessage,
            pendingAction = pendingAction,
            lastSyncAt = lastSyncAt,
            lastResolvedCollectionUrl = lastResolvedCollectionUrl,
            createListRequested = createListRequested
        )
    }

    override fun observeConfig(): Flow<CalDavSyncConfig> = _config

    override suspend fun saveConfig(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    ) = atomicWrite {
        _config.value = _config.value.copy(
            enabled = enabled,
            serverUrl = serverUrl,
            username = username,
            listName = listName,
            syncState = if (enabled) CalDavSyncState.Idle else CalDavSyncState.Disabled,
            pendingAction = CalDavPendingAction.None,
            statusMessage = null,
            lastResolvedCollectionUrl = null,
            createListRequested = false
        )
    }

    override suspend fun updateSyncState(
        state: CalDavSyncState,
        message: String?,
        pendingAction: CalDavPendingAction,
        lastSyncAt: Long?
    ) = atomicWrite {
        _config.value = _config.value.copy(
            syncState = state,
            statusMessage = message,
            pendingAction = pendingAction,
            lastSyncAt = lastSyncAt
        )
    }

    override suspend fun getPassword(): String? = "fake-password"

    override suspend fun setCreateListRequested(requested: Boolean) = atomicWrite {
        _config.value = _config.value.copy(createListRequested = requested)
    }

    override suspend fun setResolvedCollectionUrl(url: String?) = atomicWrite {
        _config.value = _config.value.copy(lastResolvedCollectionUrl = url)
    }

    override suspend fun confirmCreateList() = atomicWrite {
        _config.value = _config.value.copy(
            createListRequested = true,
            syncState = CalDavSyncState.Idle,
            pendingAction = CalDavPendingAction.None,
            statusMessage = null
        )
    }
}
