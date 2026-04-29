package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.data.sync.CalDavCollectionCandidate
import com.jhow.shopplist.data.sync.CalDavAuthenticationException
import com.jhow.shopplist.data.sync.CalDavDiscoveryService
import com.jhow.shopplist.data.sync.CalDavListLocator
import com.jhow.shopplist.data.sync.CalDavTaskDeleteResult
import com.jhow.shopplist.data.sync.CalDavTaskUpsertResult
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateCalDavSyncSettingsUseCaseTest {

    @Test
    fun `successful validation returns success when blank password falls back to stored password`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            seed(
                enabled = true,
                serverUrl = "https://dav.example.com",
                username = "jhow",
                listName = "Groceries",
                hasStoredPassword = true
            )
            setStoredPasswordAvailable()
        }
        val discoveryService = FakeDiscoveryService(
            candidates = listOf(
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries/")
            )
        )
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(discoveryService),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(CalDavValidationResult.Success("/lists/groceries/"), result)
        assertEquals(1, discoveryService.findCalls)
    }

    @Test
    fun `missing list returns MissingList with list name`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(FakeDiscoveryService(candidates = emptyList())),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(CalDavValidationResult.MissingList("Groceries"), result)
    }

    @Test
    fun `configuration error when password is blank and no stored password exists`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            clearStoredPassword()
        }
        val discoveryService = FakeDiscoveryService(candidates = emptyList())
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(discoveryService),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(
            CalDavValidationResult.ConfigurationError("Password is required"),
            result
        )
        assertEquals(0, discoveryService.findCalls)
    }

    @Test
    fun `disabled sync returns success without lookup`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            clearStoredPassword()
        }
        val discoveryService = FakeDiscoveryService(candidates = emptyList())
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(discoveryService),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            enabled = false,
            serverUrl = "",
            username = "",
            listName = "",
            password = ""
        )

        assertEquals(CalDavValidationResult.Success(), result)
        assertEquals(0, discoveryService.findCalls)
    }

    @Test
    fun `ambiguous list maps to configuration error`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(
                FakeDiscoveryService(
                    candidates = listOf(
                        CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/one/"),
                        CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/two/")
                    )
                )
            ),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(
            CalDavValidationResult.ConfigurationError("Multiple matching lists were found"),
            result
        )
    }

    @Test
    fun `locator failure maps to network error`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(FakeDiscoveryService(throwOnFind = IllegalStateException("boom"))),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(
            CalDavValidationResult.NetworkError("Unable to validate sync settings"),
            result
        )
    }

    @Test
    fun `authentication failure maps to auth error`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(
                FakeDiscoveryService(throwOnFind = CalDavAuthenticationException())
            ),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            enabled = true,
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(
            CalDavValidationResult.AuthError("Authentication failed"),
            result
        )
    }

    @Test
    fun `validation runs locator work on injected io dispatcher`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val recorder = ContextRecordingDiscoveryService(
            candidates = listOf(
                CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries/")
            )
        )
        val dispatcher = newNamedDispatcher("caldav-validation-io")
        try {
            val useCase = ValidateCalDavSyncSettingsUseCase(
                configRepository = configRepository,
                listLocator = CalDavListLocator(recorder),
                ioDispatcher = dispatcher
            )

            val result = useCase(
                enabled = true,
                serverUrl = "https://dav.example.com",
                username = "jhow",
                listName = "Groceries",
                password = ""
            )

            assertEquals(CalDavValidationResult.Success("/lists/groceries/"), result)
            assertTrue(recorder.recordedThreadName?.contains("caldav-validation-io") == true)
        } finally {
            dispatcher.close()
        }
    }

    @Test
    fun `cancellation from locator is rethrown`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(
                FakeDiscoveryService(throwOnFind = CancellationException("cancelled"))
            ),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        try {
            useCase(
                enabled = true,
                serverUrl = "https://dav.example.com",
                username = "jhow",
                listName = "Groceries",
                password = ""
            )
            fail("Expected CancellationException to be rethrown")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `fake config repository seed false clears stored password state`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
            seed(hasStoredPassword = false)
        }

        assertEquals(false, configRepository.currentConfig.hasStoredPassword)
        assertEquals(null, configRepository.getPassword())
    }

    @Test
    fun `fake config repository seed true restores stored password state`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            clearStoredPassword()
            seed(hasStoredPassword = true)
        }

        assertEquals(true, configRepository.currentConfig.hasStoredPassword)
        assertEquals("fake-password", configRepository.getPassword())
    }

    @Test
    fun `fake config repository derives stored password flag from stored password`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            seed(hasStoredPassword = false)
            setStoredPasswordAvailable()
        }

        assertEquals(true, configRepository.observeConfig().first().hasStoredPassword)

        configRepository.clearStoredPassword()

        assertEquals(false, configRepository.observeConfig().first().hasStoredPassword)
    }

    private class FakeDiscoveryService(
        private val candidates: List<CalDavCollectionCandidate> = emptyList(),
        private val throwOnFind: Throwable? = null
    ) : CalDavDiscoveryService {
        var findCalls: Int = 0
            private set

        override suspend fun findTaskCollections(
            serverUrl: String,
            username: String,
            password: String
        ): List<CalDavCollectionCandidate> {
            findCalls++
            throwOnFind?.let { throw it }
            return candidates
        }

        override suspend fun createTaskCollection(
            serverUrl: String,
            username: String,
            password: String,
            listName: String
        ): String = error("Not used in validation tests")

        override suspend fun fetchTaskItems(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String
        ): List<RemoteShoppingItemSnapshot> = emptyList()

        override suspend fun upsertTaskItem(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String,
            item: com.jhow.shopplist.domain.model.ShoppingItem
        ): CalDavTaskUpsertResult = error("Not used in validation tests")

        override suspend fun deleteTaskItem(
            serverUrl: String,
            username: String,
            password: String,
            href: String,
            eTag: String?
        ): CalDavTaskDeleteResult = error("Not used in validation tests")
    }

    private class ContextRecordingDiscoveryService(
        private val candidates: List<CalDavCollectionCandidate>
    ) : CalDavDiscoveryService {
        var recordedThreadName: String? = null
            private set

        override suspend fun findTaskCollections(
            serverUrl: String,
            username: String,
            password: String
        ): List<CalDavCollectionCandidate> {
            recordedThreadName = Thread.currentThread().name
            return candidates
        }

        override suspend fun createTaskCollection(
            serverUrl: String,
            username: String,
            password: String,
            listName: String
        ): String = error("Not used in validation tests")

        override suspend fun fetchTaskItems(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String
        ): List<RemoteShoppingItemSnapshot> = emptyList()

        override suspend fun upsertTaskItem(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String,
            item: com.jhow.shopplist.domain.model.ShoppingItem
        ): CalDavTaskUpsertResult = error("Not used in validation tests")

        override suspend fun deleteTaskItem(
            serverUrl: String,
            username: String,
            password: String,
            href: String,
            eTag: String?
        ): CalDavTaskDeleteResult = error("Not used in validation tests")
    }

    private fun newNamedDispatcher(threadName: String): ExecutorCoroutineDispatcher {
        return Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, threadName)
        }.asCoroutineDispatcher()
    }
}
