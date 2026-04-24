package com.jhow.shopplist.presentation.shoppinglist

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListSyncSettingsSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun syncSettingsSheetShowsHumanReadableSyncState() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncSettingsVisible = true,
                    syncSettings = ShoppingListSyncSettingsUiState(
                        syncState = CalDavSyncState.AuthError
                    )
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks()
            )
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_SETTINGS_SHEET).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Status: Authentication error").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Status: AuthError").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun syncSettingsSheetContentIsScrollable() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncSettingsVisible = true,
                    syncSettings = ShoppingListSyncSettingsUiState()
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks()
            )
        }

        val nodes = composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_SETTINGS_SHEET_CONTENT)
            .fetchSemanticsNodes()
        assertTrue(nodes.isNotEmpty())
        val hasScrollAction = nodes.any { node ->
            node.config.contains(SemanticsActions.ScrollBy)
        }
        assertTrue(hasScrollAction)
    }

    @Test
    fun syncSettingsPasswordFieldIsMasked() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncSettingsVisible = true,
                    syncSettings = ShoppingListSyncSettingsUiState(password = "secret")
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks()
            )
        }

        val passwordNodes = composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_PASSWORD_FIELD)
            .fetchSemanticsNodes()
        assertTrue(passwordNodes.isNotEmpty())

        val node = passwordNodes.first()
        assertTrue(
            "Password field should declare Password semantics",
            node.config.contains(SemanticsProperties.Password)
        )
        val displayedText = node.config[SemanticsProperties.EditableText].text
        assertEquals(
            "Password should be visually masked",
            "\u2022".repeat(6),
            displayedText
        )
    }

    @Test
    fun syncSettingsSheetPreservesUserEditsAcrossSyncStateUpdates() {
        var uiState by mutableStateOf(
            ShoppingListUiState(
                isSyncSettingsVisible = true,
                syncSettings = ShoppingListSyncSettingsUiState(
                    serverUrl = "https://initial.com",
                    syncState = CalDavSyncState.Idle
                )
            )
        )

        composeRule.setContent {
            ShoppingListScreen(
                uiState = uiState,
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks(
                    onSyncServerUrlChanged = { url ->
                        uiState = uiState.copy(
                            syncSettings = uiState.syncSettings.copy(serverUrl = url)
                        )
                    }
                )
            )
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_SERVER_FIELD)
            .performTextClearance()
        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_SERVER_FIELD)
            .performTextInput("modified")
        composeRule.waitForIdle()

        uiState = uiState.copy(
            syncSettings = uiState.syncSettings.copy(
                syncState = CalDavSyncState.Success
            )
        )
        composeRule.waitForIdle()

        val serverNodes = composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_SERVER_FIELD)
            .fetchSemanticsNodes()
        assertTrue(serverNodes.isNotEmpty())
        assertEquals(
            "User-edited server URL should survive sync state update",
            "modified",
            serverNodes.first().config[SemanticsProperties.EditableText].text
        )
        assertTrue(
            composeRule.onAllNodesWithText("Status: Success").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun syncSettingsSheetRendersStatusMessage() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncSettingsVisible = true,
                    syncSettings = ShoppingListSyncSettingsUiState(
                        statusMessage = "Connection timed out"
                    )
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks()
            )
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_STATUS_TEXT).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithText("Connection timed out").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun syncSettingsSheetShowsStoredPasswordPlaceholderWithoutRawPassword() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncSettingsVisible = true,
                    syncSettings = ShoppingListSyncSettingsUiState(hasStoredPassword = true)
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks()
            )
        }

        val passwordNode = composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_PASSWORD_FIELD)
            .fetchSemanticsNode()
        val editableText = if (passwordNode.config.contains(SemanticsProperties.EditableText)) {
            passwordNode.config[SemanticsProperties.EditableText].text
        } else {
            null
        }
        assertTrue("Raw password should not be exposed", editableText.isNullOrEmpty())

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_PASSWORD_FIELD).performClick()
        composeRule.waitForIdle()
        assertTrue(
            "Password field should show stored-password placeholder",
            composeRule.onAllNodesWithText("Saved password on device", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
    }

    @Test
    fun syncSettingsSheetShowsInlineProgressAndCreateListAction() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncSettingsVisible = true,
                    syncSettings = ShoppingListSyncSettingsUiState(
                        isSaving = true,
                        statusMessage = "Remote list Groceries does not exist yet",
                        pendingAction = CalDavPendingAction.CreateMissingList
                    )
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks()
            )
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_PROGRESS_INDICATOR).assertExists()
        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_CREATE_LIST_BUTTON).assertExists()
    }

    @Test
    fun syncSettingsSheetShowsCreateMissingListButton() {
        var createClicked = false
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    isSyncSettingsVisible = true,
                    syncSettings = ShoppingListSyncSettingsUiState(
                        pendingAction = CalDavPendingAction.CreateMissingList
                    )
                ),
                inputCallbacks = ShoppingListInputCallbacks(),
                itemCallbacks = ShoppingListItemCallbacks(),
                syncCallbacks = ShoppingListSyncCallbacks(
                    onConfirmCreateMissingList = { createClicked = true }
                )
            )
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SYNC_CREATE_LIST_BUTTON).fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag(ShoppingListTestTags.SYNC_CREATE_LIST_BUTTON).performClick()
        assertTrue("Create missing list callback should fire", createClicked)
    }
}
