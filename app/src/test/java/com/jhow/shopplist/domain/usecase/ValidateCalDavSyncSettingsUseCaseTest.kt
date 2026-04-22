package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.data.sync.CalDavCollectionCandidate
import com.jhow.shopplist.data.sync.CalDavDiscoveryService
import com.jhow.shopplist.data.sync.CalDavListLocator
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

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
            listLocator = CalDavListLocator(discoveryService)
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
            listLocator = CalDavListLocator(FakeDiscoveryService(candidates = emptyList()))
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
            listLocator = CalDavListLocator(discoveryService)
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
            listLocator = CalDavListLocator(discoveryService)
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
            )
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
            listLocator = CalDavListLocator(FakeDiscoveryService(throwOnFind = IllegalStateException("boom")))
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
    fun `cancellation from locator is rethrown`() = runTest {
        val configRepository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val useCase = ValidateCalDavSyncSettingsUseCase(
            configRepository = configRepository,
            listLocator = CalDavListLocator(
                FakeDiscoveryService(throwOnFind = CancellationException("cancelled"))
            )
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
    }
}
