package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.testing.FakeShoppingSyncScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestShoppingSyncUseCaseTest {
    private val scheduler = FakeShoppingSyncScheduler()
    private val useCase = RequestShoppingSyncUseCase(scheduler)

    @Test
    fun `request sync delegates to the scheduler`() {
        useCase()

        assertEquals(1, scheduler.requestCount)
    }
}
