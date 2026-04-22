package com.jhow.shopplist.domain.model

sealed interface CalDavValidationResult {
    data class Success(
        val resolvedCollectionUrl: String = ""
    ) : CalDavValidationResult

    data class MissingList(
        val listName: String
    ) : CalDavValidationResult

    data class ConfigurationError(
        val message: String
    ) : CalDavValidationResult

    data class NetworkError(
        val message: String
    ) : CalDavValidationResult
}
