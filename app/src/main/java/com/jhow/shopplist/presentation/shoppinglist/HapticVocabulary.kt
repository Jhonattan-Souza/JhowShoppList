package com.jhow.shopplist.presentation.shoppinglist

import android.os.Build
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class HapticVocabulary(private val hapticFeedback: HapticFeedback) {
    fun add() = lightTap()

    fun purchaseTap() = lightTap()

    fun restore() = lightTap()

    fun swipeThreshold() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        } else {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
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
