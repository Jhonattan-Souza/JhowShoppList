package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.CalDavPendingAction
import com.jhow.shopplist.domain.model.CalDavSyncOutcome
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
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
        if (!config.isReadyToSync) {
            return CalDavSyncOutcome.ConfigurationFailure("Sync is disabled or incomplete")
        }

        val password = configRepository.getPassword().orEmpty()
        return when (
            val located = listLocator.locate(
                serverUrl = config.serverUrl,
                username = config.username,
                password = password,
                listName = config.listName
            )
        ) {
            is CalDavListLocator.Result.Missing -> if (config.createListRequested) {
                val href = try {
                    discoveryService.createTaskCollection(
                        serverUrl = config.serverUrl,
                        username = config.username,
                        password = password,
                        listName = config.listName
                    )
                } catch (createError: Throwable) {
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
                        is CalDavListLocator.Result.Ambiguous -> return CalDavSyncOutcome.ConfigurationFailure(
                            "Multiple lists named ${config.listName} were found"
                        )
                    }
                }
                configRepository.setResolvedCollectionUrl(href)
                configRepository.setCreateListRequested(false)
                executor.execute(
                    serverUrl = config.serverUrl,
                    username = config.username,
                    password = password,
                    collectionHref = href
                )
            } else {
                CalDavSyncOutcome.UserActionRequired(
                    message = "Remote list ${config.listName} is missing",
                    pendingAction = CalDavPendingAction.CreateMissingList
                )
            }
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
}
