package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.sync.ShoppingSyncScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSyncStateUseCase @Inject constructor(
    private val scheduler: ShoppingSyncScheduler
) {
    operator fun invoke(): Flow<Boolean> = scheduler.observeSyncState()
}
