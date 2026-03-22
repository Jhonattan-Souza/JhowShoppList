package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.testing.FakeShoppingListRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddOrReclaimShoppingItemUseCaseTest {
    private val repository = FakeShoppingListRepository()
    private val useCase = AddOrReclaimShoppingItemUseCase(repository)

    @Test
    fun `blank input is ignored`() = runTest {
        useCase("   ")

        assertEquals(emptyList<String>(), repository.addedNames)
    }

    @Test
    fun `trimmed name is stored for a new item`() = runTest {
        useCase("  Coffee beans  ")

        assertEquals(listOf("Coffee beans"), repository.addedNames)
    }

    @Test
    fun `duplicate pending item is silently ignored`() = runTest {
        repository.seedItems(listOf(item(id = "milk", name = "Milk", isPurchased = false)))

        useCase("  milk  ")

        assertEquals(emptyList<String>(), repository.addedNames)
        assertEquals(emptyList<String>(), repository.pendingRequests)
    }

    @Test
    fun `purchased item is reclaimed`() = runTest {
        repository.seedItems(listOf(item(id = "coffee", name = "Coffee", isPurchased = true)))

        useCase(" coffee ")

        assertEquals(emptyList<String>(), repository.addedNames)
        assertEquals(listOf("coffee"), repository.pendingRequests)
    }

    private fun item(id: String, name: String, isPurchased: Boolean): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = isPurchased,
        purchaseCount = 2,
        createdAt = 1L,
        updatedAt = 2L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )
}
