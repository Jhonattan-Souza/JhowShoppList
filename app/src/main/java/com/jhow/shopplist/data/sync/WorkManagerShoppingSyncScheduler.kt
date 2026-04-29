package com.jhow.shopplist.data.sync

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.jhow.shopplist.domain.sync.ShoppingSyncScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class WorkManagerShoppingSyncScheduler @Inject constructor(
    private val workManager: WorkManager
) : ShoppingSyncScheduler {
    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<ShoppingSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            ShoppingSyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    override fun observeSyncState(): Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(ShoppingSyncWorker.UNIQUE_WORK_NAME)
            .map { list -> list.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .distinctUntilChanged()
}
