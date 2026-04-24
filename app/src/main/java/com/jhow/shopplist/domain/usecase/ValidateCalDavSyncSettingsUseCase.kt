package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.core.dispatchers.IoDispatcher
import com.jhow.shopplist.data.sync.CalDavAuthenticationException
import com.jhow.shopplist.data.sync.CalDavListLocator
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

open class ValidateCalDavSyncSettingsUseCase @Inject constructor(
    private val configRepository: CalDavConfigRepository,
    private val listLocator: CalDavListLocator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    open suspend operator fun invoke(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    ): CalDavValidationResult {
        val resolvedPassword = password.ifBlank { configRepository.getPassword().orEmpty() }
        return validateInputs(enabled, serverUrl, username, listName, resolvedPassword)
            ?: locateAndMapResult(serverUrl, username, resolvedPassword, listName)
    }

    private fun validateInputs(
        enabled: Boolean,
        serverUrl: String,
        username: String,
        listName: String,
        resolvedPassword: String
    ): CalDavValidationResult? = when {
        !enabled -> CalDavValidationResult.Success()
        serverUrl.isBlank() || username.isBlank() || listName.isBlank() ->
            CalDavValidationResult.ConfigurationError(
                message = "Server, username, and list name are required"
            )
        resolvedPassword.isBlank() ->
            CalDavValidationResult.ConfigurationError(message = "Password is required")
        else -> null
    }

    private suspend fun locateAndMapResult(
        serverUrl: String,
        username: String,
        password: String,
        listName: String
    ): CalDavValidationResult {
        return try {
            when (val result = withContext(ioDispatcher) {
                listLocator.locate(
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    listName = listName
                )
            }) {
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
