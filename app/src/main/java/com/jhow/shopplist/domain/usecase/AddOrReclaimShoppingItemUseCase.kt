package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.repository.ShoppingListRepository
import javax.inject.Inject

class AddOrReclaimShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return

        val existingItem = repository.findItemByName(normalizedName)
        when {
            existingItem == null -> repository.addItem(normalizedName)
            !existingItem.isPurchased -> return
            else -> repository.markItemPending(existingItem.id)
        }
    }
}
