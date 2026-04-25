package com.jhow.shopplist.presentation.shoppinglist

sealed interface ShoppingListUiEvent {
    data object SyncNotConfigured : ShoppingListUiEvent
}
