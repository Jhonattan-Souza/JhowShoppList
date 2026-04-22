package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import com.jhow.shopplist.domain.sync.ShoppingListSyncGateway
import javax.inject.Inject

class SyncPendingShoppingItemsUseCase @Inject constructor(
    private val repository: ShoppingListRepository,
    private val syncGateway: ShoppingListSyncGateway,
    private val configRepository: CalDavConfigRepository
) {
    suspend operator fun invoke(): Int {
        return when (val outcome = syncGateway.sync()) {
            is CalDavSyncOutcome.Success -> {
                repository.markItemsSynced(outcome.syncedResults)
                configRepository.updateSyncState(
                    state = CalDavSyncState.Success,
                    message = null,
                    lastSyncAt = System.currentTimeMillis()
                )
                outcome.syncedResults.size + outcome.importedCount
            }
            is CalDavSyncOutcome.UserActionRequired -> {
                configRepository.updateSyncState(
                    state = CalDavSyncState.UserActionRequired,
                    message = outcome.message,
                    pendingAction = outcome.pendingAction
                )
                0
            }
            is CalDavSyncOutcome.AuthFailure -> {
                configRepository.updateSyncState(CalDavSyncState.AuthError, outcome.message)
                0
            }
            is CalDavSyncOutcome.NetworkFailure -> {
                configRepository.updateSyncState(CalDavSyncState.NetworkError, outcome.message)
                throw IllegalStateException(outcome.message)
            }
            is CalDavSyncOutcome.ConfigurationFailure -> {
                configRepository.updateSyncState(CalDavSyncState.Warning, outcome.message)
                0
            }
        }
    }
}
