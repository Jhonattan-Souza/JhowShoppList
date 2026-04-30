package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jhow.shopplist.R
import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.presentation.icon.IconResolver

class ShoppingListInputCallbacks(
    val onValueChange: (String) -> Unit = {},
    val onAddItem: () -> Unit = {},
    val onSuggestionSelected: (String) -> Unit = {}
)

class ShoppingListItemCallbacks(
    val onPendingItemClick: (String) -> Unit = {},
    val onPurchasedItemClick: (String) -> Unit = {},
    val onPurchaseSelectedItems: () -> Unit = {},
    val onDeleteItemRequested: (ShoppingItem) -> Unit = {},
    val onDeleteItemDismissed: () -> Unit = {},
    val onDeleteItemConfirmed: () -> Unit = {}
)

class ShoppingListSyncCallbacks(
    val onManualSyncRequested: () -> Unit = {},
    val onSyncSettingsClicked: () -> Unit = {}
)

private data class ShoppingItemRowVisuals(
    val leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val textDecoration: TextDecoration? = null,
    val rowAlpha: Float = 1f
)

private data class ShoppingItemRowInteractions(
    val onClick: () -> Unit,
    val onDeleteRequested: () -> Unit,
    val swipeTag: String,
    val swipeResetTrigger: String
)

private data class ShoppingListContentLayout(
    val innerPadding: PaddingValues,
    val inputBarHeight: Dp,
    val bulkFabBottomClearance: Dp
)

internal val shoppingListWideSurfaceShape: Shape = RoundedCornerShape(percent = 50)

internal enum class ShoppingListColorRole {
    PrimaryContainer,
    OnPrimaryContainer,
    SecondaryContainer,
    OnSecondaryContainer,
    SurfaceContainerLow,
    OnSurface
}

internal data class PendingItemRowColorRoles(
    val container: ShoppingListColorRole,
    val content: ShoppingListColorRole
)

internal fun pendingItemRowColorRoles(isSelected: Boolean): PendingItemRowColorRoles =
    if (isSelected) {
        PendingItemRowColorRoles(
            container = ShoppingListColorRole.PrimaryContainer,
            content = ShoppingListColorRole.OnPrimaryContainer
        )
    } else {
        PendingItemRowColorRoles(
            container = ShoppingListColorRole.SurfaceContainerLow,
            content = ShoppingListColorRole.OnSurface
        )
    }

@Composable
private fun ShoppingListColorRole.resolve() = when (this) {
    ShoppingListColorRole.PrimaryContainer -> MaterialTheme.colorScheme.primaryContainer
    ShoppingListColorRole.OnPrimaryContainer -> MaterialTheme.colorScheme.onPrimaryContainer
    ShoppingListColorRole.SecondaryContainer -> MaterialTheme.colorScheme.secondaryContainer
    ShoppingListColorRole.OnSecondaryContainer -> MaterialTheme.colorScheme.onSecondaryContainer
    ShoppingListColorRole.SurfaceContainerLow -> MaterialTheme.colorScheme.surfaceContainerLow
    ShoppingListColorRole.OnSurface -> MaterialTheme.colorScheme.onSurface
}

@Composable
fun ShoppingListRoute(
    onNavigateToCalDavConfig: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel(),
    iconResolver: IconResolver
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val inputCallbacks = remember(viewModel) {
        ShoppingListInputCallbacks(
            onValueChange = viewModel::onInputValueChange,
            onAddItem = viewModel::onAddItem,
            onSuggestionSelected = viewModel::onSuggestionSelected
        )
    }
    val itemCallbacks = remember(viewModel) {
        ShoppingListItemCallbacks(
            onPendingItemClick = viewModel::onPendingItemClicked,
            onPurchasedItemClick = viewModel::onPurchasedItemClicked,
            onPurchaseSelectedItems = viewModel::onPurchaseSelectedItems,
            onDeleteItemRequested = viewModel::onDeleteItemRequested,
            onDeleteItemDismissed = viewModel::onDeleteItemDismissed,
            onDeleteItemConfirmed = viewModel::onDeleteItemConfirmed
        )
    }
    val syncCallbacks = remember(viewModel) {
        ShoppingListSyncCallbacks(
            onManualSyncRequested = viewModel::onManualSyncRequested,
            onSyncSettingsClicked = onNavigateToCalDavConfig
        )
    }
    ShoppingListScreen(
        uiState = uiState.value,
        snackbarHostState = snackbarHostState,
        inputCallbacks = inputCallbacks,
        itemCallbacks = itemCallbacks,
        syncCallbacks = syncCallbacks,
        iconResolver = iconResolver
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    inputCallbacks: ShoppingListInputCallbacks = ShoppingListInputCallbacks(),
    itemCallbacks: ShoppingListItemCallbacks = ShoppingListItemCallbacks(),
    syncCallbacks: ShoppingListSyncCallbacks = ShoppingListSyncCallbacks(),
    iconResolver: IconResolver
) {
    val focusManager = LocalFocusManager.current
    var inputBarContentHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bulkFabBottomClearance = with(density) {
        if (inputBarContentHeightPx > 0) inputBarContentHeightPx.toDp() + 16.dp else 88.dp
    }

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(ShoppingListTestTags.SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ShoppingListTopAppBar(
                uiState = uiState,
                syncCallbacks = syncCallbacks
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        ShoppingListScreenContent(
            uiState = uiState,
            layout = ShoppingListContentLayout(
                innerPadding = innerPadding,
                inputBarHeight = with(density) {
                    if (inputBarContentHeightPx > 0) inputBarContentHeightPx.toDp() else 0.dp
                },
                bulkFabBottomClearance = bulkFabBottomClearance
            ),
            inputCallbacks = inputCallbacks,
            itemCallbacks = itemCallbacks,
            onInputBarHeightChanged = { inputBarContentHeightPx = it },
            iconResolver = iconResolver
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListScreenContent(
    uiState: ShoppingListUiState,
    layout: ShoppingListContentLayout,
    inputCallbacks: ShoppingListInputCallbacks,
    itemCallbacks: ShoppingListItemCallbacks,
    onInputBarHeightChanged: (Int) -> Unit,
    iconResolver: IconResolver
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(layout.innerPadding)
    ) {
        ShoppingItemsContent(
            uiState = uiState,
            itemCallbacks = itemCallbacks,
            modifier = Modifier.fillMaxSize(),
            iconResolver = iconResolver
        )

        if (uiState.isManualSync) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = layout.inputBarHeight)
                    .size(48.dp)
                    .testTag(ShoppingListTestTags.MANUAL_SYNC_LOADER)
            )
        }

        ShoppingInputBar(
            value = uiState.inputValue,
            suggestions = uiState.suggestions,
            inputCallbacks = inputCallbacks,
            onContentHeightChanged = onInputBarHeightChanged,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        BulkPurchaseFab(
            visible = uiState.isBulkActionVisible,
            onClick = itemCallbacks.onPurchaseSelectedItems,
            bottomClearance = layout.bulkFabBottomClearance,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }

    DeleteItemDialog(
        item = uiState.itemPendingDeletion,
        onDismiss = itemCallbacks.onDeleteItemDismissed,
        onConfirm = itemCallbacks.onDeleteItemConfirmed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListTopAppBar(
    uiState: ShoppingListUiState,
    syncCallbacks: ShoppingListSyncCallbacks
) {
    val syncNowLabel = stringResource(R.string.sync_now)

    TopAppBar(
        title = {},
        navigationIcon = {
            Icon(
                imageVector = Icons.Rounded.ShoppingBag,
                contentDescription = stringResource(R.string.shopping_list_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(28.dp)
                    .testTag(ShoppingListTestTags.BRAND_ICON)
            )
        },
        actions = {
            if (uiState.isSyncConfigured) {
                IconButton(
                    onClick = syncCallbacks.onManualSyncRequested,
                    modifier = Modifier
                        .testTag(ShoppingListTestTags.SYNC_BADGE)
                        .semantics { contentDescription = syncNowLabel }
                ) {
                    if (uiState.isManualSync || uiState.isBackgroundSync) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .testTag(ShoppingListTestTags.SYNC_BADGE_SPINNER),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null
                        )
                    }
                }
            }
            IconButton(
                onClick = syncCallbacks.onSyncSettingsClicked,
                modifier = Modifier.testTag(ShoppingListTestTags.SYNC_SETTINGS_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.sync_settings)
                )
            }
        }
    )
}



@Composable
private fun ShoppingInputBar(
    value: String,
    suggestions: List<String>,
    inputCallbacks: ShoppingListInputCallbacks,
    onContentHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun restoreContinuousEntry() {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .onSizeChanged { onContentHeightChanged(it.height) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ShoppingInputField(
                value = value,
                inputCallbacks = inputCallbacks,
                focusRequester = focusRequester,
                onContinuousEntryRequested = ::restoreContinuousEntry
            )
            ShoppingSuggestionsList(
                suggestions = suggestions,
                onSuggestionSelected = { suggestion ->
                    inputCallbacks.onSuggestionSelected(suggestion)
                    restoreContinuousEntry()
                }
            )
        }
    }
}

@Composable
private fun ShoppingInputField(
    value: String,
    inputCallbacks: ShoppingListInputCallbacks,
    focusRequester: FocusRequester,
    onContinuousEntryRequested: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = inputCallbacks.onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag(ShoppingListTestTags.INPUT_FIELD),
        label = { Text(text = stringResource(R.string.add_item_label)) },
        placeholder = { Text(text = stringResource(R.string.add_item_placeholder)) },
        shape = shoppingListWideSurfaceShape,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                inputCallbacks.onAddItem()
                onContinuousEntryRequested()
            }
        ),
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.AddTask,
                contentDescription = null
            )
        }
    )
}

@Composable
private fun ShoppingSuggestionsList(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit
) {
    AnimatedVisibility(visible = suggestions.isNotEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .heightIn(max = 176.dp)
                .testTag(ShoppingListTestTags.SUGGESTION_LIST),
            shape = shoppingListWideSurfaceShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            LazyColumn(reverseLayout = true) {
                suggestions.forEachIndexed { index, suggestion ->
                    item(key = suggestion) {
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable { onSuggestionSelected(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag(ShoppingListTestTags.suggestionItem(suggestion))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkPurchaseFab(
    visible: Boolean,
    onClick: () -> Unit,
    bottomClearance: Dp,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(end = 16.dp, bottom = bottomClearance)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.testTag(ShoppingListTestTags.PURCHASE_SELECTED_FAB),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.purchase_selected_items)
            )
        }
    }
}

@Composable
private fun ShoppingItemsContent(
    uiState: ShoppingListUiState,
    itemCallbacks: ShoppingListItemCallbacks,
    modifier: Modifier = Modifier,
    iconResolver: IconResolver
) {
    val swipeResetTrigger = uiState.itemPendingDeletion?.id.orEmpty()
    val emptyPendingTitle = stringResource(R.string.empty_pending_title)
    val emptyPurchasedTitle = stringResource(R.string.empty_purchased_title)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = ShoppingListTestTags.PENDING_SECTION) {
            SectionHeader(
                title = stringResource(R.string.pending_items_title),
                modifier = Modifier.testTag(ShoppingListTestTags.PENDING_SECTION)
            )
        }

        pendingItemsSection(
            pendingItems = uiState.pendingItems,
            selectedIds = uiState.selectedIds,
            swipeResetTrigger = swipeResetTrigger,
            itemCallbacks = itemCallbacks,
            emptyPendingTitle = emptyPendingTitle,
            iconResolver = iconResolver
        )

        item(key = ShoppingListTestTags.SECTION_DIVIDER) {
            HorizontalDivider(modifier = Modifier.testTag(ShoppingListTestTags.SECTION_DIVIDER))
        }

        item(key = ShoppingListTestTags.PURCHASED_SECTION) {
            SectionHeader(
                title = stringResource(R.string.purchased_items_title),
                modifier = Modifier.testTag(ShoppingListTestTags.PURCHASED_SECTION)
            )
        }

        purchasedItemsSection(
            purchasedItems = uiState.purchasedItems,
            swipeResetTrigger = swipeResetTrigger,
            itemCallbacks = itemCallbacks,
            emptyPurchasedTitle = emptyPurchasedTitle,
            iconResolver = iconResolver
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pendingItemsSection(
    pendingItems: List<ShoppingItem>,
    selectedIds: Set<String>,
    swipeResetTrigger: String,
    itemCallbacks: ShoppingListItemCallbacks,
    emptyPendingTitle: String,
    iconResolver: IconResolver
) {
    if (pendingItems.isEmpty()) {
        item(key = ShoppingListTestTags.EMPTY_STATE) {
            EmptyStateCard(
                title = emptyPendingTitle,
                modifier = Modifier.testTag(ShoppingListTestTags.EMPTY_STATE)
            )
        }
        return
    }

        items(
        items = pendingItems,
        key = { item -> item.id }
    ) { item ->
        PendingItemRow(
            item = item,
            isSelected = item.id in selectedIds,
            onClick = { itemCallbacks.onPendingItemClick(item.id) },
            onDeleteRequested = { itemCallbacks.onDeleteItemRequested(item) },
            swipeResetTrigger = swipeResetTrigger,
            iconResolver = iconResolver,
            modifier = Modifier.animateItem()
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.purchasedItemsSection(
    purchasedItems: List<ShoppingItem>,
    swipeResetTrigger: String,
    itemCallbacks: ShoppingListItemCallbacks,
    emptyPurchasedTitle: String,
    iconResolver: IconResolver
) {
    if (purchasedItems.isEmpty()) {
        item(key = "purchased_empty") {
            EmptyStateCard(
                title = emptyPurchasedTitle
            )
        }
        return
    }

    items(
        items = purchasedItems,
        key = { item -> item.id }
    ) { item ->
        PurchasedItemRow(
            item = item,
            onClick = { itemCallbacks.onPurchasedItemClick(item.id) },
            onDeleteRequested = { itemCallbacks.onDeleteItemRequested(item) },
            swipeResetTrigger = swipeResetTrigger,
            iconResolver = iconResolver,
            modifier = Modifier.animateItem()
        )
    }
}

@Composable
private fun DeleteItemDialog(
    item: ShoppingItem?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (item == null) return

    AlertDialog(
        modifier = Modifier.testTag(ShoppingListTestTags.DELETE_ITEM_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_item_title)) },
        text = { Text(text = stringResource(R.string.delete_item_message, item.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete_item_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.delete_item_cancel))
            }
        }
    )
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun EmptyStateCard(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shoppingListWideSurfaceShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PendingItemRow(
    item: ShoppingItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteRequested: () -> Unit,
    swipeResetTrigger: String,
    iconResolver: IconResolver,
    modifier: Modifier = Modifier
) {
    val colorRoles = pendingItemRowColorRoles(isSelected)
    val containerColor by animateColorAsState(
        targetValue = colorRoles.container.resolve(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pendingRowContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = colorRoles.content.resolve(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pendingRowContentColor"
    )

    val iconVersion = iconResolver.version
    val leadingIcon = remember(item.name, iconVersion) {
        iconResolver.resolveIcon(item.name)
    }

    ShoppingItemRow(
        name = item.name,
        visuals = ShoppingItemRowVisuals(
            leadingIcon = leadingIcon,
            containerColor = containerColor,
            contentColor = contentColor
        ),
        interactions = ShoppingItemRowInteractions(
            onClick = onClick,
            onDeleteRequested = onDeleteRequested,
            swipeTag = ShoppingListTestTags.swipePendingItem(item.id),
            swipeResetTrigger = swipeResetTrigger
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.name }
            .testTag(ShoppingListTestTags.pendingItem(item.id))
    )
}

@Composable
private fun PurchasedItemRow(
    item: ShoppingItem,
    onClick: () -> Unit,
    onDeleteRequested: () -> Unit,
    swipeResetTrigger: String,
    iconResolver: IconResolver,
    modifier: Modifier = Modifier
) {
    val iconVersion = iconResolver.version
    val leadingIcon = remember(item.name, iconVersion) {
        iconResolver.resolveIcon(item.name)
    }

    ShoppingItemRow(
        name = item.name,
        visuals = ShoppingItemRowVisuals(
            leadingIcon = leadingIcon,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            textDecoration = TextDecoration.LineThrough,
            rowAlpha = 0.6f
        ),
        interactions = ShoppingItemRowInteractions(
            onClick = onClick,
            onDeleteRequested = onDeleteRequested,
            swipeTag = ShoppingListTestTags.swipePurchasedItem(item.id),
            swipeResetTrigger = swipeResetTrigger
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.name }
            .testTag(ShoppingListTestTags.purchasedItem(item.id))
    )
}

@Composable
private fun ShoppingItemRow(
    name: String,
    visuals: ShoppingItemRowVisuals,
    interactions: ShoppingItemRowInteractions,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            interactions.onDeleteRequested()
            dismissState.reset()
        }
    }

    LaunchedEffect(interactions.swipeResetTrigger) {
        if (
            dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
                dismissState.targetValue != SwipeToDismissBoxValue.Settled
        ) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            SwipeDeleteBackground(
                label = stringResource(R.string.delete_item_swipe_label),
                isActive = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            )
        },
        modifier = Modifier.testTag(interactions.swipeTag)
    ) {
        Row(
            modifier = modifier
                .alpha(visuals.rowAlpha)
                .clip(shoppingListWideSurfaceShape)
                .background(visuals.containerColor)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp)
                .clickable(onClick = interactions.onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = visuals.leadingIcon,
                contentDescription = null,
                tint = visuals.contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .testTag(ShoppingListTestTags.ITEM_ICON)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = visuals.contentColor,
                textDecoration = visuals.textDecoration,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun SwipeDeleteBackground(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shoppingListWideSurfaceShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
