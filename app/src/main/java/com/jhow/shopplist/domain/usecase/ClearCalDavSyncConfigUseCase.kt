package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import javax.inject.Inject

class ClearCalDavSyncConfigUseCase @Inject constructor(
    private val repository: CalDavConfigRepository
) {
    suspend operator fun invoke() {
        repository.clearConfig()
    }
}
