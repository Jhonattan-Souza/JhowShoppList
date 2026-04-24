package com.jhow.shopplist.presentation.shoppinglist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListSyncMenuTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun syncNowMenuItemInvokesCallback() {
        var syncNowClicked = false
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncMenuExpanded = true
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks(
                    onSyncNowRequested = { syncNowClicked = true }
                )
            )
        }
        composeRule.waitForIdle()

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_NOW_MENU_ITEM).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_NOW_MENU_ITEM).performClick()
        assertTrue("Sync now callback should fire", syncNowClicked)
    }
}
