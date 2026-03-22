package com.jhow.shopplist.presentation.shoppinglist

object ShoppingListTestTags {
    const val SCREEN = "shopping_list_screen"
    const val INPUT_FIELD = "shopping_list_input"
    const val PURCHASE_SELECTED_FAB = "purchase_selected_fab"
    const val PENDING_SECTION = "pending_section"
    const val PURCHASED_SECTION = "purchased_section"
    const val SECTION_DIVIDER = "shopping_list_divider"
    const val EMPTY_STATE = "empty_state"
    const val DELETE_ITEM_DIALOG = "delete_item_dialog"

    fun pendingItem(id: String): String = "pending_item_$id"

    fun purchasedItem(id: String): String = "purchased_item_$id"

    fun deletePendingItem(id: String): String = "delete_pending_item_$id"

    fun deletePurchasedItem(id: String): String = "delete_purchased_item_$id"
}
