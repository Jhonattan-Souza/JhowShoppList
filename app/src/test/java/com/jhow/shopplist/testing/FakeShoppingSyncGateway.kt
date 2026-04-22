package com.jhow.shopplist.testing

import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.sync.ShoppingListSyncGateway

class FakeShoppingSyncGateway : ShoppingListSyncGateway {
    var nextOutcome: CalDavSyncOutcome = CalDavSyncOutcome.Success(emptyList(), 0)

    override suspend fun sync(): CalDavSyncOutcome = nextOutcome
}
