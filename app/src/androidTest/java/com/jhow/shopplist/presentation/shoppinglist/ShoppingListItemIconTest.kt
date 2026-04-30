package com.jhow.shopplist.presentation.shoppinglist

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jhow.shopplist.domain.icon.IconBucket
import com.jhow.shopplist.domain.icon.IconMatcher
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.SyncStatus
import com.jhow.shopplist.presentation.icon.IconResolver
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListItemIconTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val fakeIconResolver = IconResolver(
        object : IconMatcher {
            override fun match(itemName: String): IconBucket =
                if (itemName.equals("milk", ignoreCase = true)) {
                    IconBucket.DAIRY
                } else {
                    IconBucket.GENERIC
                }

            override fun updateDictionary(newDictionary: Map<String, IconBucket>): Boolean = false
        }
    )

    @Test
    fun pendingRowRendersIcon() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    pendingItems = listOf(sampleItem("1", "Milk")),
                    selectedIds = emptySet()
                ),
                snackbarHostState = SnackbarHostState(),
                iconResolver = fakeIconResolver
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("1")).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM_ICON, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun purchasedRowRendersIcon() {
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    purchasedItems = listOf(sampleItem("2", "Bread", isPurchased = true))
                ),
                snackbarHostState = SnackbarHostState(),
                iconResolver = fakeIconResolver
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.purchasedItem("2")).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM_ICON, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun clickingPendingRowStillTogglesSelection() {
        var clicked = false
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    pendingItems = listOf(sampleItem("3", "Milk"))
                ),
                snackbarHostState = SnackbarHostState(),
                itemCallbacks = ShoppingListItemCallbacks(
                    onPendingItemClick = { clicked = true }
                ),
                iconResolver = fakeIconResolver
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("3")).performClick()
        composeRule.waitForIdle()

        assertTrue("Pending item click callback should fire", clicked)
    }

    @Test
    fun clickingPurchasedRowStillRestoresItem() {
        var clicked = false
        composeRule.setContent {
            ShoppingListScreen(
                uiState = ShoppingListUiState(
                    purchasedItems = listOf(sampleItem("4", "Bread", isPurchased = true))
                ),
                snackbarHostState = SnackbarHostState(),
                itemCallbacks = ShoppingListItemCallbacks(
                    onPurchasedItemClick = { clicked = true }
                ),
                iconResolver = fakeIconResolver
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ShoppingListTestTags.purchasedItem("4")).performClick()
        composeRule.waitForIdle()

        assertTrue("Purchased item click callback should fire", clicked)
    }

    private fun sampleItem(
        id: String,
        name: String,
        isPurchased: Boolean = false
    ): ShoppingItem = ShoppingItem(
        id = id,
        name = name,
        isPurchased = isPurchased,
        purchaseCount = 0,
        createdAt = 0L,
        updatedAt = 0L,
        isDeleted = false,
        syncStatus = SyncStatus.SYNCED
    )
}
