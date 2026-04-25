package com.jhow.shopplist.presentation.caldavconfig

import com.jhow.shopplist.domain.model.CalDavPendingAction

data class CalDavConfigUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val listName: String = "",
    val hasStoredPassword: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaveSuccessful: Boolean = false,
    val statusMessage: String? = null,
    val pendingAction: CalDavPendingAction = CalDavPendingAction.None,
    val savedServerUrl: String = ""
)
