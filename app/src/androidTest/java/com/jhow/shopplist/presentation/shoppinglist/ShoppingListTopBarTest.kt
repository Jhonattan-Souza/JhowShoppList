package com.jhow.shopplist.presentation.shoppinglist

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListTopBarTest {
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
    fun topAppBar_showsBrandIcon() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.BRAND_ICON).assertIsDisplayed()
    }

    @Test
    fun syncBadge_hidden_whenSyncIsNotConfigured() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(isSyncConfigured = false),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_BADGE).assertCountEquals(0)
        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_SETTINGS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun syncBadge_visible_whenSyncConfiguredAndIdle() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(isSyncConfigured = true),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_BADGE).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sync now").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_BADGE_SPINNER).assertCountEquals(0)
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
    fun clickingSyncBadge_invokesOnManualSyncRequested() {
        var manualSyncInvoked = false
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(isSyncConfigured = true),
                snackbarHostState = SnackbarHostState(),
                syncCallbacks = ShoppingListSyncCallbacks(
                    onManualSyncRequested = { manualSyncInvoked = true }
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_BADGE).performClick()
        composeRule.waitForIdle()

        assertTrue("Sync badge callback should fire", manualSyncInvoked)
    }

    @Test
    fun syncBadgeSpinner_visible_whenManualSyncTrue() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(isSyncConfigured = true, isManualSync = true),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_BADGE_SPINNER).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.MANUAL_SYNC_LOADER).assertIsDisplayed()
    }

    @Test
    fun syncBadgeSpinner_visible_whenBackgroundSyncTrue_butManualLoaderHidden() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(isSyncConfigured = true, isBackgroundSync = true),
                snackbarHostState = SnackbarHostState()
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_BADGE_SPINNER).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sync now").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.MANUAL_SYNC_LOADER).assertCountEquals(0)
    }
}
