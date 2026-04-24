package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.data.sync.CalDavCollectionCandidate
import com.jhow.shopplist.data.sync.CalDavAuthenticationException
import com.jhow.shopplist.data.sync.CalDavDiscoveryService
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmCreateCalDavListUseCaseTest {

    @Test
    fun `confirming create missing list returns created href`() = runTest {
        val repository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val discoveryService = FakeDiscoveryService(createdHref = "/lists/groceries/")
        val useCase = ConfirmCreateCalDavListUseCase(
            repository,
            discoveryService,
            UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(CalDavValidationResult.Success("/lists/groceries/"), result)
        assertEquals(1, discoveryService.createCalls)
    }

    @Test
    fun `blank password falls back to stored password in create flow`() = runTest {
        val repository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val discoveryService = FakeDiscoveryService(createdHref = "/lists/groceries/")
        val useCase = ConfirmCreateCalDavListUseCase(
            repository,
            discoveryService,
            UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(CalDavValidationResult.Success("/lists/groceries/"), result)
        assertEquals("fake-password", discoveryService.lastPassword)
    }

    @Test
    fun `authentication failure returns auth error`() = runTest {
        val repository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val discoveryService = FakeDiscoveryService(
            createdHref = "/lists/groceries/",
            throwOnCreate = CalDavAuthenticationException()
        )
        val useCase = ConfirmCreateCalDavListUseCase(
            repository,
            discoveryService,
            UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(CalDavValidationResult.AuthError("Authentication failed"), result)
    }

    @Test
    fun `generic failure returns network error`() = runTest {
        val repository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val discoveryService = FakeDiscoveryService(
            createdHref = "/lists/groceries/",
            throwOnCreate = IllegalStateException("boom")
        )
        val useCase = ConfirmCreateCalDavListUseCase(
            repository,
            discoveryService,
            UnconfinedTestDispatcher(testScheduler)
        )

        val result = useCase(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(CalDavValidationResult.NetworkError("Unable to create remote list"), result)
    }

    @Test
    fun `create missing list runs network work on injected io dispatcher`() = runTest {
        val repository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val dispatcher = newNamedDispatcher("caldav-create-io")
        try {
            val discoveryService = ContextRecordingDiscoveryService(createdHref = "/lists/groceries/")
            val useCase = ConfirmCreateCalDavListUseCase(
                repository = repository,
                discoveryService = discoveryService,
                ioDispatcher = dispatcher
            )

            val result = useCase(
                serverUrl = "https://dav.example.com",
                username = "jhow",
                listName = "Groceries",
                password = ""
            )

            assertEquals(CalDavValidationResult.Success("/lists/groceries/"), result)
            assertTrue(discoveryService.recordedThreadName?.contains("caldav-create-io") == true)
        } finally {
            dispatcher.close()
        }
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        val repository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val discoveryService = FakeDiscoveryService(
            createdHref = "/lists/groceries/",
            throwOnCreate = CancellationException("cancelled")
        )
        val useCase = ConfirmCreateCalDavListUseCase(
            repository,
            discoveryService,
            UnconfinedTestDispatcher(testScheduler)
        )

        try {
            useCase(
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

    private class FakeDiscoveryService(
        private val createdHref: String,
        private val throwOnCreate: Throwable? = null
    ) : CalDavDiscoveryService {
        var createCalls: Int = 0
            private set
        var lastPassword: String? = null
            private set

        override suspend fun findTaskCollections(
            serverUrl: String,
            username: String,
            password: String
        ): List<CalDavCollectionCandidate> = emptyList()

        override suspend fun createTaskCollection(
            serverUrl: String,
            username: String,
            password: String,
            listName: String
        ): String {
            createCalls++
            lastPassword = password
            throwOnCreate?.let { throw it }
            return createdHref
        }

        override suspend fun fetchTaskItems(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String
        ): List<RemoteShoppingItemSnapshot> = emptyList()
    }

    private class ContextRecordingDiscoveryService(
        private val createdHref: String
    ) : CalDavDiscoveryService {
        var recordedThreadName: String? = null
            private set

        override suspend fun findTaskCollections(
            serverUrl: String,
            username: String,
            password: String
        ): List<CalDavCollectionCandidate> = emptyList()

        override suspend fun createTaskCollection(
            serverUrl: String,
            username: String,
            password: String,
            listName: String
        ): String {
            recordedThreadName = Thread.currentThread().name
            return createdHref
        }

        override suspend fun fetchTaskItems(
            serverUrl: String,
            username: String,
            password: String,
            collectionHref: String
        ): List<RemoteShoppingItemSnapshot> = emptyList()
    }

    private fun newNamedDispatcher(threadName: String): ExecutorCoroutineDispatcher {
        return Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, threadName)
        }.asCoroutineDispatcher()
    }
}
