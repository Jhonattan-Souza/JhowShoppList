package com.jhow.shopplist.data.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CalDavListLocatorTest {

    @Test
    fun `returns matching collection when exactly one list has the configured display name`() = runTest {
        val locator = CalDavListLocator(FakeDiscoveryService(listOf(
            CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries/"),
            CalDavCollectionCandidate(displayName = "Work", href = "/lists/work/")
        )))

        val result = locator.locate(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            listName = "Groceries"
        )

        assertEquals(CalDavListLocator.Result.Found("/lists/groceries/"), result)
    }

    @Test
    fun `returns Missing when no collection matches`() = runTest {
        val locator = CalDavListLocator(FakeDiscoveryService(emptyList()))

        val result = locator.locate(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            listName = "Groceries"
        )

        assertEquals(CalDavListLocator.Result.Missing, result)
    }

    @Test
    fun `returns Ambiguous when multiple collections match`() = runTest {
        val locator = CalDavListLocator(FakeDiscoveryService(listOf(
            CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries1/"),
            CalDavCollectionCandidate(displayName = "Groceries", href = "/lists/groceries2/")
        )))

        val result = locator.locate(
            serverUrl = "https://dav.example.com",
            username = "jhow",
            password = "secret",
            listName = "Groceries"
        )

        assertEquals(CalDavListLocator.Result.Ambiguous, result)
    }

    private class FakeDiscoveryService(
        private val candidates: List<CalDavCollectionCandidate>
    ) : CalDavDiscoveryService {
        override suspend fun findTaskCollections(
            serverUrl: String,
            username: String,
            password: String
        ): List<CalDavCollectionCandidate> = candidates

        override suspend fun createTaskCollection(
            serverUrl: String,
            username: String,
            password: String,
            listName: String
        ): String = "$serverUrl/$listName/"
    }
}
