package com.jhow.shopplist.presentation.shoppinglist

import app.cash.turbine.test
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.domain.usecase.AddOrReclaimShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.ClearCalDavPendingActionUseCase
import com.jhow.shopplist.domain.usecase.ConfirmCreateCalDavListUseCase
import com.jhow.shopplist.domain.usecase.DeleteShoppingItemUseCase
import com.jhow.shopplist.domain.usecase.GetCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.MarkPurchasedItemPendingUseCase
import com.jhow.shopplist.domain.usecase.MarkSelectedItemsPurchasedUseCase
import com.jhow.shopplist.domain.usecase.ObserveItemNamesUseCase
import com.jhow.shopplist.domain.usecase.ObservePendingItemsUseCase
import com.jhow.shopplist.domain.usecase.ObservePurchasedItemsUseCase
import com.jhow.shopplist.domain.usecase.RequestShoppingSyncUseCase
import com.jhow.shopplist.domain.usecase.SaveCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.ValidateCalDavSyncSettingsUseCase
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import com.jhow.shopplist.testing.FakeShoppingListRepository
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
class ShoppingListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeShoppingListRepository
    private lateinit var syncScheduler: FakeShoppingSyncScheduler
    private lateinit var configRepository: FakeCalDavConfigRepository
    private lateinit var validationUseCase: FakeValidateCalDavSyncSettingsUseCase
    private lateinit var createListUseCase: FakeConfirmCreateCalDavListUseCase
    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        repository = FakeShoppingListRepository()
        syncScheduler = FakeShoppingSyncScheduler()
        configRepository = FakeCalDavConfigRepository()
        validationUseCase = FakeValidateCalDavSyncSettingsUseCase()
        createListUseCase = FakeConfirmCreateCalDavListUseCase()
        viewModel = ShoppingListViewModel(
            observePendingItemsUseCase = ObservePendingItemsUseCase(repository),
            observePurchasedItemsUseCase = ObservePurchasedItemsUseCase(repository),
            observeItemNamesUseCase = ObserveItemNamesUseCase(repository),
            addOrReclaimShoppingItemUseCase = AddOrReclaimShoppingItemUseCase(repository),
            deleteShoppingItemUseCase = DeleteShoppingItemUseCase(repository),
            markSelectedItemsPurchasedUseCase = MarkSelectedItemsPurchasedUseCase(repository),
            markPurchasedItemPendingUseCase = MarkPurchasedItemPendingUseCase(repository),
            requestShoppingSyncUseCase = RequestShoppingSyncUseCase(syncScheduler),
            getCalDavSyncConfigUseCase = GetCalDavSyncConfigUseCase(configRepository),
            validateCalDavSyncSettingsUseCase = validationUseCase,
            saveCalDavSyncConfigUseCase = SaveCalDavSyncConfigUseCase(configRepository),
            confirmCreateCalDavListUseCase = createListUseCase,
            clearCalDavPendingActionUseCase = ClearCalDavPendingActionUseCase(configRepository)
        )
    }

    @Test
    fun `resolveSyncSettingsSubmission success clears password and preserves stored password state`() {
        val current = ShoppingListSyncSettingsUiState(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            listName = "Groceries",
            hasStoredPassword = false,
            isSaving = true,
            pendingAction = CalDavPendingAction.None
        )

        val outcome = resolveSyncSettingsSubmission(
            current = current,
            result = CalDavValidationResult.Success("/lists/groceries/")
        )

        assertEquals(
            SyncSettingsSubmissionOutcome.Success(
                resolvedCollectionUrl = "/lists/groceries/",
                updatedForm = current.copy(
                    password = "",
                    hasStoredPassword = true,
                    isSaving = false,
                    statusMessage = null,
                    pendingAction = CalDavPendingAction.None
                )
            ),
            outcome
        )
    }

    @Test
    fun `resolveSyncSettingsSubmission missing list keeps sheet state and exposes pending action`() {
        val current = ShoppingListSyncSettingsUiState(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            listName = "Groceries",
            hasStoredPassword = false,
            isSaving = true,
            pendingAction = CalDavPendingAction.None
        )

        val outcome = resolveSyncSettingsSubmission(
            current = current,
            result = CalDavValidationResult.MissingList("Groceries")
        )

        assertEquals(
            SyncSettingsSubmissionOutcome.Failure(
                current.copy(
                    isSaving = false,
                    statusMessage = "Remote list Groceries does not exist yet",
                    pendingAction = CalDavPendingAction.CreateMissingList
                )
            ),
            outcome
        )
    }

    @Test
    fun `adding an item trims input and clears the field`() = runTest {
        viewModel.onInputValueChange("  Oats  ")

        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("Oats"), repository.addedNames)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `typing shows fuzzy matched suggestions`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "milk", name = "Milk"),
                samplePurchasedItem(id = "almond-milk", name = "Almond Milk"),
                samplePendingItem(id = "bread", name = "Bread")
            )
        )
        advanceUntilIdle()

        viewModel.onInputValueChange("mil")
        advanceUntilIdle()

        assertEquals(listOf("Milk", "Almond Milk"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `typing shows accent insensitive suggestions`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "cafe", name = "Café"),
                samplePendingItem(id = "tea", name = "Tea")
            )
        )
        advanceUntilIdle()

        viewModel.onInputValueChange("cafe")
        advanceUntilIdle()

        assertEquals(listOf("Café"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `typing shows subsequence fuzzy suggestions`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "home", name = "Home"),
                samplePendingItem(id = "honey", name = "Honey"),
                samplePendingItem(id = "bread", name = "Bread")
            )
        )
        advanceUntilIdle()

        viewModel.onInputValueChange("hme")
        advanceUntilIdle()

        assertEquals(listOf("Home"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `adding a duplicate pending item clears input without adding`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "oats", name = "Oats")))
        advanceUntilIdle()

        viewModel.onInputValueChange(" oats ")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.addedNames)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `adding a purchased item reclaims it`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "beans", name = "Beans")))
        advanceUntilIdle()

        viewModel.onInputValueChange(" beans ")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("beans"), repository.pendingRequests)
        assertEquals(listOf("beans"), viewModel.uiState.value.pendingItems.map { it.id })
        assertEquals("", viewModel.uiState.value.inputValue)
    }

    @Test
    fun `adding an accentless duplicate pending item clears input without adding`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "cafe", name = "Café")))
        advanceUntilIdle()

        viewModel.onInputValueChange("cafe")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.addedNames)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `adding an accentless purchased item reclaims it`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "cafe", name = "Café")))
        advanceUntilIdle()

        viewModel.onInputValueChange("cafe")
        viewModel.onAddItem()
        advanceUntilIdle()

        assertEquals(listOf("cafe"), repository.pendingRequests)
        assertEquals(listOf("cafe"), viewModel.uiState.value.pendingItems.map { it.id })
        assertEquals("", viewModel.uiState.value.inputValue)
    }

    @Test
    fun `selecting a suggestion submits it`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "coffee", name = "Coffee")))
        advanceUntilIdle()

        viewModel.onSuggestionSelected("Coffee")
        advanceUntilIdle()

        assertEquals(listOf("coffee"), repository.pendingRequests)
        assertEquals("", viewModel.uiState.value.inputValue)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `pending item click toggles selection`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "rice")))
        advanceUntilIdle()

        viewModel.onPendingItemClicked("rice")
        advanceUntilIdle()
        assertEquals(setOf("rice"), viewModel.uiState.value.selectedIds)

        viewModel.onPendingItemClicked("rice")
        advanceUntilIdle()
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `purchase selected items clears selection and updates repository`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "milk"),
                samplePendingItem(id = "tea", purchaseCount = 2)
            )
        )
        advanceUntilIdle()

        viewModel.onPendingItemClicked("milk")
        viewModel.onPendingItemClicked("tea")
        advanceUntilIdle()

        viewModel.onPurchaseSelectedItems()
        advanceUntilIdle()

        assertEquals(listOf(setOf("milk", "tea")), repository.purchasedRequests)
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
        assertEquals(listOf("milk", "tea"), viewModel.uiState.value.purchasedItems.map { it.id })
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `selected ids are dropped when items leave the pending section`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "eggs")))
        advanceUntilIdle()

        viewModel.onPendingItemClicked("eggs")
        advanceUntilIdle()
        repository.seedItems(listOf(samplePurchasedItem(id = "eggs")))
        advanceUntilIdle()

        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun `purchased item click moves item back to pending`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "coffee")))
        advanceUntilIdle()

        viewModel.onPurchasedItemClicked("coffee")
        advanceUntilIdle()

        assertEquals(listOf("coffee"), repository.pendingRequests)
        assertEquals(listOf("coffee"), viewModel.uiState.value.pendingItems.map { it.id })
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `delete confirmation soft deletes the item and requests sync`() = runTest {
        repository.seedItems(listOf(samplePendingItem(id = "tomatoes")))
        advanceUntilIdle()

        viewModel.onDeleteItemRequested(samplePendingItem(id = "tomatoes"))
        advanceUntilIdle()
        assertEquals("tomatoes", viewModel.uiState.value.itemPendingDeletion?.id)

        viewModel.onDeleteItemConfirmed()
        advanceUntilIdle()

        assertEquals(listOf("tomatoes"), repository.deletedRequests)
        assertEquals(null, viewModel.uiState.value.itemPendingDeletion)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `delete dismissal clears pending deletion item`() = runTest {
        repository.seedItems(listOf(samplePurchasedItem(id = "olive-oil")))
        advanceUntilIdle()

        viewModel.onDeleteItemRequested(samplePurchasedItem(id = "olive-oil"))
        viewModel.onDeleteItemDismissed()

        assertEquals(null, viewModel.uiState.value.itemPendingDeletion)
    }

    @Test
    fun `ui state emits pending and purchased sections`() = runTest {
        repository.seedItems(
            listOf(
                samplePendingItem(id = "bananas"),
                samplePurchasedItem(id = "beans")
            )
        )

        viewModel.uiState.test {
            val firstState = awaitItem()
            val state = if (firstState.pendingItems.isEmpty() && firstState.purchasedItems.isEmpty()) {
                awaitItem()
            } else {
                firstState
            }
            assertEquals(listOf("bananas"), state.pendingItems.map { it.id })
            assertEquals(listOf("beans"), state.purchasedItems.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `opening sync settings shows saved config and saving requests sync`() = runTest {
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle
        )
        validationUseCase.nextResult = CalDavValidationResult.Success("/lists/groceries/")
        advanceUntilIdle()

        viewModel.onSyncMenuClicked()
        viewModel.onSyncSettingsRequested()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isSyncSettingsVisible)
        assertEquals("https://dav.example.com", viewModel.uiState.value.syncSettings.serverUrl)

        viewModel.onSyncEnabledChanged(true)
        viewModel.onSyncServerUrlChanged("https://dav.example.com")
        viewModel.onSyncUsernameChanged("jhow")
        viewModel.onSyncListNameChanged("Groceries")
        viewModel.onSyncPasswordChanged("secret")
        viewModel.onSyncSettingsSaved()
        advanceUntilIdle()

        assertEquals("Groceries", configRepository.currentConfig.listName)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `confirming create missing list clears pending action and requests sync`() = runTest {
        createListUseCase.nextResult = CalDavValidationResult.Success("/lists/groceries/")
        viewModel.onSyncSettingsRequested()
        viewModel.onSyncEnabledChanged(true)
        viewModel.onSyncServerUrlChanged("https://dav.example.com")
        viewModel.onSyncUsernameChanged("jhow")
        viewModel.onSyncListNameChanged("Groceries")
        viewModel.onSyncPasswordChanged("secret")
        advanceUntilIdle()

        viewModel.onConfirmCreateMissingList()
        advanceUntilIdle()

        assertEquals(CalDavPendingAction.None, configRepository.currentConfig.pendingAction)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `save keeps sheet open and shows inline error on validation failure`() = runTest {
        validationUseCase.nextResult = CalDavValidationResult.ConfigurationError("Password is required")
        viewModel.onSyncSettingsRequested()
        advanceUntilIdle()

        viewModel.onSyncSettingsSaved()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isSyncSettingsVisible)
        assertEquals("Password is required", viewModel.uiState.value.syncSettings.statusMessage)
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `save closes sheet only after successful validation and persistence`() = runTest {
        validationUseCase.nextResult = CalDavValidationResult.Success("/lists/groceries/")
        viewModel.onSyncSettingsRequested()
        viewModel.onSyncEnabledChanged(true)
        viewModel.onSyncServerUrlChanged("https://dav.example.com")
        viewModel.onSyncUsernameChanged("jhow")
        viewModel.onSyncPasswordChanged("secret")
        viewModel.onSyncListNameChanged("Groceries")
        advanceUntilIdle()

        viewModel.onSyncSettingsSaved()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSyncSettingsVisible)
        assertEquals("Groceries", configRepository.currentConfig.listName)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `missing list keeps sheet open and shows create-list action immediately`() = runTest {
        validationUseCase.nextResult = CalDavValidationResult.MissingList("Groceries")
        viewModel.onSyncSettingsRequested()
        advanceUntilIdle()

        viewModel.onSyncSettingsSaved()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isSyncSettingsVisible)
        assertEquals(
            CalDavPendingAction.CreateMissingList,
            viewModel.uiState.value.syncSettings.pendingAction
        )
    }

    @Test
    fun `dismissing sync settings clears typed password from form`() = runTest {
        viewModel.onSyncSettingsRequested()
        viewModel.onSyncPasswordChanged("secret")
        advanceUntilIdle()

        viewModel.onSyncSettingsDismissed()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSyncSettingsVisible)
        assertEquals("", viewModel.uiState.value.syncSettings.password)
    }

    @Test
    fun `create missing list coerces blank resolved url to null`() = runTest {
        createListUseCase.nextResult = CalDavValidationResult.Success("")
        viewModel.onSyncSettingsRequested()
        viewModel.onSyncEnabledChanged(true)
        viewModel.onSyncServerUrlChanged("https://dav.example.com")
        viewModel.onSyncUsernameChanged("jhow")
        viewModel.onSyncListNameChanged("Groceries")
        viewModel.onSyncPasswordChanged("secret")
        advanceUntilIdle()

        viewModel.onConfirmCreateMissingList()
        advanceUntilIdle()

        assertEquals(null, configRepository.currentConfig.lastResolvedCollectionUrl)
    }

    @Test
    fun `create missing list closes sheet only after foreground success`() = runTest {
        validationUseCase.nextResult = CalDavValidationResult.MissingList("Groceries")
        createListUseCase.nextResult = CalDavValidationResult.Success("/lists/groceries/")
        viewModel.onSyncSettingsRequested()
        viewModel.onSyncEnabledChanged(true)
        viewModel.onSyncServerUrlChanged("https://dav.example.com")
        viewModel.onSyncUsernameChanged("jhow")
        viewModel.onSyncPasswordChanged("secret")
        viewModel.onSyncListNameChanged("Groceries")
        advanceUntilIdle()

        viewModel.onSyncSettingsSaved()
        advanceUntilIdle()
        viewModel.onConfirmCreateMissingList()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSyncSettingsVisible)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `clearing pending action updates config without requesting sync`() = runTest {
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.UserActionRequired,
            pendingAction = CalDavPendingAction.CreateMissingList
        )
        advanceUntilIdle()

        viewModel.onClearPendingAction()
        advanceUntilIdle()

        assertEquals(CalDavPendingAction.None, configRepository.currentConfig.pendingAction)
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `opening sync settings restores saved values and password presence`() = runTest {
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle,
            hasStoredPassword = true
        )
        advanceUntilIdle()

        viewModel.onSyncSettingsRequested()
        advanceUntilIdle()

        val settings = viewModel.uiState.value.syncSettings
        assertEquals(true, settings.enabled)
        assertEquals("https://dav.example.com", settings.serverUrl)
        assertEquals(true, settings.hasStoredPassword)
        assertEquals("", settings.password)
    }

    @Test
    fun `editing sync fields updates view model form state without saving immediately`() = runTest {
        viewModel.onSyncSettingsRequested()
        advanceUntilIdle()

        viewModel.onSyncServerUrlChanged("https://changed.example.com")
        viewModel.onSyncUsernameChanged("alice")
        advanceUntilIdle()

        assertEquals("https://changed.example.com", viewModel.uiState.value.syncSettings.serverUrl)
        assertEquals("alice", viewModel.uiState.value.syncSettings.username)
        assertEquals("", configRepository.currentConfig.serverUrl)
    }

    @Test
    fun `sync settings projects sync state and pending action from config flow`() = runTest {
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Success,
            statusMessage = "Last sync succeeded",
            pendingAction = CalDavPendingAction.None
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(CalDavSyncState.Success, state.syncSettings.syncState)
        assertEquals("Last sync succeeded", state.syncSettings.statusMessage)
        assertEquals(CalDavPendingAction.None, state.syncSettings.pendingAction)
    }

    private fun samplePendingItem(
        id: String,
        name: String = id,
        purchaseCount: Int = 0
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = false,
        purchaseCount = purchaseCount,
        createdAt = 1L,
        updatedAt = 1L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )

    private fun samplePurchasedItem(id: String, name: String = id): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = true,
        purchaseCount = 3,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )

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
        var callCount: Int = 0
            private set

        override suspend operator fun invoke(
            enabled: Boolean,
            serverUrl: String,
            username: String,
            listName: String,
            password: String
        ): CalDavValidationResult {
            callCount++
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
        var callCount: Int = 0
            private set

        override suspend operator fun invoke(
            serverUrl: String,
            username: String,
            listName: String,
            password: String
        ): CalDavValidationResult {
            callCount++
            return nextResult
        }
    }
}
