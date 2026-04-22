package com.jhow.shopplist.domain.sync

import com.jhow.shopplist.domain.model.CalDavSyncOutcome

interface ShoppingListSyncGateway {
    suspend fun sync(): CalDavSyncOutcome
}
