package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import javax.inject.Inject

class ClearCalDavPendingActionUseCase @Inject constructor(
    private val repository: CalDavConfigRepository
) {
    suspend operator fun invoke() {
        repository.updateSyncState(
            state = CalDavSyncState.Idle,
            message = null,
            pendingAction = CalDavPendingAction.None
        )
    }
}
