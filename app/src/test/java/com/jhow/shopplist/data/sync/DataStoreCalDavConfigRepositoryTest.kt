package com.jhow.shopplist.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.sync.PasswordStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreCalDavConfigRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `saving config persists enabled settings and sync state`() = runTest {
        val repository = testRepository()

        repository.saveConfig(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = "secret"
        )
        repository.updateSyncState(
            state = CalDavSyncState.UserActionRequired,
            message = "Remote list missing",
            pendingAction = CalDavPendingAction.CreateMissingList
        )

        repository.observeConfig().test {
            val config = awaitItem()
            assertEquals(true, config.enabled)
            assertEquals("https://dav.example.com", config.serverUrl)
            assertEquals("jhow", config.username)
            assertEquals("Groceries", config.listName)
            assertEquals(CalDavSyncState.UserActionRequired, config.syncState)
            assertEquals(CalDavPendingAction.CreateMissingList, config.pendingAction)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saving blank password preserves previous password`() = runTest {
        val repository = testRepository()

        repository.saveConfig(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = "secret"
        )
        repository.saveConfig(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals("secret", repository.getPassword())
    }

    @Test
    fun `unknown persisted enum values fall back to defaults`() = runTest {
        val (repository, dataStore) = testRepositoryWithDataStore()

        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[booleanPreferencesKey("enabled")] = true
                this[stringPreferencesKey("server_url")] = "https://dav.example.com"
                this[stringPreferencesKey("username")] = "jhow"
                this[stringPreferencesKey("list_name")] = "Groceries"
                this[stringPreferencesKey("sync_state")] = "UnknownState"
                this[stringPreferencesKey("pending_action")] = "UnknownAction"
            }
        }

        repository.observeConfig().test {
            val config = awaitItem()
            assertEquals(CalDavSyncState.Disabled, config.syncState)
            assertEquals(CalDavPendingAction.None, config.pendingAction)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updating sync state with null lastSyncAt removes it`() = runTest {
        val repository = testRepository()

        repository.saveConfig(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = "secret"
        )
        repository.updateSyncState(
            state = CalDavSyncState.Success,
            message = null,
            pendingAction = CalDavPendingAction.None,
            lastSyncAt = 1234L
        )

        repository.observeConfig().test {
            val configWithTimestamp = awaitItem()
            assertEquals(1234L, configWithTimestamp.lastSyncAt)

            repository.updateSyncState(
                state = CalDavSyncState.Idle,
                message = null,
                pendingAction = CalDavPendingAction.None,
                lastSyncAt = null
            )

            val configWithoutTimestamp = awaitItem()
            assertNull(configWithoutTimestamp.lastSyncAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `password is null before first save`() = runTest {
        val repository = testRepository()

        assertNull(repository.getPassword())
    }

    @Test
    fun `saveConfig with enabled true resets sync state to Idle and clears status and pending action`() = runTest {
        val repository = testRepository()

        repository.saveConfig(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = "secret"
        )
        repository.updateSyncState(
            state = CalDavSyncState.UserActionRequired,
            message = "Remote list missing",
            pendingAction = CalDavPendingAction.CreateMissingList
        )

        repository.saveConfig(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = "secret"
        )

        repository.observeConfig().test {
            val config = awaitItem()
            assertEquals(CalDavSyncState.Idle, config.syncState)
            assertNull(config.statusMessage)
            assertEquals(CalDavPendingAction.None, config.pendingAction)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun testRepository(): DataStoreCalDavConfigRepository = testRepositoryWithDataStore().first

    private fun testRepositoryWithDataStore(): Pair<DataStoreCalDavConfigRepository, DataStore<Preferences>> {
        val tempDir = tempFolder.newFolder("caldav-test")
        val dataStore = PreferenceDataStoreFactory.create { File(tempDir, "test_prefs.preferences_pb") }
        return DataStoreCalDavConfigRepository(dataStore, FakePasswordStorage()) to dataStore
    }

    private class FakePasswordStorage : PasswordStorage {
        private var storedPassword: String? = null

        override suspend fun save(password: String) {
            storedPassword = password
        }

        override suspend fun load(): String? = storedPassword

        override suspend fun clear() {
            storedPassword = null
        }
    }
}
