package com.jhow.shopplist.data.sync

import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.exception.ForbiddenException
import at.bitfire.dav4jvm.exception.UnauthorizedException
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.ResourceType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request


@Singleton
class Dav4jvmCalDavDiscoveryService @Inject constructor() : CalDavDiscoveryService {

    override suspend fun findTaskCollections(
        serverUrl: String,
        username: String,
        password: String
    ): List<CalDavCollectionCandidate> {
        val baseUrl = normalizeBaseUrl(serverUrl)
        val resource = DavResource(httpClient(username, password), baseUrl)
        val candidates = mutableListOf<CalDavCollectionCandidate>()

        try {
            resource.propfind(
                1,
                DisplayName.NAME,
                ResourceType.NAME
            ) { response, _ ->
                val displayName = response.get(DisplayName::class.java)?.displayName?.trim().orEmpty()
                val resourceType = response.get(ResourceType::class.java)
                val isCollection = resourceType?.types?.contains(collectionPropertyName()) == true
                val isBaseCollection = response.href.toString() == baseUrl.toString()

                if (displayName.isNotEmpty() && isCollection && !isBaseCollection) {
                    candidates += CalDavCollectionCandidate(
                        displayName = displayName,
                        href = response.href.toString()
                    )
                }
            }
        } catch (exception: UnauthorizedException) {
            throw CalDavAuthenticationException()
        } catch (exception: ForbiddenException) {
            throw CalDavAuthenticationException()
        }

        return candidates
    }

    override suspend fun createTaskCollection(
        serverUrl: String,
        username: String,
        password: String,
        listName: String
    ): String {
        val baseUrl = normalizeBaseUrl(serverUrl)
        val collectionUrl = requireNotNull(baseUrl.resolve(listPath(listName))) {
            "Invalid collection URL"
        }

        httpClient(username, password)
            .newCall(
                Request.Builder()
                    .url(collectionUrl)
                    .method("MKCOL", null)
                    .build()
            )
            .execute()
            .use { response ->
                when (response.code) {
                    201, 405 -> Unit
                    401, 403 -> throw CalDavAuthenticationException()
                    else -> error("MKCOL failed with HTTP ${response.code}")
                }
            }

        return collectionUrl.toString()
    }

    private fun httpClient(username: String, password: String): OkHttpClient {
        val authHandler = BasicDigestAuthHandler(
            domain = null,
            username = username,
            password = password,
            insecurePreemptive = true
        )

        return OkHttpClient.Builder()
            .addInterceptor(authHandler)
            .authenticator(authHandler)
            .followRedirects(false)
            .build()
    }

    private fun normalizeBaseUrl(serverUrl: String) =
        serverUrl
            .trim()
            .let { if (it.endsWith('/')) it else "$it/" }
            .toHttpUrl()

    private fun listPath(listName: String): String {
        val sanitizedName = listName.trim().trim('/').ifBlank { error("List name is required") }
        val encodedSegment = URLEncoder.encode(sanitizedName, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "$encodedSegment/"
    }

    private fun collectionPropertyName(): Property.Name = ResourceType.Companion.COLLECTION
}
