package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jhow.shopplist.R
import com.jhow.shopplist.domain.model.ShoppingItem

@Composable
fun ShoppingListRoute(
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingListScreen(
        uiState = uiState.value,
        onInputValueChange = viewModel::onInputValueChange,
        onAddItem = viewModel::onAddItem,
        onPendingItemClick = viewModel::onPendingItemClicked,
        onPurchasedItemClick = viewModel::onPurchasedItemClicked,
        onPurchaseSelectedItems = viewModel::onPurchaseSelectedItems
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    onInputValueChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onPendingItemClick: (String) -> Unit,
    onPurchasedItemClick: (String) -> Unit,
    onPurchaseSelectedItems: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag(ShoppingListTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = stringResource(R.string.shopping_list_title))
                        Text(
                            text = stringResource(R.string.shopping_list_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            ShoppingInputBar(
                value = uiState.inputValue,
                onValueChange = onInputValueChange,
                onDone = onAddItem
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.isBulkActionVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onPurchaseSelectedItems,
                    modifier = Modifier.testTag(ShoppingListTestTags.PURCHASE_SELECTED_FAB)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.purchase_selected_items)
                    )
                }
            }
        }
    ) { innerPadding ->
        ShoppingItemsContent(
            uiState = uiState,
            onPendingItemClick = onPendingItemClick,
            onPurchasedItemClick = onPurchasedItemClick,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun ShoppingInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ShoppingListTestTags.INPUT_FIELD),
            label = { Text(text = stringResource(R.string.add_item_label)) },
            placeholder = { Text(text = stringResource(R.string.add_item_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AddTask,
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun ShoppingItemsContent(
    uiState: ShoppingListUiState,
    onPendingItemClick: (String) -> Unit,
    onPurchasedItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = ShoppingListTestTags.PENDING_SECTION) {
            SectionHeader(
                title = stringResource(R.string.pending_items_title),
                subtitle = stringResource(R.string.pending_items_subtitle),
                modifier = Modifier.testTag(ShoppingListTestTags.PENDING_SECTION)
            )
        }

        if (uiState.pendingItems.isEmpty()) {
            item(key = ShoppingListTestTags.EMPTY_STATE) {
                EmptyStateCard(
                    title = stringResource(R.string.empty_pending_title),
                    subtitle = stringResource(R.string.empty_pending_subtitle),
                    modifier = Modifier.testTag(ShoppingListTestTags.EMPTY_STATE)
                )
            }
        } else {
            items(
                items = uiState.pendingItems,
                key = { item -> item.id }
            ) { item ->
                PendingItemRow(
                    item = item,
                    isSelected = item.id in uiState.selectedIds,
                    onClick = { onPendingItemClick(item.id) }
                )
            }
        }

        item(key = ShoppingListTestTags.SECTION_DIVIDER) {
            HorizontalDivider(modifier = Modifier.testTag(ShoppingListTestTags.SECTION_DIVIDER))
        }

        item(key = ShoppingListTestTags.PURCHASED_SECTION) {
            SectionHeader(
                title = stringResource(R.string.purchased_items_title),
                subtitle = stringResource(R.string.purchased_items_subtitle),
                modifier = Modifier.testTag(ShoppingListTestTags.PURCHASED_SECTION)
            )
        }

        if (uiState.purchasedItems.isEmpty()) {
            item(key = "purchased_empty") {
                EmptyStateCard(
                    title = stringResource(R.string.empty_purchased_title),
                    subtitle = stringResource(R.string.empty_purchased_subtitle)
                )
            }
        } else {
            items(
                items = uiState.purchasedItems,
                key = { item -> item.id }
            ) { item ->
                PurchasedItemRow(
                    item = item,
                    onClick = { onPurchasedItemClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PendingItemRow(
    item: ShoppingItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    ListItem(
        headlineContent = {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = if (item.purchaseCount == 0) {
                    stringResource(R.string.never_purchased_label)
                } else {
                    stringResource(R.string.purchased_count_label, item.purchaseCount)
                }
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics { contentDescription = item.name }
            .testTag(ShoppingListTestTags.pendingItem(item.id))
    )
}

@Composable
private fun PurchasedItemRow(
    item: ShoppingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.name,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(text = stringResource(R.string.restore_to_pending_label))
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.Restore,
                contentDescription = null
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.6f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .semantics { contentDescription = item.name }
            .testTag(ShoppingListTestTags.purchasedItem(item.id))
    )
}
