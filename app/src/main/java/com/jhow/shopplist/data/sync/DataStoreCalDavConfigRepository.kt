package com.jhow.shopplist.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import com.jhow.shopplist.domain.sync.CalDavSyncConfig
import com.jhow.shopplist.domain.sync.PasswordStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreCalDavConfigRepository(
    private val dataStore: DataStore<Preferences>,
    private val passwordStorage: PasswordStorage
) : CalDavConfigRepository {

    @Inject
    constructor(
        preferences: CalDavSyncPreferences,
        passwordStorage: PasswordStorage
    ) : this(preferences.dataStore, passwordStorage)

    override fun observeConfig(): Flow<CalDavSyncConfig> = dataStore.data.map { prefs ->
        CalDavSyncConfig(
            enabled = prefs[ENABLED] ?: false,
            serverUrl = prefs[SERVER_URL].orEmpty(),
            username = prefs[USERNAME].orEmpty(),
            listName = prefs[LIST_NAME].orEmpty(),
            syncState = prefs[SYNC_STATE]?.let {
                runCatching { CalDavSyncState.valueOf(it) }.getOrDefault(CalDavSyncState.Disabled)
            } ?: CalDavSyncState.Disabled,
            statusMessage = prefs[STATUS_MESSAGE],
            pendingAction = prefs[PENDING_ACTION]?.let {
                runCatching { CalDavPendingAction.valueOf(it) }.getOrDefault(CalDavPendingAction.None)
            } ?: CalDavPendingAction.None,
            lastSyncAt = prefs[LAST_SYNC_AT],
            lastResolvedCollectionUrl = prefs[LAST_RESOLVED_COLLECTION_URL],
            createListRequested = prefs[CREATE_LIST_REQUESTED] ?: false,
            hasStoredPassword = passwordStorage.hasSavedPassword()
        )
    }

    override suspend fun saveConfig(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    ) {
        if (password.isNotBlank()) {
            passwordStorage.save(password)
        }
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[ENABLED] = enabled
                this[SERVER_URL] = serverUrl
                this[USERNAME] = username
                this[LIST_NAME] = listName
                this[SYNC_STATE] = if (enabled) CalDavSyncState.Idle.name else CalDavSyncState.Disabled.name
                this[PENDING_ACTION] = CalDavPendingAction.None.name
                this.remove(STATUS_MESSAGE)
                this.remove(LAST_RESOLVED_COLLECTION_URL)
                this[CREATE_LIST_REQUESTED] = false
            }
        }
    }

    override suspend fun updateSyncState(
        state: CalDavSyncState,
        message: String?,
        pendingAction: CalDavPendingAction,
        lastSyncAt: Long?
    ) {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[SYNC_STATE] = state.name
                this[PENDING_ACTION] = pendingAction.name
                if (message == null) remove(STATUS_MESSAGE) else this[STATUS_MESSAGE] = message
                if (lastSyncAt == null) remove(LAST_SYNC_AT) else this[LAST_SYNC_AT] = lastSyncAt
            }
        }
    }

    override suspend fun getPassword(): String? = passwordStorage.load()

    override suspend fun setCreateListRequested(requested: Boolean) {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[CREATE_LIST_REQUESTED] = requested
            }
        }
    }

    override suspend fun setResolvedCollectionUrl(url: String?) {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                if (url == null) remove(LAST_RESOLVED_COLLECTION_URL) else this[LAST_RESOLVED_COLLECTION_URL] = url
            }
        }
    }

    override suspend fun confirmCreateList() {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[CREATE_LIST_REQUESTED] = true
                this[SYNC_STATE] = CalDavSyncState.Idle.name
                this[PENDING_ACTION] = CalDavPendingAction.None.name
                remove(STATUS_MESSAGE)
            }
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val LIST_NAME = stringPreferencesKey("list_name")
        val SYNC_STATE = stringPreferencesKey("sync_state")
        val STATUS_MESSAGE = stringPreferencesKey("status_message")
        val PENDING_ACTION = stringPreferencesKey("pending_action")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_RESOLVED_COLLECTION_URL = stringPreferencesKey("last_resolved_collection_url")
        val CREATE_LIST_REQUESTED = booleanPreferencesKey("create_list_requested")
    }
}
