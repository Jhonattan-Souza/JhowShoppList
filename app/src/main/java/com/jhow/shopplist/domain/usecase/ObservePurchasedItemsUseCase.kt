package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class ObservePurchasedItemsUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    operator fun invoke() = repository.observePurchasedItems()
}
