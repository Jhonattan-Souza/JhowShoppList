package com.jhow.shopplist.domain.model

sealed interface CalDavSyncOutcome {
    data class Success(
        val syncedResults: List<ShoppingItemSyncResult>,
        val importedCount: Int
    ) : CalDavSyncOutcome

    data class UserActionRequired(
        val message: String,
        val pendingAction: CalDavPendingAction
    ) : CalDavSyncOutcome

    data class AuthFailure(val message: String) : CalDavSyncOutcome
    data class NetworkFailure(val message: String) : CalDavSyncOutcome
    data class ConfigurationFailure(val message: String) : CalDavSyncOutcome
}
