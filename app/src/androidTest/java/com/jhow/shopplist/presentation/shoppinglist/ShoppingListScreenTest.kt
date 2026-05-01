package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jhow.shopplist.MainActivity
import com.jhow.shopplist.data.local.db.AppDatabase
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.di.DatabaseEntryPoint
import com.jhow.shopplist.domain.model.SyncStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private var database: AppDatabase? = null

    @Before
    fun setUp() {
        val context = composeRule.activity.applicationContext
        database = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java).database()
        runBlocking {
            database?.shoppingItemDao()?.replaceAll(sampleItems())
        }
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        runBlocking {
            database?.shoppingItemDao()?.deleteAll()
        }
        database = null
    }

    @Test
    fun tappingPendingItemMovesItToPurchasedImmediately() {
        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performClick()
        composeRule.waitForIdle()

        expandPurchasedSection()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-apples")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-apples")).fetchSemanticsNodes().isNotEmpty()
        )
        assertFalse(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.PURCHASE_SELECTED_BUTTON).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun clickingPurchasedItemRestoresItToPendingSection() {
        expandPurchasedSection()

        composeRule.onNodeWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun shoppingRowsUseCompactSingleLineHeight() {
        composeRule.waitForIdle()
        expandPurchasedSection()

        val maxCompactRowHeight = with(composeRule.density) { 64.dp.toPx() }
        val heightTolerance = with(composeRule.density) { 1.dp.toPx() }

        val pendingHeight = composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples"))
            .fetchSemanticsNode().boundsInRoot.height
        val purchasedHeight = composeRule.onNodeWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee"))
            .fetchSemanticsNode().boundsInRoot.height

        assertTrue(pendingHeight <= maxCompactRowHeight + heightTolerance)
        assertTrue(purchasedHeight <= maxCompactRowHeight + heightTolerance)
    }

    @Test
    fun longPressingPendingItemEntersSelectionModeWithContextualActions() {
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.PURCHASE_SELECTED_BUTTON).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.DELETE_SELECTED_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EXIT_SELECTION_BUTTON).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun rowsExposeLocalizedAccessibilityStatesAndActions() {
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "pending"))
        assertCustomAction(ShoppingListTestTags.pendingItem("pending-apples"), "Mark purchased")
        assertCustomAction(ShoppingListTestTags.pendingItem("pending-apples"), "Delete")

        expandPurchasedSection()

        composeRule.onNodeWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "purchased"))
        assertCustomAction(ShoppingListTestTags.purchasedItem("purchased-coffee"), "Restore to pending")
        assertCustomAction(ShoppingListTestTags.purchasedItem("purchased-coffee"), "Delete")
    }

    @Test
    fun selectionModeRowsExposeCheckboxSemantics() {
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "selected"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))

        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-bread"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "pending"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
    }

    @Test
    fun accessibilityMarkPurchasedActionPurchasesItemEvenWhenSelectionModeIsActive() {
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()

        performCustomAction(ShoppingListTestTags.pendingItem("pending-bread"), "Mark purchased")
        composeRule.waitForIdle()
        expandPurchasedSection()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-bread")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun tappingPendingItemWhileSelectionModeActiveTogglesSelectionInsteadOfPurchasing() {
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-bread")).performClick()
        composeRule.waitForIdle()

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-bread")).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-bread")).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun purchaseSelectedActionMarksAllSelectedItemsPurchased() {
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-bread")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.PURCHASE_SELECTED_BUTTON).performClick()
        composeRule.waitForIdle()

        expandPurchasedSection()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-apples")).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-bread")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun purchasedSectionIsCollapsedByDefaultWhenPendingItemsRemain() {
        composeRule.waitForIdle()

        assertTrue(composeRule.onAllNodesWithText("Pending items \u00b7 8").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Purchased history \u00b7 2").fetchSemanticsNodes().isNotEmpty())
        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun tappingPurchasedSectionHeaderExpandsCollapsedRows() {
        expandPurchasedSection()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun purchasedSectionExpandsByDefaultWhenNothingIsPending() {
        runBlocking {
            database?.shoppingItemDao()?.replaceAll(
                listOf(
                    ShoppingItemEntity(
                        id = "purchased-only",
                        name = "Coffee",
                        isPurchased = true,
                        purchaseCount = 1,
                        createdAt = 1L,
                        updatedAt = 1L,
                        isDeleted = false,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Pending items \u00b7 0").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Purchased history \u00b7 1").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-only")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-only")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun emptyListShowsMinimalPendingAndPurchasedEmptyStates() {
        runBlocking {
            database?.shoppingItemDao()?.replaceAll(emptyList())
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EMPTY_PENDING_ICON).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.EMPTY_PENDING_TEXT).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.EMPTY_PURCHASED_ICON).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.EMPTY_PURCHASED_TEXT).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Add your first item from the input bar.").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Purchased items will appear here.").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Nothing pending yet").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("No purchase history").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun deleteSelectedActionRemovesAllSelectedItems() {
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-bread")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.DELETE_SELECTED_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-bread")).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun addingItemFromInputShowsItInPendingList() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("Yogurt")
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performImeAction()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Yogurt").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Yogurt").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun tappingSubmitIconAddsCurrentInputToPendingList() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("Yogurt")
        composeRule.onNodeWithTag(ShoppingListTestTags.SUBMIT_INPUT_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Yogurt").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Yogurt").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun submitIconIsDisabledUntilInputHasText() {
        composeRule.onNodeWithTag(ShoppingListTestTags.SUBMIT_INPUT_BUTTON).assertIsNotEnabled()

        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("Yogurt")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.SUBMIT_INPUT_BUTTON).assertIsEnabled()
    }

    @Test
    fun addingItemFromInputKeepsInputFocusedForContinuousEntry() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("Yogurt")
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performImeAction()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Yogurt").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).assertIsFocused()
    }

    @Test
    fun typingShowsSuggestionDropdownWithMatchingItems() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("co")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SUGGESTION_LIST).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SUGGESTION_LIST).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Coffee")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun typingAccentlessQueryShowsAccentedSuggestion() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("cafe")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Café")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Café")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun typingFuzzyQueryShowsSubsequenceSuggestion() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("hme")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Home")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Home")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun overflowingSuggestionListKeepsTopMatchesVisibleNearestTheKeyboard() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("co")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Coffee")).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Cocoa")).fetchSemanticsNodes().isNotEmpty()
        }

        val suggestionListBottom = composeRule.onNodeWithTag(ShoppingListTestTags.SUGGESTION_LIST).fetchSemanticsNode().boundsInRoot.bottom
        val coffeeBottom = composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Coffee")).fetchSemanticsNode().boundsInRoot.bottom
        val coffeeTop = composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Coffee")).fetchSemanticsNode().boundsInRoot.top
        val cocoaTop = composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Cocoa")).fetchSemanticsNode().boundsInRoot.top

        assertTrue(suggestionListBottom - coffeeBottom < 8f)
        assertTrue(coffeeTop > cocoaTop)
    }

    @Test
    fun tappingSuggestionReclaimsTheItem() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("co")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Coffee")).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun tappingSuggestionDismissesInputFocus() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("co")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Coffee")).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).assertIsNotFocused()
    }

    @Test
    fun longPressingSuggestionKeepsInputFocusedForContinuousEntry() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("co")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Coffee")).performTouchInput {
            longClick()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).assertIsFocused()
    }

    @Test
    fun duplicatePendingSuggestionSelectionDoesNotCreateDuplicate() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("ap")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Apples")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Apples")).performClick()

        composeRule.waitForIdle()

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().size == 1
        )
    }

    @Test
    fun accentInsensitiveSuggestionSelectionReclaimsTheItem() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("cafe")
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.suggestionItem("Café")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.suggestionItem("Café")).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-cafe")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-cafe")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun swipingPendingItemRemovesItAndShowsUndoSnackbar() {
        composeRule.onNodeWithTag(ShoppingListTestTags.swipePendingItem("pending-apples")).performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Item deleted").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().isEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty())
        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.DELETE_ITEM_DIALOG).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun swipingPurchasedItemRemovesItAndShowsUndoSnackbar() {
        expandPurchasedSection()

        composeRule.onNodeWithTag(ShoppingListTestTags.swipePurchasedItem("purchased-coffee")).performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Item deleted").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).fetchSemanticsNodes().isEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun swipingPendingItemAndTappingUndoRestoresIt() {
        val initialLeft = composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-bread"))
            .fetchSemanticsNode().boundsInRoot.left

        composeRule.onNodeWithTag(ShoppingListTestTags.swipePendingItem("pending-bread")).performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Item deleted").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-bread")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitForIdle()

        val restoredLeft = composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-bread"))
            .fetchSemanticsNode().boundsInRoot.left
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-bread")).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(abs(restoredLeft - initialLeft) <= tolerance)
    }

    @Test
    fun deletingMultipleItemsCoalescesUndoSnackbar() {
        composeRule.onNodeWithTag(ShoppingListTestTags.swipePendingItem("pending-apples")).performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Item deleted").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(ShoppingListTestTags.swipePendingItem("pending-bread")).performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("2 items deleted").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty())
    }

    private fun expandPurchasedSection() {
        composeRule.onNodeWithTag(ShoppingListTestTags.PURCHASED_SECTION).performClick()
        composeRule.waitForIdle()
    }

    private fun assertCustomAction(tag: String, label: String) {
        val actions = composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        assertTrue(actions.any { action -> action.label == label })
    }

    private fun performCustomAction(tag: String, label: String) {
        val action = composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .first { action -> action.label == label }
        composeRule.runOnIdle {
            assertTrue(action.action())
        }
    }



    private fun sampleItems(): List<ShoppingItemEntity> = listOf(
        ShoppingItemEntity(
            id = "pending-apples",
            name = "Apples",
            isPurchased = false,
            purchaseCount = 4,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "pending-bread",
            name = "Bread",
            isPurchased = false,
            purchaseCount = 2,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "purchased-coffee",
            name = "Coffee",
            isPurchased = true,
            purchaseCount = 7,
            createdAt = 1L,
            updatedAt = 2L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "pending-cocoa",
            name = "Cocoa",
            isPurchased = false,
            purchaseCount = 2,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "pending-coconut-milk",
            name = "Coconut Milk",
            isPurchased = false,
            purchaseCount = 3,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "pending-cookie-butter",
            name = "Cookie Butter",
            isPurchased = false,
            purchaseCount = 5,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "pending-cola",
            name = "Cola",
            isPurchased = false,
            purchaseCount = 1,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "pending-cornflakes",
            name = "Cornflakes",
            isPurchased = false,
            purchaseCount = 1,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "pending-home",
            name = "Home",
            isPurchased = false,
            purchaseCount = 6,
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        ),
        ShoppingItemEntity(
            id = "purchased-cafe",
            name = "Café",
            isPurchased = true,
            purchaseCount = 4,
            createdAt = 1L,
            updatedAt = 2L,
            isDeleted = false,
            syncStatus = SyncStatus.SYNCED
        )
    )
}
