package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class MarkPurchasedItemPendingUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(id: String) {
        repository.markItemPending(id)
    }
}
