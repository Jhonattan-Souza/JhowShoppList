package com.jhow.shopplist.presentation.caldavconfig

import app.cash.turbine.test
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.usecase.ClearCalDavPendingActionUseCase
import com.jhow.shopplist.domain.usecase.ClearCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.ConfirmCreateCalDavListUseCase
import com.jhow.shopplist.domain.usecase.GetCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.RequestShoppingSyncUseCase
import com.jhow.shopplist.domain.usecase.SaveCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.ValidateCalDavSyncSettingsUseCase
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import com.jhow.shopplist.testing.FakeShoppingSyncScheduler
import com.jhow.shopplist.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalDavConfigViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var syncScheduler: FakeShoppingSyncScheduler
    private lateinit var configRepository: FakeCalDavConfigRepository
    private lateinit var validationUseCase: FakeValidateCalDavSyncSettingsUseCase
    private lateinit var createListUseCase: FakeConfirmCreateCalDavListUseCase

    @Before
    fun setUp() {
        syncScheduler = FakeShoppingSyncScheduler()
        configRepository = FakeCalDavConfigRepository()
        validationUseCase = FakeValidateCalDavSyncSettingsUseCase()
        createListUseCase = FakeConfirmCreateCalDavListUseCase()
    }

    private fun createViewModel(): CalDavConfigViewModel {
        return CalDavConfigViewModel(
            getCalDavSyncConfigUseCase = GetCalDavSyncConfigUseCase(configRepository),
            validateCalDavSyncSettingsUseCase = validationUseCase,
            saveCalDavSyncConfigUseCase = SaveCalDavSyncConfigUseCase(configRepository),
            confirmCreateCalDavListUseCase = createListUseCase,
            clearCalDavPendingActionUseCase = ClearCalDavPendingActionUseCase(configRepository),
            clearCalDavSyncConfigUseCase = ClearCalDavSyncConfigUseCase(configRepository),
            requestShoppingSyncUseCase = RequestShoppingSyncUseCase(syncScheduler)
        )
    }

    @Test
    fun `form starts in loading state`() = runTest {
        val viewModel = createViewModel()
        val initialState = viewModel.uiState.value
        assertEquals(true, initialState.isLoading)
    }

    @Test
    fun `form is hydrated from saved config on init`() = runTest {
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle,
            hasStoredPassword = true
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("https://dav.example.com", state.serverUrl)
        assertEquals("jhow", state.username)
        assertEquals("Groceries", state.listName)
        assertEquals(true, state.hasStoredPassword)
        assertEquals(true, state.isSaveSuccessful)
        assertEquals("https://dav.example.com", state.savedServerUrl)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `editing fields updates form without saving to repository immediately`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onServerUrlChanged("https://changed.example.com")
        viewModel.onUsernameChanged("alice")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("https://changed.example.com", state.serverUrl)
        assertEquals("alice", state.username)
        assertEquals("", configRepository.currentConfig.serverUrl)
    }

    @Test
    fun `save success updates state and persists`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        validationUseCase.nextResult = CalDavValidationResult.Success("/lists/groceries/")
        viewModel.onServerUrlChanged("https://dav.example.com")
        viewModel.onUsernameChanged("jhow")
        viewModel.onPasswordChanged("secret")
        viewModel.onListNameChanged("Groceries")
        advanceUntilIdle()

        viewModel.onSaveClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.isSaveSuccessful)
        assertEquals("https://dav.example.com", state.savedServerUrl)
        assertEquals("", state.password)
        assertEquals(true, state.hasStoredPassword)
        
        assertEquals("Groceries", configRepository.currentConfig.listName)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `save shows error on validation failure`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        validationUseCase.nextResult = CalDavValidationResult.ConfigurationError("Password is required")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isSaveSuccessful)
        assertEquals("Password is required", state.statusMessage)
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `missing list failure shows missing list pending action`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        validationUseCase.nextResult = CalDavValidationResult.MissingList("Groceries")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isSaveSuccessful)
        assertEquals(CalDavPendingAction.CreateMissingList, state.pendingAction)
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `confirm create missing list succeeds and saves`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        validationUseCase.nextResult = CalDavValidationResult.MissingList("Groceries")
        createListUseCase.nextResult = CalDavValidationResult.Success("/lists/groceries/")
        
        viewModel.onServerUrlChanged("https://dav.example.com")
        viewModel.onUsernameChanged("jhow")
        viewModel.onPasswordChanged("secret")
        viewModel.onListNameChanged("Groceries")
        advanceUntilIdle()

        viewModel.onSaveClicked()
        advanceUntilIdle()
        
        viewModel.onConfirmCreateMissingList()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.isSaveSuccessful)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `reset clears all fields and DataStore`() = runTest {
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            hasStoredPassword = true
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onServerUrlChanged("https://changed.example.com")
        
        viewModel.onResetForm()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.serverUrl)
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertEquals("", state.listName)
        assertEquals(false, state.hasStoredPassword)
        assertEquals(false, state.isSaveSuccessful)
        assertEquals(false, state.isLoading)
    }

    private class FakeValidateCalDavSyncSettingsUseCase : ValidateCalDavSyncSettingsUseCase(
        configRepository = FakeCalDavConfigRepository(),
        listLocator = com.jhow.shopplist.data.sync.CalDavListLocator(
            object : com.jhow.shopplist.data.sync.CalDavDiscoveryService {
                override suspend fun findTaskCollections(
                    serverUrl: String,
                    username: String,
                    password: String
                ) = emptyList<com.jhow.shopplist.data.sync.CalDavCollectionCandidate>()

                override suspend fun createTaskCollection(
                    serverUrl: String,
                    username: String,
                    password: String,
                    listName: String
                ): String = error("Not used")

                override suspend fun fetchTaskItems(
                    serverUrl: String,
                    username: String,
                    password: String,
                    collectionHref: String
                ) = emptyList<com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot>()
            }
        ),
        ioDispatcher = UnconfinedTestDispatcher()
    ) {
        var nextResult: CalDavValidationResult = CalDavValidationResult.Success()

        override suspend operator fun invoke(
            enabled: Boolean,
            serverUrl: String,
            username: String,
            listName: String,
            password: String
        ): CalDavValidationResult {
            return nextResult
        }
    }

    private class FakeConfirmCreateCalDavListUseCase : ConfirmCreateCalDavListUseCase(
        repository = FakeCalDavConfigRepository(),
        discoveryService = object : com.jhow.shopplist.data.sync.CalDavDiscoveryService {
            override suspend fun findTaskCollections(
                serverUrl: String,
                username: String,
                password: String
            ) = emptyList<com.jhow.shopplist.data.sync.CalDavCollectionCandidate>()

            override suspend fun createTaskCollection(
                serverUrl: String,
                username: String,
                password: String,
                listName: String
            ): String = error("Not used")

            override suspend fun fetchTaskItems(
                serverUrl: String,
                username: String,
                password: String,
                collectionHref: String
            ) = emptyList<com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot>()
        },
        ioDispatcher = UnconfinedTestDispatcher()
    ) {
        var nextResult: CalDavValidationResult = CalDavValidationResult.Success()

        override suspend operator fun invoke(
            serverUrl: String,
            username: String,
            listName: String,
            password: String
        ): CalDavValidationResult {
            return nextResult
        }
    }
}
