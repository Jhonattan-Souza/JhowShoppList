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
    const val SUGGESTION_LIST = "suggestion_list"
    const val SYNC_MENU_BUTTON = "sync_menu_button"
    const val SYNC_NOW_MENU_ITEM = "sync_now_menu_item"
    const val SYNC_SETTINGS_MENU_ITEM = "sync_settings_menu_item"
    const val SYNC_SETTINGS_SHEET = "sync_settings_sheet"
    const val SYNC_SERVER_FIELD = "sync_server_field"
    const val SYNC_USERNAME_FIELD = "sync_username_field"
    const val SYNC_PASSWORD_FIELD = "sync_password_field"
    const val SYNC_LIST_NAME_FIELD = "sync_list_name_field"
    const val SYNC_SAVE_BUTTON = "sync_save_button"
    const val SYNC_CREATE_LIST_BUTTON = "sync_create_list_button"
    const val SYNC_STATUS_TEXT = "sync_status_text"
    const val SYNC_ENABLED_SWITCH = "sync_enabled_switch"
    const val SYNC_STATE_TEXT = "sync_state_text"
    const val SYNC_SETTINGS_SHEET_CONTENT = "sync_settings_sheet_content"

    fun pendingItem(id: String): String = "pending_item_$id"

    fun purchasedItem(id: String): String = "purchased_item_$id"

    fun swipePendingItem(id: String): String = "swipe_pending_item_$id"

    fun swipePurchasedItem(id: String): String = "swipe_purchased_item_$id"

    fun suggestionItem(name: String): String = "suggestion_item_$name"
}
