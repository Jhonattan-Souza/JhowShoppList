package com.jhow.shopplist.domain.sync

import kotlinx.coroutines.flow.Flow

interface ShoppingSyncScheduler {
    fun requestSync()
    fun observeSyncState(): Flow<Boolean>
}
