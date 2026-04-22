package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import com.jhow.shopplist.domain.sync.CalDavSyncConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCalDavSyncConfigUseCase @Inject constructor(
    private val repository: CalDavConfigRepository
) {
    operator fun invoke(): Flow<CalDavSyncConfig> = repository.observeConfig()
}
