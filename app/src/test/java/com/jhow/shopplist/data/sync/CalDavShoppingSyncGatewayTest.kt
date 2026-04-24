package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemRemoteMetadata
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import com.jhow.shopplist.testing.FakeShoppingListRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalDavShoppingSyncGatewayTest {

    @Test
    fun `missing list with create requested creates collection persists href clears request and executes sync`() = runTest {
        val configRepository = FakeCalDavConfigRepository()
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle,
            createListRequested = true
        )
        val discoveryService = FakeDiscoveryService()
        val locator = CalDavListLocator(discoveryService)
        val repository = FakeShoppingListRepository()
        repository.seedItems(
            listOf(
                sampleItem(id = "milk", syncStatus = SyncStatus.PENDING_INSERT)
            )
        )
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = CalDavSyncPlanner(),
            mapper = VTodoMapper(),
            discoveryService = discoveryService
        )
        val gateway = CalDavShoppingSyncGateway(
            configRepository = configRepository,
            listLocator = locator,
            executor = executor,
            discoveryService = discoveryService
        )

        val outcome = gateway.sync()

        assertTrue(discoveryService.createCollectionCalled)
        assertEquals("Groceries", discoveryService.lastListName)
        assertEquals("https://dav.example.com/Groceries", configRepository.currentConfig.lastResolvedCollectionUrl)
        assertFalse(configRepository.currentConfig.createListRequested)
        assertTrue(outcome is CalDavSyncOutcome.Success)
        val success = outcome as CalDavSyncOutcome.Success
        assertEquals(1, success.syncedResults.size)
        assertEquals("https://dav.example.com/Groceries/milk.ics", success.syncedResults[0].remoteHref)
    }

    @Test
    fun `missing list without create requested returns user action required`() = runTest {
        val configRepository = FakeCalDavConfigRepository()
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle,
            createListRequested = false
        )
        val discoveryService = FakeDiscoveryService()
        val locator = CalDavListLocator(discoveryService)
        val executor = CalDavSyncExecutor(
            repository = FakeShoppingListRepository(),
            planner = CalDavSyncPlanner(),
            mapper = VTodoMapper(),
            discoveryService = discoveryService
        )
        val gateway = CalDavShoppingSyncGateway(
            configRepository = configRepository,
            listLocator = locator,
            executor = executor,
            discoveryService = discoveryService
        )

        val outcome = gateway.sync()

        assertFalse(discoveryService.createCollectionCalled)
        assertEquals(
            CalDavSyncOutcome.UserActionRequired(
                message = "Remote list Groceries is missing",
                pendingAction = CalDavPendingAction.CreateMissingList
            ),
            outcome
        )
    }

    @Test
    fun `found list persists resolved href and executes sync`() = runTest {
        val configRepository = FakeCalDavConfigRepository()
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle
        )
        val discoveryService = FakeDiscoveryService(
            candidates = listOf(
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries/")
            )
        )
        val locator = CalDavListLocator(discoveryService)
        val repository = FakeShoppingListRepository()
        repository.seedItems(
            listOf(
                sampleItem(id = "bread", syncStatus = SyncStatus.SYNCED)
            )
        )
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = CalDavSyncPlanner(),
            mapper = VTodoMapper(),
            discoveryService = discoveryService
        )
        val gateway = CalDavShoppingSyncGateway(
            configRepository = configRepository,
            listLocator = locator,
            executor = executor,
            discoveryService = discoveryService
        )

        val outcome = gateway.sync()

        assertEquals("/lists/groceries/", configRepository.currentConfig.lastResolvedCollectionUrl)
        assertTrue(outcome is CalDavSyncOutcome.Success)
    }

    @Test
    fun `create collection throws but subsequent locate finds list persists href clears flag and succeeds`() = runTest {
        val configRepository = FakeCalDavConfigRepository()
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle,
            createListRequested = true
        )
        val discoveryService = FakeDiscoveryService(
            throwOnCreate = true,
            candidatesAfterCreate = listOf(
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries/")
            )
        )
        val locator = CalDavListLocator(discoveryService)
        val repository = FakeShoppingListRepository()
        repository.seedItems(listOf(sampleItem(id = "bread", syncStatus = SyncStatus.SYNCED)))
        val executor = CalDavSyncExecutor(
            repository = repository,
            planner = CalDavSyncPlanner(),
            mapper = VTodoMapper(),
            discoveryService = discoveryService
        )
        val gateway = CalDavShoppingSyncGateway(
            configRepository = configRepository,
            listLocator = locator,
            executor = executor,
            discoveryService = discoveryService
        )

        val outcome = gateway.sync()

        assertTrue(discoveryService.createCollectionCalled)
        assertTrue(discoveryService.findTaskCollectionsCalled)
        assertEquals("/lists/groceries/", configRepository.currentConfig.lastResolvedCollectionUrl)
        assertFalse(configRepository.currentConfig.createListRequested)
        assertTrue(outcome is CalDavSyncOutcome.Success)
    }

    @Test
    fun `create collection throws and still missing rethrows original exception`() = runTest {
        val configRepository = FakeCalDavConfigRepository()
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle,
            createListRequested = true
        )
        val discoveryService = FakeDiscoveryService(
            throwOnCreate = true,
            candidatesAfterCreate = emptyList()
        )
        val locator = CalDavListLocator(discoveryService)
        val executor = CalDavSyncExecutor(
            repository = FakeShoppingListRepository(),
            planner = CalDavSyncPlanner(),
            mapper = VTodoMapper(),
            discoveryService = discoveryService
        )
        val gateway = CalDavShoppingSyncGateway(
            configRepository = configRepository,
            listLocator = locator,
            executor = executor,
            discoveryService = discoveryService
        )

        try {
            gateway.sync()
            throw AssertionError("Expected exception to be rethrown")
        } catch (e: RuntimeException) {
            assertEquals("create failed", e.message)
        }
    }

    @Test
    fun `create collection throws and ambiguous returns configuration failure`() = runTest {
        val configRepository = FakeCalDavConfigRepository()
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle,
            createListRequested = true
        )
        val discoveryService = FakeDiscoveryService(
            throwOnCreate = true,
            candidatesAfterCreate = listOf(
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries1/"),
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries2/")
            )
        )
        val locator = CalDavListLocator(discoveryService)
        val executor = CalDavSyncExecutor(
            repository = FakeShoppingListRepository(),
            planner = CalDavSyncPlanner(),
            mapper = VTodoMapper(),
            discoveryService = discoveryService
        )
        val gateway = CalDavShoppingSyncGateway(
            configRepository = configRepository,
            listLocator = locator,
            executor = executor,
            discoveryService = discoveryService
        )

        val outcome = gateway.sync()

        assertEquals(
            CalDavSyncOutcome.ConfigurationFailure("Multiple lists named Groceries were found"),
            outcome
        )
    }

    @Test
    fun `ambiguous list returns configuration failure`() = runTest {
        val configRepository = FakeCalDavConfigRepository()
        configRepository.seed(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            syncState = CalDavSyncState.Idle
        )
        val discoveryService = FakeDiscoveryService(
            candidates = listOf(
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries1/"),
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries2/")
            )
        )
        val locator = CalDavListLocator(discoveryService)
        val executor = CalDavSyncExecutor(
            repository = FakeShoppingListRepository(),
            planner = CalDavSyncPlanner(),
            mapper = VTodoMapper(),
            discoveryService = discoveryService
        )
        val gateway = CalDavShoppingSyncGateway(
            configRepository = configRepository,
            listLocator = locator,
            executor = executor,
            discoveryService = discoveryService
        )

        val outcome = gateway.sync()

        assertEquals(
            CalDavSyncOutcome.ConfigurationFailure("Multiple lists named Groceries were found"),
            outcome
        )
    }

    private class FakeDiscoveryService(
        private val candidates: List<CalDavCollectionCandidate> = emptyList(),
        private val throwOnCreate: Boolean = false,
        private val candidatesAfterCreate: List<CalDavCollectionCandidate>? = null
    ) : CalDavDiscoveryService {
        var createCollectionCalled = false
        var findTaskCollectionsCalled = false
        var lastListName: String? = null
        private var findCallCount = 0

        override suspend fun findTaskCollections(
            serverUrl: String,
            username: String,
            password: String
        ): List<CalDavCollectionCandidate> {
            findTaskCollectionsCalled = true
            findCallCount++
            return if (findCallCount == 1 || candidatesAfterCreate == null) candidates else candidatesAfterCreate
        }

        override suspend fun createTaskCollection(
            serverUrl: String,
            username: String,
            password: String,
            listName: String
        ): String {
            createCollectionCalled = true
            lastListName = listName
            if (throwOnCreate) throw RuntimeException("create failed")
            return "$serverUrl/$listName"
        }

        override suspend fun fetchTaskItems(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String
        ): List<RemoteShoppingItemSnapshot> = emptyList()
    }

    private fun sampleItem(
        id: String,
        syncStatus: SyncStatus
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = id,
        isPurchased = false,
        purchaseCount = 0,
        createdAt = 1L,
        updatedAt = 1L,
        isDeleted = false,
        syncStatus = syncStatus,
        remoteMetadata = ShoppingItemRemoteMetadata()
    )
}
