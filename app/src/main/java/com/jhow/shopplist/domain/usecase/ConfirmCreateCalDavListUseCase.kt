package com.jhow.shopplist.domain.usecase

import com.jhow.shopplist.data.sync.CalDavDiscoveryService
import com.jhow.shopplist.data.sync.CalDavAuthenticationException
import com.jhow.shopplist.domain.model.CalDavValidationResult
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject

open class ConfirmCreateCalDavListUseCase @Inject constructor(
    private val repository: CalDavConfigRepository,
    private val discoveryService: CalDavDiscoveryService
) {
    open suspend operator fun invoke(
        serverUrl: String,
        username: String,
        listName: String,
        password: String
    ): CalDavValidationResult {
        if (serverUrl.isBlank() || username.isBlank() || listName.isBlank()) {
            return CalDavValidationResult.ConfigurationError(
                message = "Server, username, and list name are required"
            )
        }

        val resolvedPassword = password.ifBlank { repository.getPassword().orEmpty() }
        if (resolvedPassword.isBlank()) {
            return CalDavValidationResult.ConfigurationError(message = "Password is required")
        }

        return try {
            val href = discoveryService.createTaskCollection(
                serverUrl = serverUrl,
                username = username,
                password = resolvedPassword,
                listName = listName
            )
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
