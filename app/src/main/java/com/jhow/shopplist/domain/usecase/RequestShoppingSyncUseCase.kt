package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.sync.ShoppingSyncScheduler
import javax.inject.Inject

class RequestShoppingSyncUseCase @Inject constructor(
    private val scheduler: ShoppingSyncScheduler
) {
    operator fun invoke() {
        scheduler.requestSync()
    }
}
