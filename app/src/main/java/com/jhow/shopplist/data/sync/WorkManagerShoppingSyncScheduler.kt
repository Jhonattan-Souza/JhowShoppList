package com.jhow.shopplist.data.sync

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jhow.shopplist.domain.sync.ShoppingSyncScheduler
import javax.inject.Inject

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
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
