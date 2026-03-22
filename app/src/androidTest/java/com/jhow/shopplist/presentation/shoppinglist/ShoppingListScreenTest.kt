package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jhow.shopplist.MainActivity
import com.jhow.shopplist.data.local.db.AppDatabase
import com.jhow.shopplist.data.local.entity.ShoppingItemEntity
import com.jhow.shopplist.di.DatabaseEntryPoint
import com.jhow.shopplist.domain.model.SyncStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
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
    fun selectingPendingItemShowsFabAndMovesItemToPurchased() {
        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().isNotEmpty()
        )

        composeRule.onNodeWithTag(ShoppingListTestTags.pendingItem("pending-apples")).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.PURCHASE_SELECTED_FAB).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-apples")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("pending-apples")).fetchSemanticsNodes().isNotEmpty()
        )
        assertFalse(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.PURCHASE_SELECTED_FAB).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun clickingPurchasedItemRestoresItToPendingSection() {
        composeRule.onNodeWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("purchased-coffee")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun addingItemFromInputShowsItInPendingList() {
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performTextInput("Yogurt")
        composeRule.onNodeWithTag(ShoppingListTestTags.INPUT_FIELD).performImeAction()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Yogurt").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(composeRule.onAllNodesWithText("Yogurt").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun deletingPendingItemRemovesItFromVisibleLists() {
        composeRule.onNodeWithTag(ShoppingListTestTags.deletePendingItem("pending-apples")).performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().isEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.pendingItem("pending-apples")).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun deletingPurchasedItemRemovesItFromHistory() {
        composeRule.onNodeWithTag(ShoppingListTestTags.deletePurchasedItem("purchased-coffee")).performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).fetchSemanticsNodes().isEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(ShoppingListTestTags.purchasedItem("purchased-coffee")).fetchSemanticsNodes().isEmpty()
        )
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
        )
    )
}
