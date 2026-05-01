package com.jhow.shopplist.presentation.shoppinglist

import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelectionController @Inject constructor() {
    private val mutableSelected = MutableStateFlow(emptySet<String>())
    private val mutableIsActive = MutableStateFlow(false)

    val selected: StateFlow<Set<String>> = mutableSelected.asStateFlow()
    val isActive: StateFlow<Boolean> = mutableIsActive.asStateFlow()

    fun enter(itemId: String) {
        if (mutableIsActive.value) return

        mutableSelected.value = setOf(itemId)
        mutableIsActive.value = true
    }

    fun toggle(itemId: String) {
        if (!mutableIsActive.value) return

        val updatedSelection = mutableSelected.value.let { currentSelection ->
            if (itemId in currentSelection) currentSelection - itemId else currentSelection + itemId
        }

        mutableSelected.value = updatedSelection
        mutableIsActive.value = updatedSelection.isNotEmpty()
    }

    fun exit() {
        mutableSelected.value = emptySet()
        mutableIsActive.value = false
    }

    internal fun retainOnly(validItemIds: Set<String>) {
        if (!mutableIsActive.value) return

        val retainedSelection = mutableSelected.value.intersect(validItemIds)
        if (retainedSelection == mutableSelected.value) return

        mutableSelected.value = retainedSelection
        mutableIsActive.value = retainedSelection.isNotEmpty()
    }
}
