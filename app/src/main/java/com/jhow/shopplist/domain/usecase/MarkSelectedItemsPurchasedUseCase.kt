package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class MarkSelectedItemsPurchasedUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(ids: Set<String>) {
        repository.markItemsPurchased(ids)
    }
}
