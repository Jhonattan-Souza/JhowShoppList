package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.testing.FakeShoppingListRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddShoppingItemUseCaseTest {
    private val repository = FakeShoppingListRepository()
    private val useCase = AddShoppingItemUseCase(repository)

    @Test
    fun `trimmed names are stored`() = runTest {
        useCase("  Coffee beans  ")

        assertEquals(listOf("Coffee beans"), repository.addedNames)
    }

    @Test
    fun `blank names are ignored`() = runTest {
        useCase("   ")

        assertEquals(emptyList<String>(), repository.addedNames)
    }
}
