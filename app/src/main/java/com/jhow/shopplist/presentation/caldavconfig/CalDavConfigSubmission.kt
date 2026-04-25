@file:Suppress("MatchingDeclarationName")
package com.jhow.shopplist.presentation.caldavconfig

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavValidationResult

internal sealed interface CalDavConfigSubmissionOutcome {
    data class Success(
        val resolvedCollectionUrl: String,
        val updatedState: CalDavConfigUiState
    ) : CalDavConfigSubmissionOutcome

    data class Failure(
        val updatedState: CalDavConfigUiState
    ) : CalDavConfigSubmissionOutcome
}

internal fun resolveConfigSubmission(
    current: CalDavConfigUiState,
    result: CalDavValidationResult
): CalDavConfigSubmissionOutcome = when (result) {
    is CalDavValidationResult.Success -> CalDavConfigSubmissionOutcome.Success(
        resolvedCollectionUrl = result.resolvedCollectionUrl,
        updatedState = current.finishSavingAfterSuccess()
    )

    is CalDavValidationResult.MissingList -> CalDavConfigSubmissionOutcome.Failure(
        current.finishSavingWithMessage(
            message = "Remote list ${result.listName} does not exist yet",
            pendingAction = CalDavPendingAction.CreateMissingList
        )
    )

    is CalDavValidationResult.AuthError -> CalDavConfigSubmissionOutcome.Failure(
        current.finishSavingWithMessage(result.message)
    )

    is CalDavValidationResult.NetworkError -> CalDavConfigSubmissionOutcome.Failure(
        current.finishSavingWithMessage(result.message)
    )

    is CalDavValidationResult.ConfigurationError -> CalDavConfigSubmissionOutcome.Failure(
        current.finishSavingWithMessage(result.message)
    )
}

internal fun CalDavConfigUiState.startSaving(): CalDavConfigUiState =
    copy(
        isLoading = false,
        isSaving = true,
        statusMessage = null,
        pendingAction = CalDavPendingAction.None
    )

private fun CalDavConfigUiState.finishSavingAfterSuccess(): CalDavConfigUiState =
    copy(
        password = "",
        isSaving = false,
        isSaveSuccessful = true,
        savedServerUrl = serverUrl,
        statusMessage = null,
        pendingAction = CalDavPendingAction.None
    )

private fun CalDavConfigUiState.finishSavingWithMessage(
    message: String,
    pendingAction: CalDavPendingAction = CalDavPendingAction.None
): CalDavConfigUiState = copy(
    isSaving = false,
    statusMessage = message,
    pendingAction = pendingAction
)
