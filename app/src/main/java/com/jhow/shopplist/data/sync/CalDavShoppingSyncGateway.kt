package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import com.jhow.shopplist.domain.sync.CalDavSyncConfig
import com.jhow.shopplist.domain.sync.ShoppingListSyncGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CalDavShoppingSyncGateway @Inject constructor(
    private val configRepository: CalDavConfigRepository,
    private val listLocator: CalDavListLocator,
    private val executor: CalDavSyncExecutor,
    private val discoveryService: CalDavDiscoveryService
) : ShoppingListSyncGateway {

    override suspend fun sync(): CalDavSyncOutcome {
        val config = configRepository.observeConfig().first()
        return if (!config.isReadyToSync) {
            CalDavSyncOutcome.ConfigurationFailure("Sync is disabled or incomplete")
        } else {
            performSync(config)
        }
    }

    private suspend fun performSync(config: CalDavSyncConfig): CalDavSyncOutcome {
        val password = configRepository.getPassword().orEmpty()
        return when (
            val located = listLocator.locate(
                serverUrl = config.serverUrl,
                username = config.username,
                password = password,
                listName = config.listName
            )
        ) {
            is CalDavListLocator.Result.Missing -> handleMissingList(config, password)
            is CalDavListLocator.Result.Ambiguous -> CalDavSyncOutcome.ConfigurationFailure(
                "Multiple lists named ${config.listName} were found"
            )
            is CalDavListLocator.Result.Found -> {
                configRepository.setResolvedCollectionUrl(located.href)
                executor.execute(
                    serverUrl = config.serverUrl,
                    username = config.username,
                    password = password,
                    collectionHref = located.href
                )
            }
        }
    }

    private suspend fun handleMissingList(
        config: CalDavSyncConfig,
        password: String
    ): CalDavSyncOutcome {
        if (!config.createListRequested) {
            return CalDavSyncOutcome.UserActionRequired(
                message = "Remote list ${config.listName} is missing",
                pendingAction = CalDavPendingAction.CreateMissingList
            )
        }

        val href = tryCreateAndResolve(config, password)
        return if (href != null) {
            configRepository.setResolvedCollectionUrl(href)
            configRepository.setCreateListRequested(false)
            executor.execute(
                serverUrl = config.serverUrl,
                username = config.username,
                password = password,
                collectionHref = href
            )
        } else {
            CalDavSyncOutcome.ConfigurationFailure(
                "Multiple lists named ${config.listName} were found"
            )
        }
    }

    private suspend fun tryCreateAndResolve(
        config: CalDavSyncConfig,
        password: String
    ): String? {
        val createResult = runCatching {
            discoveryService.createTaskCollection(
                serverUrl = config.serverUrl,
                username = config.username,
                password = password,
                listName = config.listName
            )
        }
        return createResult.getOrNull() ?: run {
            val createError = createResult.exceptionOrNull()!!
            when (
                val retryLocated = listLocator.locate(
                    serverUrl = config.serverUrl,
                    username = config.username,
                    password = password,
                    listName = config.listName
                )
            ) {
                is CalDavListLocator.Result.Found -> retryLocated.href
                is CalDavListLocator.Result.Missing -> throw createError
                is CalDavListLocator.Result.Ambiguous -> null
            }
        }
    }
}
