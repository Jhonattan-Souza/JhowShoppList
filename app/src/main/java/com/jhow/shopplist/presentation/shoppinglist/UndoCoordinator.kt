package com.jhow.shopplist.presentation.shoppinglist

import com.jhow.shopplist.domain.model.ShoppingItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteUndoSnackbarState(
    val count: Int
)

class UndoCoordinator(
    private val timeoutMillis: Long,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val onOptimisticDelete: suspend (ShoppingItem) -> Unit,
    private val onRestore: suspend (ShoppingItem) -> Unit,
    private val onCommit: suspend (List<ShoppingItem>) -> Unit
) {
    private val pendingItems = mutableListOf<ShoppingItem>()
    private var timeoutJob: Job? = null
    private val mutableSnackbarState = MutableStateFlow<DeleteUndoSnackbarState?>(null)

    val snackbarState: StateFlow<DeleteUndoSnackbarState?> = mutableSnackbarState.asStateFlow()

    fun onDelete(item: ShoppingItem) {
        scope.launch(dispatcher) {
            onOptimisticDelete(item)
            pendingItems += item
            mutableSnackbarState.value = DeleteUndoSnackbarState(count = pendingItems.size)
            restartTimeout()
        }
    }

    fun onUndo() {
        scope.launch(dispatcher) {
            timeoutJob?.cancel()
            val itemsToRestore = pendingItems.toList()
            pendingItems.clear()
            mutableSnackbarState.value = null
            itemsToRestore.forEach { item -> onRestore(item) }
        }
    }

    private fun restartTimeout() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch(dispatcher) {
            delay(timeoutMillis)
            commitPendingItems()
        }
    }

    private suspend fun commitPendingItems() {
        val itemsToCommit = pendingItems.toList()
        pendingItems.clear()
        mutableSnackbarState.value = null
        if (itemsToCommit.isNotEmpty()) {
            onCommit(itemsToCommit)
        }
    }
}
