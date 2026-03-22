package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.testing.FakeShoppingListRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteShoppingItemUseCaseTest {
    private val repository = FakeShoppingListRepository()
    private val useCase = DeleteShoppingItemUseCase(repository)

    @Test
    fun `delete forwards the item id to repository`() = runTest {
        useCase("item-42")

        assertEquals(listOf("item-42"), repository.deletedRequests)
    }
}
