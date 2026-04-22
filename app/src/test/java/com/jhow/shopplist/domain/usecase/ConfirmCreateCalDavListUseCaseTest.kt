package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmCreateCalDavListUseCaseTest {

    @Test
    fun `confirming create missing list sets create request flag and clears pending action atomically`() = runTest {
        val repository = FakeCalDavConfigRepository()
        repository.seed(
            enabled = true,
            syncState = CalDavSyncState.UserActionRequired,
            pendingAction = CalDavPendingAction.CreateMissingList
        )
        val useCase = ConfirmCreateCalDavListUseCase(repository)

        useCase()

        assertEquals(true, repository.currentConfig.createListRequested)
        assertEquals(CalDavSyncState.Idle, repository.currentConfig.syncState)
        assertEquals(CalDavPendingAction.None, repository.currentConfig.pendingAction)
        assertEquals(null, repository.currentConfig.statusMessage)
        assertEquals(1, repository.atomicWriteCount)
    }
}
