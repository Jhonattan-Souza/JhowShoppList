package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class DeleteShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(id: String) {
        repository.softDeleteItem(id)
    }
}
