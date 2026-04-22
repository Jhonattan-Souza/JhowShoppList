package com.jhow.shopplist.presentation.shoppinglist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncState
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
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onPendingItemClick = viewModel::onPendingItemClicked,
        onPurchasedItemClick = viewModel::onPurchasedItemClicked,
        onPurchaseSelectedItems = viewModel::onPurchaseSelectedItems,
        onDeleteItemRequested = viewModel::onDeleteItemRequested,
        onDeleteItemDismissed = viewModel::onDeleteItemDismissed,
        onDeleteItemConfirmed = viewModel::onDeleteItemConfirmed,
        onSyncMenuClicked = viewModel::onSyncMenuClicked,
        onSyncMenuDismissed = viewModel::onSyncMenuDismissed,
        onSyncSettingsRequested = viewModel::onSyncSettingsRequested,
        onSyncSettingsDismissed = viewModel::onSyncSettingsDismissed,
        onSyncSettingsSaved = viewModel::onSyncSettingsSaved,
        onSyncNowRequested = viewModel::onSyncNowRequested,
        onConfirmCreateMissingList = viewModel::onConfirmCreateMissingList
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    onInputValueChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onPendingItemClick: (String) -> Unit,
    onPurchasedItemClick: (String) -> Unit,
    onPurchaseSelectedItems: () -> Unit,
    onDeleteItemRequested: (ShoppingItem) -> Unit,
    onDeleteItemDismissed: () -> Unit,
    onDeleteItemConfirmed: () -> Unit,
    onSyncMenuClicked: () -> Unit,
    onSyncMenuDismissed: () -> Unit,
    onSyncSettingsRequested: () -> Unit,
    onSyncSettingsDismissed: () -> Unit,
    onSyncSettingsSaved: () -> Unit,
    onSyncNowRequested: () -> Unit,
    onConfirmCreateMissingList: () -> Unit,
    modifier: Modifier = Modifier
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
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.shopping_list_title))
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = onSyncMenuClicked,
                            modifier = Modifier.testTag(ShoppingListTestTags.SYNC_MENU_BUTTON)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.sync_menu_open)
                            )
                        }

                        DropdownMenu(
                            expanded = uiState.isSyncMenuExpanded,
                            onDismissRequest = onSyncMenuDismissed
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sync_now)) },
                                onClick = onSyncNowRequested,
                                modifier = Modifier.testTag(ShoppingListTestTags.SYNC_NOW_MENU_ITEM)
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sync_settings)) },
                                onClick = onSyncSettingsRequested,
                                modifier = Modifier.testTag(ShoppingListTestTags.SYNC_SETTINGS_MENU_ITEM)
                            )
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ShoppingItemsContent(
                uiState = uiState,
                onPendingItemClick = onPendingItemClick,
                onPurchasedItemClick = onPurchasedItemClick,
                onDeleteItemRequested = onDeleteItemRequested,
                modifier = Modifier.fillMaxSize()
            )

            ShoppingInputBar(
                value = uiState.inputValue,
                suggestions = uiState.suggestions,
                onValueChange = onInputValueChange,
                onDone = onAddItem,
                onSuggestionSelected = onSuggestionSelected,
                onContentHeightChanged = { inputBarContentHeightPx = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            BulkPurchaseFab(
                visible = uiState.isBulkActionVisible,
                onClick = onPurchaseSelectedItems,
                bottomClearance = bulkFabBottomClearance,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        DeleteItemDialog(
            item = uiState.itemPendingDeletion,
            onDismiss = onDeleteItemDismissed,
            onConfirm = onDeleteItemConfirmed
        )

        if (uiState.isSyncSettingsVisible) {
            val settings = uiState.syncSettings
            var enabled by remember { mutableStateOf(settings.enabled) }
            var serverUrl by remember { mutableStateOf(settings.serverUrl) }
            var username by remember { mutableStateOf(settings.username) }
            var password by remember { mutableStateOf(settings.password) }
            var listName by remember { mutableStateOf(settings.listName) }

            ModalBottomSheet(
                onDismissRequest = onSyncSettingsDismissed,
                modifier = Modifier.testTag(ShoppingListTestTags.SYNC_SETTINGS_SHEET)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .testTag(ShoppingListTestTags.SYNC_SETTINGS_SHEET_CONTENT)
                ) {
                    Text(
                        text = stringResource(R.string.sync_settings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sync_enabled_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            modifier = Modifier.testTag(ShoppingListTestTags.SYNC_ENABLED_SWITCH)
                        )
                    }

                    Text(
                        text = stringResource(R.string.sync_state_label, syncStateLabel(settings.syncState)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag(ShoppingListTestTags.SYNC_STATE_TEXT)
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text(stringResource(R.string.sync_server_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag(ShoppingListTestTags.SYNC_SERVER_FIELD),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.sync_username_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag(ShoppingListTestTags.SYNC_USERNAME_FIELD),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.sync_password_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag(ShoppingListTestTags.SYNC_PASSWORD_FIELD),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    OutlinedTextField(
                        value = listName,
                        onValueChange = { listName = it },
                        label = { Text(stringResource(R.string.sync_list_name_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag(ShoppingListTestTags.SYNC_LIST_NAME_FIELD),
                        singleLine = true
                    )

                    if (settings.statusMessage != null) {
                        Text(
                            text = settings.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag(ShoppingListTestTags.SYNC_STATUS_TEXT)
                        )
                    }

                    if (settings.pendingAction == CalDavPendingAction.CreateMissingList) {
                        Button(
                            onClick = onConfirmCreateMissingList,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag(ShoppingListTestTags.SYNC_CREATE_LIST_BUTTON)
                        ) {
                            Text(stringResource(R.string.sync_create_missing_list))
                        }
                    }

                    Button(
                        onClick = onSyncSettingsSaved,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ShoppingListTestTags.SYNC_SAVE_BUTTON)
                    ) {
                        Text(stringResource(R.string.sync_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingInputBar(
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
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
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(ShoppingListTestTags.INPUT_FIELD),
                label = { Text(text = stringResource(R.string.add_item_label)) },
                placeholder = { Text(text = stringResource(R.string.add_item_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onDone()
                        restoreContinuousEntry()
                    }
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AddTask,
                        contentDescription = null
                    )
                }
            )

            AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 176.dp)
                        .testTag(ShoppingListTestTags.SUGGESTION_LIST),
                    shape = RoundedCornerShape(20.dp),
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
                                        .clickable {
                                            onSuggestionSelected(suggestion)
                                            restoreContinuousEntry()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                        .testTag(ShoppingListTestTags.suggestionItem(suggestion))
                                )
                            }
                        }
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
            modifier = Modifier.testTag(ShoppingListTestTags.PURCHASE_SELECTED_FAB)
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
    onPendingItemClick: (String) -> Unit,
    onPurchasedItemClick: (String) -> Unit,
    onDeleteItemRequested: (ShoppingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeResetTrigger = uiState.itemPendingDeletion?.id.orEmpty()

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

        if (uiState.pendingItems.isEmpty()) {
            item(key = ShoppingListTestTags.EMPTY_STATE) {
                EmptyStateCard(
                    title = stringResource(R.string.empty_pending_title),
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
                    onClick = { onPendingItemClick(item.id) },
                    onDeleteRequested = { onDeleteItemRequested(item) },
                    swipeResetTrigger = swipeResetTrigger
                )
            }
        }

        item(key = ShoppingListTestTags.SECTION_DIVIDER) {
            HorizontalDivider(modifier = Modifier.testTag(ShoppingListTestTags.SECTION_DIVIDER))
        }

        item(key = ShoppingListTestTags.PURCHASED_SECTION) {
            SectionHeader(
                title = stringResource(R.string.purchased_items_title),
                modifier = Modifier.testTag(ShoppingListTestTags.PURCHASED_SECTION)
            )
        }

        if (uiState.purchasedItems.isEmpty()) {
            item(key = "purchased_empty") {
                EmptyStateCard(
                    title = stringResource(R.string.empty_purchased_title)
                )
            }
        } else {
            items(
                items = uiState.purchasedItems,
                key = { item -> item.id }
            ) { item ->
                PurchasedItemRow(
                    item = item,
                    onClick = { onPurchasedItemClick(item.id) },
                    onDeleteRequested = { onDeleteItemRequested(item) },
                    swipeResetTrigger = swipeResetTrigger
                )
            }
        }
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
            .clip(RoundedCornerShape(20.dp))
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
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    ShoppingItemRow(
        name = item.name,
        leadingIcon = if (isSelected) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
        containerColor = containerColor,
        onClick = onClick,
        onDeleteRequested = onDeleteRequested,
        swipeTag = ShoppingListTestTags.swipePendingItem(item.id),
        swipeResetTrigger = swipeResetTrigger,
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
    modifier: Modifier = Modifier
) {
    ShoppingItemRow(
        name = item.name,
        leadingIcon = Icons.Rounded.History,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        textDecoration = TextDecoration.LineThrough,
        rowAlpha = 0.6f,
        onClick = onClick,
        onDeleteRequested = onDeleteRequested,
        swipeTag = ShoppingListTestTags.swipePurchasedItem(item.id),
        swipeResetTrigger = swipeResetTrigger,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.name }
            .testTag(ShoppingListTestTags.purchasedItem(item.id))
    )
}

@Composable
private fun ShoppingItemRow(
    name: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onDeleteRequested: () -> Unit,
    swipeTag: String,
    swipeResetTrigger: String,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration? = null,
    rowAlpha: Float = 1f
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDeleteRequested()
            dismissState.reset()
        }
    }

    LaunchedEffect(swipeResetTrigger) {
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
        modifier = Modifier.testTag(swipeTag)
    ) {
        Row(
            modifier = modifier
                .alpha(rowAlpha)
                .clip(RoundedCornerShape(18.dp))
                .background(containerColor)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = textDecoration,
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
            .clip(RoundedCornerShape(18.dp))
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

@Composable
private fun syncStateLabel(state: CalDavSyncState): String = when (state) {
    CalDavSyncState.Disabled -> stringResource(R.string.sync_state_disabled)
    CalDavSyncState.Idle -> stringResource(R.string.sync_state_idle)
    CalDavSyncState.Syncing -> stringResource(R.string.sync_state_syncing)
    CalDavSyncState.Success -> stringResource(R.string.sync_state_success)
    CalDavSyncState.AuthError -> stringResource(R.string.sync_state_auth_error)
    CalDavSyncState.NetworkError -> stringResource(R.string.sync_state_network_error)
    CalDavSyncState.MissingList -> stringResource(R.string.sync_state_missing_list)
    CalDavSyncState.AmbiguousListName -> stringResource(R.string.sync_state_ambiguous_list_name)
    CalDavSyncState.UserActionRequired -> stringResource(R.string.sync_state_user_action_required)
    CalDavSyncState.Warning -> stringResource(R.string.sync_state_warning)
}
