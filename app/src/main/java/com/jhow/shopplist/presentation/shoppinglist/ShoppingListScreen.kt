package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShoppingListInputCallbacks(
    val onValueChange: (String) -> Unit = {},
    val onAddItem: () -> Unit = {},
    val onSuggestionSelected: (String) -> Unit = {}
)

class ShoppingListItemCallbacks(
    val onPendingItemClick: (String) -> Unit = {},
    val onPendingItemMarkPurchased: (String) -> Unit = {},
    val onPendingItemLongPress: (String) -> Unit = {},
    val onPurchasedItemClick: (String) -> Unit = {},
    val onPurchaseSelectedItems: () -> Unit = {},
    val onDeleteSelectedItems: () -> Unit = {},
    val onSelectionModeExited: () -> Unit = {},
    val onDeleteItemRequested: (ShoppingItem) -> Unit = {},
    val onDeleteUndoRequested: () -> Unit = {}
)

class ShoppingListSyncCallbacks(
    val onManualSyncRequested: () -> Unit = {},
    val onSyncSettingsClicked: () -> Unit = {}
)

private const val EMPTY_STATE_ICON_ALPHA = 0.5f
private const val STRIKETHROUGH_CENTER_Y_FRACTION = 0.52f
private const val STRIKETHROUGH_ANIMATION_MILLIS = 200

private data class ShoppingListContentDependencies(
    val iconResolver: IconResolver,
    val haptics: HapticVocabulary
)

private data class ShoppingItemRowVisuals(
    val leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val showStrikethrough: Boolean = false,
    val rowAlpha: Float = 1f
)

private data class PendingItemRowCallbacks(
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
    val onMarkPurchased: () -> Unit,
    val onDeleteRequested: () -> Unit
)

private data class PendingItemRowState(
    val item: ShoppingItem,
    val isSelected: Boolean,
    val isSelectionMode: Boolean,
    val animatePurchaseStrikethrough: Boolean = false
)

private data class PendingItemsSectionState(
    val items: List<ShoppingItem>,
    val selectedIds: Set<String>,
    val isSelectionMode: Boolean,
    val purchasingItems: Map<String, ShoppingItem>,
    val emptyText: String
)

private data class PendingPurchaseAnimationCallbacks(
    val onStarted: (ShoppingItem) -> Unit,
    val onFinished: (String) -> Unit
)

private data class PurchasedItemsSectionState(
    val items: List<ShoppingItem>,
    val animatedPurchaseIds: Set<String>
)

private data class PurchasedItemRowState(
    val item: ShoppingItem,
    val animatePurchaseStrikethrough: Boolean
)

private data class ShoppingItemRowInteractions(
    val onClick: () -> Unit,
    val onLongClick: () -> Unit = {},
    val onDeleteRequested: () -> Unit,
    val onSwipeThresholdCrossed: () -> Unit,
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
            onPendingItemMarkPurchased = viewModel::onPendingItemMarkPurchased,
            onPendingItemLongPress = viewModel::onPendingItemLongPressed,
            onPurchasedItemClick = viewModel::onPurchasedItemClicked,
            onPurchaseSelectedItems = viewModel::onPurchaseSelectedItems,
            onDeleteSelectedItems = viewModel::onDeleteSelectedItems,
            onSelectionModeExited = viewModel::onSelectionModeExited,
            onDeleteItemRequested = viewModel::onDeleteItemRequested,
            onDeleteUndoRequested = viewModel::onDeleteUndoRequested
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

@Composable
private fun rememberHapticVocabulary(): HapticVocabulary {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { HapticVocabulary(hapticFeedback) }
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
    val haptics = rememberHapticVocabulary()
    var inputBarContentHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
    }
    DeleteUndoSnackbarEffect(
        snackbarState = uiState.deleteUndoSnackbar,
        snackbarHostState = snackbarHostState,
        onUndo = itemCallbacks.onDeleteUndoRequested
    )

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
                    syncCallbacks = syncCallbacks,
                    haptics = haptics
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
            dependencies = ShoppingListContentDependencies(
                iconResolver = iconResolver,
                haptics = haptics
            )
        )
    }
}

@Composable
private fun DeleteUndoSnackbarEffect(
    snackbarState: DeleteUndoSnackbarState?,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit
) {
    val message = when {
        snackbarState == null -> ""
        snackbarState.count == 1 -> stringResource(R.string.delete_item_snackbar_single)
        else -> pluralStringResource(
            R.plurals.delete_item_snackbar_multiple,
            snackbarState.count,
            snackbarState.count
        )
    }
    val undoLabel = stringResource(R.string.delete_item_undo)

    LaunchedEffect(snackbarState) {
        if (snackbarState == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }

        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndo()
        }
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
    dependencies: ShoppingListContentDependencies
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
            bottomContentPadding = shoppingListBottomContentPadding(layout.inputBarHeight),
            dependencies = dependencies
        )
        ShoppingInputBar(
            value = uiState.inputValue,
            suggestions = uiState.suggestions,
            inputCallbacks = inputCallbacks,
            onContentHeightChanged = onInputBarHeightChanged,
            haptics = dependencies.haptics,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListTopAppBar(
    uiState: ShoppingListUiState,
    itemCallbacks: ShoppingListItemCallbacks,
    syncCallbacks: ShoppingListSyncCallbacks,
    haptics: HapticVocabulary
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
                syncCallbacks = syncCallbacks,
                haptics = haptics
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
    syncCallbacks: ShoppingListSyncCallbacks,
    haptics: HapticVocabulary
) {
    if (uiState.isSelectionMode) {
        SelectionModeActions(itemCallbacks = itemCallbacks, haptics = haptics)
    } else {
        DefaultTopBarActions(
            uiState = uiState,
            syncCallbacks = syncCallbacks
        )
    }
}

@Composable
private fun SelectionModeActions(
    itemCallbacks: ShoppingListItemCallbacks,
    haptics: HapticVocabulary
) {
    val purchaseSelectedLabel = stringResource(R.string.purchase_selected_items)
    val deleteSelectedLabel = stringResource(R.string.delete_selected_items)

    IconButton(
        onClick = {
            haptics.batchAction()
            itemCallbacks.onPurchaseSelectedItems()
        },
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
        onClick = {
            haptics.batchAction()
            itemCallbacks.onDeleteSelectedItems()
        },
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
    haptics: HapticVocabulary,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun restoreContinuousEntry() {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun dismissContinuousEntry() {
        focusManager.clearFocus()
        keyboardController?.hide()
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
                haptics = haptics,
                onContinuousEntryRequested = ::restoreContinuousEntry
            )
            ShoppingSuggestionsList(
                suggestions = suggestions,
                onSuggestionTapped = { suggestion ->
                    inputCallbacks.onSuggestionSelected(suggestion)
                    dismissContinuousEntry()
                },
                onSuggestionLongPressed = { suggestion ->
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
    haptics: HapticVocabulary,
    onContinuousEntryRequested: () -> Unit
) {
    val canSubmit = value.isNotBlank()

    fun submitAndRestoreContinuousEntry() {
        if (!canSubmit) return
        inputCallbacks.onAddItem()
        haptics.add()
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
    onSuggestionTapped: (String) -> Unit,
    onSuggestionLongPressed: (String) -> Unit
) {
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
    ) {
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
                                .combinedClickable(
                                    onClick = { onSuggestionTapped(suggestion) },
                                    onLongClick = { onSuggestionLongPressed(suggestion) }
                                )
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
    bottomContentPadding: Dp,
    dependencies: ShoppingListContentDependencies
) {
    val swipeResetTrigger = uiState.deleteUndoSnackbar?.count?.toString().orEmpty()
    val emptyPendingHelper = stringResource(R.string.empty_pending_helper)
    val emptyPurchasedHelper = stringResource(R.string.empty_purchased_helper)
    var animatedPurchaseIds by remember { mutableStateOf(emptySet<String>()) }
    var purchasingItems by remember { mutableStateOf(emptyMap<String, ShoppingItem>()) }
    var userPurchasedSectionExpanded by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val purchasedSectionVisibility = PurchasedSectionState.resolve(
        pendingCount = uiState.pendingItems.size,
        purchasedCount = uiState.purchasedItems.size,
        userExpanded = userPurchasedSectionExpanded
    )
    val isPurchasedSectionExpanded = purchasedSectionVisibility == PurchasedSectionVisibility.Expanded

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imeNestedScroll(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = ShoppingListTestTags.PENDING_SECTION) {
            SectionHeader(
                title = sectionTitle(
                    title = stringResource(R.string.pending_items_title),
                    count = uiState.pendingItems.size
                ),
                modifier = Modifier.testTag(ShoppingListTestTags.PENDING_SECTION)
            )
        }

        pendingItemsSection(
            state = PendingItemsSectionState(
                items = uiState.pendingItems,
                selectedIds = uiState.selectedIds,
                isSelectionMode = uiState.isSelectionMode,
                purchasingItems = purchasingItems,
                emptyText = emptyPendingHelper
            ),
            swipeResetTrigger = swipeResetTrigger,
            itemCallbacks = itemCallbacks,
            dependencies = dependencies,
            animationCallbacks = PendingPurchaseAnimationCallbacks(
                onStarted = { item -> purchasingItems += item.id to item; animatedPurchaseIds += item.id },
                onFinished = { itemId -> purchasingItems -= itemId; animatedPurchaseIds -= itemId }
            )
        )

        item(key = ShoppingListTestTags.SECTION_DIVIDER) {
            HorizontalDivider(modifier = Modifier.testTag(ShoppingListTestTags.SECTION_DIVIDER))
        }

        item(key = ShoppingListTestTags.PURCHASED_SECTION) {
            SectionHeader(
                title = sectionTitle(
                    title = stringResource(R.string.purchased_items_title),
                    count = uiState.purchasedItems.size
                ),
                modifier = Modifier
                    .testTag(ShoppingListTestTags.PURCHASED_SECTION)
                    .clickable {
                        userPurchasedSectionExpanded = !isPurchasedSectionExpanded
                    }
            )
        }

        if (isPurchasedSectionExpanded) {
            purchasedItemsSection(
                state = PurchasedItemsSectionState(
                    items = uiState.purchasedItems,
                    animatedPurchaseIds = animatedPurchaseIds
                ),
                swipeResetTrigger = swipeResetTrigger,
                itemCallbacks = itemCallbacks,
                emptyPurchasedText = emptyPurchasedHelper,
                dependencies = dependencies,
                onPurchaseAnimationFinished = { itemId -> animatedPurchaseIds = animatedPurchaseIds - itemId }
            )
        }
    }
}

private fun sectionTitle(title: String, count: Int): String = "$title \u00b7 $count"

private fun androidx.compose.foundation.lazy.LazyListScope.pendingItemsSection(
    state: PendingItemsSectionState,
    swipeResetTrigger: String,
    itemCallbacks: ShoppingListItemCallbacks,
    dependencies: ShoppingListContentDependencies,
    animationCallbacks: PendingPurchaseAnimationCallbacks
) {
    val purchasingItems = state.purchasingItems.values.filterNot { purchasingItem ->
        state.items.any { item -> item.id == purchasingItem.id }
    }
    if (state.items.isEmpty() && purchasingItems.isEmpty()) {
        item(key = ShoppingListTestTags.EMPTY_STATE) {
            EmptyState(
                text = state.emptyText,
                iconTag = ShoppingListTestTags.EMPTY_PENDING_ICON,
                textTag = ShoppingListTestTags.EMPTY_PENDING_TEXT,
                modifier = Modifier.testTag(ShoppingListTestTags.EMPTY_STATE)
            )
        }
        return
    }

    items(
        items = purchasingItems,
        key = { item -> "purchasing_${item.id}" }
    ) { item ->
        PendingItemRow(
            state = PendingItemRowState(
                item = item,
                isSelected = false,
                isSelectionMode = false,
                animatePurchaseStrikethrough = true
            ),
            callbacks = PendingItemRowCallbacks(
                onClick = {},
                onLongClick = {},
                onMarkPurchased = {},
                onDeleteRequested = {}
            ),
            onPurchaseAnimationStarted = {},
            onPurchaseAnimationFinished = { animationCallbacks.onFinished(item.id) },
            swipeResetTrigger = swipeResetTrigger,
            dependencies = dependencies,
            modifier = Modifier.animateItem()
        )
    }

    items(
        items = state.items,
        key = { item -> item.id }
    ) { item ->
        PendingItemRow(
            state = PendingItemRowState(
                item = item,
                isSelected = item.id in state.selectedIds,
                isSelectionMode = state.isSelectionMode
            ),
            callbacks = PendingItemRowCallbacks(
                onClick = { itemCallbacks.onPendingItemClick(item.id) },
                onLongClick = { itemCallbacks.onPendingItemLongPress(item.id) },
                onMarkPurchased = {
                    dependencies.haptics.purchaseTap()
                    itemCallbacks.onPendingItemMarkPurchased(item.id)
                },
                onDeleteRequested = { itemCallbacks.onDeleteItemRequested(item) }
            ),
            onPurchaseAnimationStarted = { animationCallbacks.onStarted(item) },
            onPurchaseAnimationFinished = {},
            swipeResetTrigger = swipeResetTrigger,
            dependencies = dependencies,
            modifier = Modifier.animateItem()
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.purchasedItemsSection(
    state: PurchasedItemsSectionState,
    swipeResetTrigger: String,
    itemCallbacks: ShoppingListItemCallbacks,
    emptyPurchasedText: String,
    dependencies: ShoppingListContentDependencies,
    onPurchaseAnimationFinished: (String) -> Unit
) {
    if (state.items.isEmpty()) {
        item(key = "purchased_empty") {
            EmptyState(
                text = emptyPurchasedText,
                iconTag = ShoppingListTestTags.EMPTY_PURCHASED_ICON,
                textTag = ShoppingListTestTags.EMPTY_PURCHASED_TEXT
            )
        }
        return
    }

    items(
        items = state.items,
        key = { item -> item.id }
    ) { item ->
        PurchasedItemRow(
            state = PurchasedItemRowState(
                item = item,
                animatePurchaseStrikethrough = item.id in state.animatedPurchaseIds
            ),
            onClick = { itemCallbacks.onPurchasedItemClick(item.id) },
            onDeleteRequested = { itemCallbacks.onDeleteItemRequested(item) },
            swipeResetTrigger = swipeResetTrigger,
            dependencies = dependencies,
            onPurchaseAnimationFinished = { onPurchaseAnimationFinished(item.id) },
            modifier = Modifier.animateItem()
        )
    }
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
private fun EmptyState(
    text: String,
    iconTag: String,
    textTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ShoppingBag,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(48.dp)
                .alpha(EMPTY_STATE_ICON_ALPHA)
                .testTag(iconTag)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(textTag)
        )
    }
}

@Composable
private fun PendingItemRow(
    state: PendingItemRowState,
    callbacks: PendingItemRowCallbacks,
    onPurchaseAnimationStarted: () -> Unit,
    onPurchaseAnimationFinished: () -> Unit,
    swipeResetTrigger: String,
    dependencies: ShoppingListContentDependencies,
    modifier: Modifier = Modifier
) {
    val item = state.item
    val isSelected = state.isSelected
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

    val iconResolver = dependencies.iconResolver
    val haptics = dependencies.haptics
    val iconVersion = iconResolver.version
    val leadingIcon = remember(item.name, iconVersion) {
        iconResolver.resolveIcon(item.name)
    }
    val pendingState = stringResource(R.string.shopping_item_state_pending)
    val selectedState = stringResource(R.string.shopping_item_state_selected)
    val markPurchasedLabel = stringResource(R.string.shopping_item_action_mark_purchased)
    val deleteLabel = stringResource(R.string.shopping_item_action_delete)

    ShoppingItemRow(
        name = item.name,
        visuals = ShoppingItemRowVisuals(
            leadingIcon = leadingIcon,
            containerColor = containerColor,
            contentColor = contentColor,
            showStrikethrough = state.animatePurchaseStrikethrough
        ),
        interactions = ShoppingItemRowInteractions(
            onClick = {
                if (!state.isSelectionMode) {
                    haptics.purchaseTap()
                    onPurchaseAnimationStarted()
                }
                callbacks.onClick()
            },
            onLongClick = {
                haptics.multiSelectEntry()
                callbacks.onLongClick()
            },
            onDeleteRequested = callbacks.onDeleteRequested,
            onSwipeThresholdCrossed = haptics::swipeThreshold,
            swipeTag = ShoppingListTestTags.swipePendingItem(item.id),
            swipeResetTrigger = swipeResetTrigger
        ),
        animateStrikethroughFromStart = state.animatePurchaseStrikethrough,
        onStrikethroughAnimationFinished = onPurchaseAnimationFinished,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = item.name
                stateDescription = if (isSelected) selectedState else pendingState
                customActions = listOf(
                    CustomAccessibilityAction(markPurchasedLabel) {
                        onPurchaseAnimationStarted()
                        callbacks.onMarkPurchased()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        callbacks.onDeleteRequested()
                        true
                    }
                )
                if (state.isSelectionMode) {
                    role = Role.Checkbox
                    toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
                }
            }
            .testTag(ShoppingListTestTags.pendingItem(item.id))
    )
}

@Composable
private fun PurchasedItemRow(
    state: PurchasedItemRowState,
    onClick: () -> Unit,
    onDeleteRequested: () -> Unit,
    swipeResetTrigger: String,
    dependencies: ShoppingListContentDependencies,
    onPurchaseAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.item
    val iconResolver = dependencies.iconResolver
    val haptics = dependencies.haptics
    var isRestoring by remember(item.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val iconVersion = iconResolver.version
    val leadingIcon = remember(item.name, iconVersion) {
        iconResolver.resolveIcon(item.name)
    }
    val purchasedState = stringResource(R.string.shopping_item_state_purchased)
    val restoreLabel = stringResource(R.string.shopping_item_action_restore_pending)
    val deleteLabel = stringResource(R.string.shopping_item_action_delete)

    ShoppingItemRow(
        name = item.name,
        visuals = ShoppingItemRowVisuals(
            leadingIcon = leadingIcon,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            showStrikethrough = !isRestoring,
            rowAlpha = 0.6f
        ),
        interactions = ShoppingItemRowInteractions(
            onClick = {
                haptics.restore()
                isRestoring = true
                coroutineScope.launch {
                    delay(STRIKETHROUGH_ANIMATION_MILLIS.toLong())
                    onClick()
                }
            },
            onDeleteRequested = onDeleteRequested,
            onSwipeThresholdCrossed = haptics::swipeThreshold,
            swipeTag = ShoppingListTestTags.swipePurchasedItem(item.id),
            swipeResetTrigger = swipeResetTrigger
        ),
        animateStrikethroughFromStart = state.animatePurchaseStrikethrough,
        onStrikethroughAnimationFinished = onPurchaseAnimationFinished,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = item.name
                stateDescription = purchasedState
                customActions = listOf(
                    CustomAccessibilityAction(restoreLabel) {
                        haptics.restore()
                        isRestoring = true
                        coroutineScope.launch {
                            delay(STRIKETHROUGH_ANIMATION_MILLIS.toLong())
                            onClick()
                        }
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDeleteRequested()
                        true
                    }
                )
            }
            .testTag(ShoppingListTestTags.purchasedItem(item.id))
    )
}

@Composable
private fun ShoppingItemRow(
    name: String,
    visuals: ShoppingItemRowVisuals,
    interactions: ShoppingItemRowInteractions,
    modifier: Modifier = Modifier,
    animateStrikethroughFromStart: Boolean = false,
    onStrikethroughAnimationFinished: () -> Unit = {},
) {
    val dismissState = rememberSwipeToDismissBoxState()
    var didFireSwipeThresholdHaptic by remember { mutableStateOf(false) }

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            interactions.onDeleteRequested()
            dismissState.reset()
        }
    }

    LaunchedEffect(dismissState.targetValue) {
        if (
            dismissState.currentValue == SwipeToDismissBoxValue.Settled &&
            dismissState.targetValue == SwipeToDismissBoxValue.EndToStart &&
            !didFireSwipeThresholdHaptic
        ) {
            interactions.onSwipeThresholdCrossed()
            didFireSwipeThresholdHaptic = true
        }
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
            didFireSwipeThresholdHaptic = false
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
            AnimatedStrikethroughText(
                text = name,
                color = visuals.contentColor,
                isStrikethroughVisible = visuals.showStrikethrough,
                animateFromStart = animateStrikethroughFromStart,
                onAnimationFinished = onStrikethroughAnimationFinished,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AnimatedStrikethroughText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    isStrikethroughVisible: Boolean,
    animateFromStart: Boolean,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(if (isStrikethroughVisible && !animateFromStart) 1f else 0f) }
    LaunchedEffect(isStrikethroughVisible) {
        progress.animateTo(
            targetValue = if (isStrikethroughVisible) 1f else 0f,
            animationSpec = tween(durationMillis = STRIKETHROUGH_ANIMATION_MILLIS)
        )
        onAnimationFinished()
    }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = color,
            modifier = Modifier.fillMaxWidth()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (progress.value > 0f) {
                val y = size.height * STRIKETHROUGH_CENTER_Y_FRACTION
                drawLine(
                    color = color,
                    start = Offset(0f, y),
                    end = Offset(size.width * progress.value, y),
                    strokeWidth = 2.dp.toPx()
                )
            }
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
