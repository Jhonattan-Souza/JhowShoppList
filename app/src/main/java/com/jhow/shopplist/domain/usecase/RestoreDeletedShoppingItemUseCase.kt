package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class RestoreDeletedShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(item: ShoppingItem) {
        repository.restoreDeletedItem(item)
    }
}
