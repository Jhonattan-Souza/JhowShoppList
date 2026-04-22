package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.data.sync.CalDavListLocator
import com.jhow.shopplist.data.sync.CalDavAuthenticationException
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject

open class ValidateCalDavSyncSettingsUseCase @Inject constructor(
    private val configRepository: CalDavConfigRepository,
    private val listLocator: CalDavListLocator
) {
    open suspend operator fun invoke(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    ): CalDavValidationResult {
        if (!enabled) {
            return CalDavValidationResult.Success()
        }

        if (serverUrl.isBlank() || username.isBlank() || listName.isBlank()) {
            return CalDavValidationResult.ConfigurationError(
                message = "Server, username, and list name are required"
            )
        }

        val resolvedPassword = password.ifBlank { configRepository.getPassword().orEmpty() }
        if (resolvedPassword.isBlank()) {
            return CalDavValidationResult.ConfigurationError(message = "Password is required")
        }

        return try {
            when (
                val result = listLocator.locate(
                    serverUrl = serverUrl,
                    username = username,
                    password = resolvedPassword,
                    listName = listName
                )
            ) {
                is CalDavListLocator.Result.Found -> {
                    CalDavValidationResult.Success(resolvedCollectionUrl = result.href)
                }
                CalDavListLocator.Result.Missing -> {
                    CalDavValidationResult.MissingList(listName = listName)
                }
                CalDavListLocator.Result.Ambiguous -> {
                    CalDavValidationResult.ConfigurationError(
                        message = "Multiple matching lists were found"
                    )
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: CalDavAuthenticationException) {
            CalDavValidationResult.AuthError(message = "Authentication failed")
        } catch (_: Exception) {
            CalDavValidationResult.NetworkError(message = "Unable to validate sync settings")
        }
    }
}
