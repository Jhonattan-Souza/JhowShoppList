package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeNestedScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    val onPendingItemLongPress: (String) -> Unit = {},
    val onPurchasedItemClick: (String) -> Unit = {},
    val onPurchaseSelectedItems: () -> Unit = {},
    val onDeleteSelectedItems: () -> Unit = {},
    val onSelectionModeExited: () -> Unit = {},
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

private data class PendingItemRowCallbacks(
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
    val onDeleteRequested: () -> Unit
)

private data class ShoppingItemRowInteractions(
    val onClick: () -> Unit,
    val onLongClick: () -> Unit = {},
    val onDeleteRequested: () -> Unit,
    val swipeTag: String,
    val swipeResetTrigger: String
)

private data class ShoppingListContentLayout(
    val innerPadding: PaddingValues,
    val inputBarHeight: Dp
)

internal fun shoppingListBottomContentPadding(inputBarHeight: Dp): Dp = inputBarHeight

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
            onPendingItemLongPress = viewModel::onPendingItemLongPressed,
            onPurchasedItemClick = viewModel::onPurchasedItemClicked,
            onPurchaseSelectedItems = viewModel::onPurchaseSelectedItems,
            onDeleteSelectedItems = viewModel::onDeleteSelectedItems,
            onSelectionModeExited = viewModel::onSelectionModeExited,
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

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(ShoppingListTestTags.SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                ShoppingListTopAppBar(
                    uiState = uiState,
                    itemCallbacks = itemCallbacks,
                    syncCallbacks = syncCallbacks
                )
                if (uiState.isManualSync) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .testTag(ShoppingListTestTags.MANUAL_SYNC_PROGRESS_INDICATOR)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        ShoppingListScreenContent(
            uiState = uiState,
            layout = ShoppingListContentLayout(
                innerPadding = innerPadding,
                inputBarHeight = with(density) {
                    if (inputBarContentHeightPx > 0) inputBarContentHeightPx.toDp() else 0.dp
                }
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
            iconResolver = iconResolver,
            bottomContentPadding = shoppingListBottomContentPadding(layout.inputBarHeight)
        )
        ShoppingInputBar(
            value = uiState.inputValue,
            suggestions = uiState.suggestions,
            inputCallbacks = inputCallbacks,
            onContentHeightChanged = onInputBarHeightChanged,
            modifier = Modifier.align(Alignment.BottomCenter)
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
    itemCallbacks: ShoppingListItemCallbacks,
    syncCallbacks: ShoppingListSyncCallbacks
) {
    TopAppBar(
        title = {
            if (uiState.isSelectionMode) {
                Text(text = uiState.selectedIds.size.toString())
            }
        },
        navigationIcon = {
            ShoppingListNavigationIcon(
                isSelectionMode = uiState.isSelectionMode,
                onSelectionModeExited = itemCallbacks.onSelectionModeExited
            )
        },
        actions = {
            ShoppingListTopBarActions(
                uiState = uiState,
                itemCallbacks = itemCallbacks,
                syncCallbacks = syncCallbacks
            )
        }
    )
}

@Composable
private fun ShoppingListNavigationIcon(
    isSelectionMode: Boolean,
    onSelectionModeExited: () -> Unit
) {
    if (isSelectionMode) {
        val exitSelectionModeLabel = stringResource(R.string.exit_selection_mode)

        IconButton(
            onClick = onSelectionModeExited,
            modifier = Modifier
                .testTag(ShoppingListTestTags.EXIT_SELECTION_BUTTON)
                .semantics { contentDescription = exitSelectionModeLabel }
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null
            )
        }
        return
    }

    Icon(
        imageVector = Icons.Rounded.ShoppingBag,
        contentDescription = stringResource(R.string.shopping_list_title),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp)
            .size(28.dp)
            .testTag(ShoppingListTestTags.BRAND_ICON)
    )
}

@Composable
private fun ShoppingListTopBarActions(
    uiState: ShoppingListUiState,
    itemCallbacks: ShoppingListItemCallbacks,
    syncCallbacks: ShoppingListSyncCallbacks
) {
    if (uiState.isSelectionMode) {
        SelectionModeActions(itemCallbacks = itemCallbacks)
    } else {
        DefaultTopBarActions(
            uiState = uiState,
            syncCallbacks = syncCallbacks
        )
    }
}

@Composable
private fun SelectionModeActions(itemCallbacks: ShoppingListItemCallbacks) {
    val purchaseSelectedLabel = stringResource(R.string.purchase_selected_items)
    val deleteSelectedLabel = stringResource(R.string.delete_selected_items)

    IconButton(
        onClick = itemCallbacks.onPurchaseSelectedItems,
        modifier = Modifier
            .testTag(ShoppingListTestTags.PURCHASE_SELECTED_BUTTON)
            .semantics { contentDescription = purchaseSelectedLabel }
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null
        )
    }
    IconButton(
        onClick = itemCallbacks.onDeleteSelectedItems,
        modifier = Modifier
            .testTag(ShoppingListTestTags.DELETE_SELECTED_BUTTON)
            .semantics { contentDescription = deleteSelectedLabel }
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null
        )
    }
}

@Composable
private fun DefaultTopBarActions(
    uiState: ShoppingListUiState,
    syncCallbacks: ShoppingListSyncCallbacks
) {
    val syncNowLabel = stringResource(R.string.sync_now)

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
    val canSubmit = value.isNotBlank()

    fun submitAndRestoreContinuousEntry() {
        inputCallbacks.onAddItem()
        onContinuousEntryRequested()
    }

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
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                submitAndRestoreContinuousEntry()
            }
        ),
        trailingIcon = {
            IconButton(
                onClick = ::submitAndRestoreContinuousEntry,
                enabled = canSubmit,
                modifier = Modifier.testTag(ShoppingListTestTags.SUBMIT_INPUT_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddTask,
                    contentDescription = stringResource(R.string.add_item_submit)
                )
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingItemsContent(
    uiState: ShoppingListUiState,
    itemCallbacks: ShoppingListItemCallbacks,
    modifier: Modifier = Modifier,
    iconResolver: IconResolver,
    bottomContentPadding: Dp
) {
    val swipeResetTrigger = uiState.itemPendingDeletion?.id.orEmpty()
    val emptyPendingTitle = stringResource(R.string.empty_pending_title)
    val emptyPurchasedTitle = stringResource(R.string.empty_purchased_title)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imeNestedScroll(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottomContentPadding),
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
            callbacks = PendingItemRowCallbacks(
                onClick = { itemCallbacks.onPendingItemClick(item.id) },
                onLongClick = { itemCallbacks.onPendingItemLongPress(item.id) },
                onDeleteRequested = { itemCallbacks.onDeleteItemRequested(item) }
            ),
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
    callbacks: PendingItemRowCallbacks,
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
            onClick = callbacks.onClick,
            onLongClick = callbacks.onLongClick,
            onDeleteRequested = callbacks.onDeleteRequested,
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
                .combinedClickable(
                    onClick = interactions.onClick,
                    onLongClick = interactions.onLongClick
                ),
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
