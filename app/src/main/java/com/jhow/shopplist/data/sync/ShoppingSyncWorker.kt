package com.jhow.shopplist.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jhow.shopplist.domain.usecase.SyncPendingShoppingItemsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ShoppingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPendingShoppingItemsUseCase: SyncPendingShoppingItemsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        syncPendingShoppingItemsUseCase()
        Result.success()
    } catch (_: Throwable) {
        Result.retry()
    }

    companion object {
        const val UNIQUE_WORK_NAME: String = "shopping-sync"
    }
}
