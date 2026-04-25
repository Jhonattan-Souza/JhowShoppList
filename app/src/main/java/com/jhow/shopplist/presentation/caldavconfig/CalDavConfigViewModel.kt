package com.jhow.shopplist.presentation.caldavconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.usecase.ClearCalDavPendingActionUseCase
import com.jhow.shopplist.domain.usecase.ClearCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.ConfirmCreateCalDavListUseCase
import com.jhow.shopplist.domain.usecase.GetCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.RequestShoppingSyncUseCase
import com.jhow.shopplist.domain.usecase.SaveCalDavSyncConfigUseCase
import com.jhow.shopplist.domain.usecase.ValidateCalDavSyncSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@Suppress("LongParameterList")
class CalDavConfigViewModel @Inject constructor(
    private val getCalDavSyncConfigUseCase: GetCalDavSyncConfigUseCase,
    private val validateCalDavSyncSettingsUseCase: ValidateCalDavSyncSettingsUseCase,
    private val saveCalDavSyncConfigUseCase: SaveCalDavSyncConfigUseCase,
    private val confirmCreateCalDavListUseCase: ConfirmCreateCalDavListUseCase,
    private val clearCalDavPendingActionUseCase: ClearCalDavPendingActionUseCase,
    private val clearCalDavSyncConfigUseCase: ClearCalDavSyncConfigUseCase,
    private val requestShoppingSyncUseCase: RequestShoppingSyncUseCase
) : ViewModel() {

    private val formState = MutableStateFlow(CalDavConfigUiState())

    private val configFlow = getCalDavSyncConfigUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = com.jhow.shopplist.domain.sync.CalDavSyncConfig()
    )

    val uiState: StateFlow<CalDavConfigUiState> = combine(
        formState,
        configFlow
    ) { form, config ->
        form.copy(
            hasStoredPassword = config.hasStoredPassword && form.password.isBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CalDavConfigUiState()
    )

    init {
        viewModelScope.launch {
            val config = getCalDavSyncConfigUseCase().first()
            formState.update {
                it.copy(
                    isLoading = false,
                    serverUrl = config.serverUrl,
                    username = config.username,
                    listName = config.listName,
                    isSaveSuccessful = config.enabled,
                    savedServerUrl = if (config.enabled) config.serverUrl else ""
                )
            }
        }
    }

    fun onServerUrlChanged(value: String) {
        formState.update { it.copy(serverUrl = value) }
    }

    fun onUsernameChanged(value: String) {
        formState.update { it.copy(username = value) }
    }

    fun onPasswordChanged(value: String) {
        formState.update { it.copy(password = value) }
    }

    fun onListNameChanged(value: String) {
        formState.update { it.copy(listName = value) }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            submitConfig { current ->
                validateCalDavSyncSettingsUseCase(
                    enabled = true,
                    serverUrl = current.serverUrl,
                    username = current.username,
                    listName = current.listName,
                    password = current.password
                )
            }
        }
    }

    fun onConfirmCreateMissingList() {
        viewModelScope.launch {
            submitConfig { current ->
                confirmCreateCalDavListUseCase(
                    serverUrl = current.serverUrl,
                    username = current.username,
                    listName = current.listName,
                    password = current.password
                )
            }
        }
    }

    fun onClearPendingAction() {
        viewModelScope.launch {
            clearCalDavPendingActionUseCase()
        }
    }

    fun onResetForm() {
        viewModelScope.launch {
            clearCalDavSyncConfigUseCase()
            formState.update { CalDavConfigUiState(isLoading = false) }
        }
    }

    private suspend fun submitConfig(
        submit: suspend (CalDavConfigUiState) -> CalDavValidationResult
    ) {
        formState.update(CalDavConfigUiState::startSaving)
        val current = formState.value
        when (val outcome = resolveConfigSubmission(current, submit(current))) {
            is CalDavConfigSubmissionOutcome.Success -> handleSuccess(current, outcome)
            is CalDavConfigSubmissionOutcome.Failure -> formState.value = outcome.updatedState
        }
    }

    private suspend fun handleSuccess(
        current: CalDavConfigUiState,
        outcome: CalDavConfigSubmissionOutcome.Success
    ) {
        saveCalDavSyncConfigUseCase(
            enabled = true,
            serverUrl = current.serverUrl,
            username = current.username,
            listName = current.listName,
            password = current.password,
            resolvedCollectionUrl = outcome.resolvedCollectionUrl.ifBlank { null }
        )
        formState.value = outcome.updatedState
        requestShoppingSyncUseCase()
    }
}
