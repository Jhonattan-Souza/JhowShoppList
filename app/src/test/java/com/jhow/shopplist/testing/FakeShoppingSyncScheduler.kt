package com.jhow.shopplist.testing

import com.jhow.shopplist.domain.sync.ShoppingSyncScheduler

class FakeShoppingSyncScheduler : ShoppingSyncScheduler {
    var requestCount: Int = 0

    override fun requestSync() {
        requestCount += 1
    }
}
