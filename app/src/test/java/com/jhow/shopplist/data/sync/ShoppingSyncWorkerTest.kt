package com.jhow.shopplist.data.sync

import android.app.Application
import android.content.Context
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerParameters
import androidx.work.WorkerFactory
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.usecase.SyncPendingShoppingItemsUseCase
import com.jhow.shopplist.testing.FakeCalDavConfigRepository
import com.jhow.shopplist.testing.FakeShoppingListRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingSyncWorkerTest {

    @Test
    fun `doWork retries on illegal state`() = runTest {
        val worker = createWorker(IllegalStateException("retry"))

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork returns failure with error message on unexpected exception`() = runTest {
        val worker = createWorker(IllegalArgumentException("boom"))

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals("boom", outputData.getString(ShoppingSyncWorker.ERROR_MESSAGE_KEY))
    }

    private fun createWorker(throwable: Throwable): ShoppingSyncWorker {
        val syncGateway = object : com.jhow.shopplist.domain.sync.ShoppingListSyncGateway {
            override suspend fun sync(): CalDavSyncOutcome {
                throw throwable
            }
        }

        val useCase = SyncPendingShoppingItemsUseCase(
            repository = FakeShoppingListRepository(),
            syncGateway = syncGateway,
            configRepository = FakeCalDavConfigRepository()
        )

        return ShoppingSyncWorker(
            appContext = testContext(),
            workerParams = testWorkerParams(),
            syncPendingShoppingItemsUseCase = useCase
        )
    }

    private fun testContext(): Context {
        return Application()
    }

    private fun testWorkerParams(): WorkerParameters {
        val executor = Executor { runnable -> runnable.run() }
        val workerContext: CoroutineContext = StandardTestDispatcher()
        return WorkerParameters(
            UUID.randomUUID(),
            Data.EMPTY,
            emptyList(),
            WorkerParameters.RuntimeExtras(),
            1,
            0,
            executor,
            workerContext,
            ImmediateTaskExecutor(executor),
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ) = null
            },
            object : ProgressUpdater {
                override fun updateProgress(
                    context: Context,
                    id: UUID,
                    data: Data
                ) = completedFuture()
            },
            object : ForegroundUpdater {
                override fun setForegroundAsync(
                    context: Context,
                    id: UUID,
                    foregroundInfo: ForegroundInfo
                ) = completedFuture()
            }
        )
    }

    private fun completedFuture() = SettableFuture.create<Void>().apply {
        set(null)
    }

    private class ImmediateTaskExecutor(
        private val executor: Executor
    ) : TaskExecutor {
        private val serialExecutor = object : SerialExecutor {
            private val queue = ArrayBlockingQueue<Runnable>(16)

            override fun execute(command: Runnable) {
                queue.add(command)
                while (queue.isNotEmpty()) {
                    executor.execute(queue.remove())
                }
            }

            override fun hasPendingTasks(): Boolean = queue.isNotEmpty()
        }

        override fun getMainThreadExecutor(): Executor = executor

        override fun getSerialTaskExecutor(): SerialExecutor = serialExecutor
    }
}
