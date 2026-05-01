package com.jhow.shopplist.presentation.caldavconfig

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jhow.shopplist.R
import com.jhow.shopplist.domain.model.CalDavPendingAction

@Composable
fun CalDavConfigRoute(
    onNavigateBack: () -> Unit,
    viewModel: CalDavConfigViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val callbacks = remember(viewModel) {
        CalDavConfigCallbacks(
            onServerUrlChanged = viewModel::onServerUrlChanged,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onListNameChanged = viewModel::onListNameChanged,
            onSaveClicked = viewModel::onSaveClicked,
            onConfirmCreateMissingList = viewModel::onConfirmCreateMissingList,
            onResetForm = viewModel::onResetForm,
            onNavigateBack = onNavigateBack
        )
    }
    CalDavConfigScreen(
        uiState = uiState.value,
        callbacks = callbacks
    )
}

class CalDavConfigCallbacks(
    val onServerUrlChanged: (String) -> Unit = {},
    val onUsernameChanged: (String) -> Unit = {},
    val onPasswordChanged: (String) -> Unit = {},
    val onListNameChanged: (String) -> Unit = {},
    val onSaveClicked: () -> Unit = {},
    val onConfirmCreateMissingList: () -> Unit = {},
    val onResetForm: () -> Unit = {},
    val onNavigateBack: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalDavConfigScreen(
    uiState: CalDavConfigUiState,
    modifier: Modifier = Modifier,
    callbacks: CalDavConfigCallbacks = CalDavConfigCallbacks()
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(CalDavConfigTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isSaveSuccessful) {
                            stringResource(R.string.caldav_config_title_success)
                        } else {
                            stringResource(R.string.caldav_config_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = callbacks.onNavigateBack,
                        modifier = Modifier.testTag(CalDavConfigTestTags.BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.caldav_config_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.isSaveSuccessful,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.92f))
                    .togetherWith(fadeOut())
            },
            label = "caldav_config_content",
            modifier = Modifier.padding(innerPadding)
        ) { isSuccess ->
            if (isSuccess) {
                CalDavConfigSuccessContent(
                    savedServerUrl = uiState.savedServerUrl,
                    callbacks = callbacks
                )
            } else {
                CalDavConfigFormContent(
                    uiState = uiState,
                    callbacks = callbacks
                )
            }
        }
    }
}

@Composable
private fun CalDavConfigFormContent(
    uiState: CalDavConfigUiState,
    callbacks: CalDavConfigCallbacks
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalDavConfigFields(uiState = uiState, callbacks = callbacks)

        if (uiState.isLoading || uiState.isSaving) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CalDavConfigTestTags.PROGRESS_INDICATOR)
            )
        }

        if (uiState.statusMessage != null) {
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CalDavConfigTestTags.STATUS_TEXT)
            )
        }

        if (uiState.pendingAction == CalDavPendingAction.CreateMissingList) {
            Button(
                onClick = callbacks.onConfirmCreateMissingList,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CalDavConfigTestTags.CREATE_LIST_BUTTON)
            ) {
                Text(stringResource(R.string.sync_create_missing_list))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = callbacks.onSaveClicked,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CalDavConfigTestTags.SAVE_BUTTON)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.caldav_config_save_connect))
            }
        }
    }
}

@Composable
private fun CalDavConfigFields(
    uiState: CalDavConfigUiState,
    callbacks: CalDavConfigCallbacks
) {
    val fieldsEnabled = !uiState.isSaving && !uiState.isLoading

    OutlinedTextField(
        value = uiState.serverUrl,
        onValueChange = callbacks.onServerUrlChanged,
        label = { Text(stringResource(R.string.sync_server_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CalDavConfigTestTags.SERVER_FIELD),
        singleLine = true,
        enabled = fieldsEnabled
    )

    OutlinedTextField(
        value = uiState.username,
        onValueChange = callbacks.onUsernameChanged,
        label = { Text(stringResource(R.string.sync_username_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CalDavConfigTestTags.USERNAME_FIELD),
        singleLine = true,
        enabled = fieldsEnabled
    )

    OutlinedTextField(
        value = uiState.password,
        onValueChange = callbacks.onPasswordChanged,
        label = { Text(stringResource(R.string.sync_password_label)) },
        placeholder = {
            if (uiState.hasStoredPassword && uiState.password.isBlank()) {
                Text(stringResource(R.string.sync_password_saved_placeholder))
            }
        },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CalDavConfigTestTags.PASSWORD_FIELD),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = fieldsEnabled
    )

    OutlinedTextField(
        value = uiState.listName,
        onValueChange = callbacks.onListNameChanged,
        label = { Text(stringResource(R.string.sync_list_name_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CalDavConfigTestTags.LIST_NAME_FIELD),
        singleLine = true,
        enabled = fieldsEnabled
    )
}

@Composable
private fun CalDavConfigSuccessContent(
    savedServerUrl: String,
    callbacks: CalDavConfigCallbacks
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = stringResource(R.string.caldav_config_success_icon),
            tint = Color(0xFF4CAF50.toInt()),
            modifier = Modifier
                .size(96.dp)
                .testTag(CalDavConfigTestTags.SUCCESS_ICON)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.caldav_config_saved_headline),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(CalDavConfigTestTags.SUCCESS_HEADLINE)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.caldav_config_connected_to, savedServerUrl),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(CalDavConfigTestTags.SUCCESS_SUMMARY)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = callbacks.onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CalDavConfigTestTags.DONE_BUTTON)
        ) {
            Text(stringResource(R.string.caldav_config_done))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = callbacks.onResetForm,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CalDavConfigTestTags.CONFIGURE_ANOTHER_BUTTON)
        ) {
            Text(stringResource(R.string.caldav_config_configure_another))
        }
    }
}
