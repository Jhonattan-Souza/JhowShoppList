package com.jhow.shopplist.presentation.shoppinglist

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListPullRefreshTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun topAppBar_showsSettingsGearIcon_withTestTag() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_SETTINGS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun clickingSettingsGear_invokesOnSyncSettingsClicked() {
        var settingsClicked = false
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(),
                snackbarHostState = SnackbarHostState(),
                syncCallbacks = ShoppingListSyncCallbacks(
                    onSyncSettingsClicked = { settingsClicked = true }
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_SETTINGS_BUTTON).performClick()

        assertTrue("Sync settings callback should fire", settingsClicked)
    }

    @Test
    fun pullDownGesture_invokesOnPullToRefresh() {
        var pullToRefreshInvoked = false
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(),
                snackbarHostState = SnackbarHostState(),
                syncCallbacks = ShoppingListSyncCallbacks(
                    onPullToRefresh = { pullToRefreshInvoked = true }
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.PULL_REFRESH_INDICATOR)
            .performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertTrue("Pull-to-refresh callback should fire", pullToRefreshInvoked)
    }

    @Test
    fun pullToRefreshSpinner_visible_whenIsSyncingTrue() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(isSyncing = true),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.PULL_REFRESH_SPINNER).assertIsDisplayed()
    }

    @Test
    fun pullToRefreshSpinner_notShown_whenIsSyncingFalse() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(isSyncing = false),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.PULL_REFRESH_SPINNER).assertDoesNotExist()
    }
}
