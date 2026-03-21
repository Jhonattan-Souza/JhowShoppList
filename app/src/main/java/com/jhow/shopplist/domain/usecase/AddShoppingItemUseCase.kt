package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class AddShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        repository.addItem(normalizedName)
    }
}
