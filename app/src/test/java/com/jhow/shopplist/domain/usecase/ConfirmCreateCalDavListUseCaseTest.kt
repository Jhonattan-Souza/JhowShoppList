package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.data.sync.CalDavCollectionCandidate
import com.jhow.shopplist.data.sync.CalDavDiscoveryService
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmCreateCalDavListUseCaseTest {

    @Test
    fun `confirming create missing list returns created href`() = runTest {
        val repository = FakeCalDavConfigRepository().apply {
            setStoredPasswordAvailable()
        }
        val discoveryService = FakeDiscoveryService(createdHref = "/lists/groceries/")
        val useCase = ConfirmCreateCalDavListUseCase(repository, discoveryService)

        val result = useCase(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            listName = "Groceries",
            password = ""
        )

        assertEquals(CalDavValidationResult.Success("/lists/groceries/"), result)
        assertEquals(1, discoveryService.createCalls)
    }

    private class FakeDiscoveryService(
        private val createdHref: String
    ) : CalDavDiscoveryService {
        var createCalls: Int = 0
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
            return createdHref
        }
    }
}
