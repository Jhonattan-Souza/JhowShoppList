package com.jhow.shopplist.testing

import com.jhow.shopplist.domain.sync.ShoppingSyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeShoppingSyncScheduler : ShoppingSyncScheduler {
    var requestCount: Int = 0
    private val _syncing = MutableStateFlow(false)

    override fun requestSync() {
        requestCount += 1
    }

    override fun observeSyncState(): Flow<Boolean> = _syncing

    fun setSyncing(value: Boolean) {
        _syncing.value = value
    }
}
