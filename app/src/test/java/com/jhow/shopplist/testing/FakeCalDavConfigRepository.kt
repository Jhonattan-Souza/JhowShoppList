package com.jhow.shopplist.testing

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import com.jhow.shopplist.domain.sync.CalDavSyncConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCalDavConfigRepository : CalDavConfigRepository {
    private val _config = MutableStateFlow(CalDavSyncConfig())
    private var storedPassword: String? = DEFAULT_STORED_PASSWORD

    val currentConfig: CalDavSyncConfig
        get() = _config.value.copy(hasStoredPassword = storedPassword != null)
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
        createListRequested: Boolean = false,
        hasStoredPassword: Boolean = false
    ) {
        storedPassword = if (hasStoredPassword) DEFAULT_STORED_PASSWORD else null

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
            createListRequested = createListRequested,
            hasStoredPassword = hasStoredPassword
        )
    }

    fun setStoredPasswordAvailable() {
        storedPassword = DEFAULT_STORED_PASSWORD
    }

    fun clearStoredPassword() {
        storedPassword = null
    }

    override fun observeConfig(): Flow<CalDavSyncConfig> = _config.map { config ->
        config.copy(hasStoredPassword = storedPassword != null)
    }

    override suspend fun saveConfig(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        newPassword: String?
    ) = atomicWrite {
        if (newPassword != null) {
            storedPassword = newPassword
        }

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

    override suspend fun clearConfig() = atomicWrite {
        storedPassword = null
        _config.value = CalDavSyncConfig(
            enabled = false,
            serverUrl = "",
            username = "",
            listName = "",
            syncState = CalDavSyncState.Disabled,
            statusMessage = null,
            pendingAction = CalDavPendingAction.None,
            lastSyncAt = null,
            lastResolvedCollectionUrl = null,
            createListRequested = false,
            hasStoredPassword = false
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

    override suspend fun getPassword(): String? = storedPassword

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

    private companion object {
        const val DEFAULT_STORED_PASSWORD = "fake-password"
    }
}
