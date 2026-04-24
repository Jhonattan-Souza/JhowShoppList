package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.core.dispatchers.IoDispatcher
import com.jhow.shopplist.data.sync.CalDavAuthenticationException
import com.jhow.shopplist.data.sync.CalDavDiscoveryService
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

open class ConfirmCreateCalDavListUseCase @Inject constructor(
    private val repository: CalDavConfigRepository,
    private val discoveryService: CalDavDiscoveryService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    open suspend operator fun invoke(
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    ): CalDavValidationResult {
        val resolvedPassword = password.ifBlank { repository.getPassword().orEmpty() }
        return validateInputs(serverUrl, username, listName, resolvedPassword)
            ?: createListAndMapResult(serverUrl, username, resolvedPassword, listName)
    }

    private fun validateInputs(
        serverUrl: String,
        username: String,
        listName: String,
        resolvedPassword: String
    ): CalDavValidationResult? = when {
        serverUrl.isBlank() || username.isBlank() || listName.isBlank() ->
            CalDavValidationResult.ConfigurationError(
                message = "Server, username, and list name are required"
            )
        resolvedPassword.isBlank() ->
            CalDavValidationResult.ConfigurationError(message = "Password is required")
        else -> null
    }

    private suspend fun createListAndMapResult(
        serverUrl: String,
        username: String,
        password: String,
        listName: String
    ): CalDavValidationResult {
        return try {
            val href = withContext(ioDispatcher) {
                discoveryService.createTaskCollection(
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    listName = listName
                )
            }
            CalDavValidationResult.Success(href)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: CalDavAuthenticationException) {
            CalDavValidationResult.AuthError(message = "Authentication failed")
        } catch (_: Exception) {
            CalDavValidationResult.NetworkError(message = "Unable to create remote list")
        }
    }
}
