package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class ObserveItemNamesUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    operator fun invoke() = repository.observeAllItemNames()
}
