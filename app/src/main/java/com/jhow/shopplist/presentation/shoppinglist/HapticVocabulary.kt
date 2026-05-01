package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class HapticVocabulary(private val hapticFeedback: HapticFeedback) {
    fun add() = lightTap()

    fun purchaseTap() = lightTap()

    fun restore() = lightTap()

    fun swipeThreshold() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun multiSelectEntry() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun batchAction() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    private fun lightTap() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
