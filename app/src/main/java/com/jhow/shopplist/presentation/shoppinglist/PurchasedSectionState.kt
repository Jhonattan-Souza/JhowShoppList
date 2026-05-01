package com.jhow.shopplist.presentation.shoppinglist

enum class PurchasedSectionVisibility {
    Collapsed,
    Expanded
}

object PurchasedSectionState {
    fun resolve(
        pendingCount: Int,
        purchasedCount: Int,
        userExpanded: Boolean?
    ): PurchasedSectionVisibility {
        require(pendingCount >= 0) { "pendingCount must not be negative" }
        require(purchasedCount >= 0) { "purchasedCount must not be negative" }

        userExpanded?.let { expanded ->
            return if (expanded) {
                PurchasedSectionVisibility.Expanded
            } else {
                PurchasedSectionVisibility.Collapsed
            }
        }

        return if (pendingCount == 0) {
            PurchasedSectionVisibility.Expanded
        } else {
            PurchasedSectionVisibility.Collapsed
        }
    }
}
