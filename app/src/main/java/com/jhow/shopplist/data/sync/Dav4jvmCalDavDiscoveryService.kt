package com.jhow.shopplist.data.sync

import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.exception.ForbiddenException
import at.bitfire.dav4jvm.exception.UnauthorizedException
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.ResourceType
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import com.jhow.shopplist.domain.model.ShoppingItem
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.w3c.dom.Element
import org.w3c.dom.NodeList


@Singleton
class Dav4jvmCalDavDiscoveryService @Inject constructor(
    private val mapper: VTodoMapper
) : CalDavDiscoveryService {

    override suspend fun findTaskCollections(
        serverUrl: String,
        username: String,
        password: String
    ): List<CalDavCollectionCandidate> {
        val baseUrl = serverUrl.normalizeBaseUrl()
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
                val isCollection = resourceType?.types?.contains(COLLECTION_PROPERTY_NAME) == true
                val isBaseCollection = response.href.toString() == baseUrl.toString()

                if (displayName.isNotEmpty() && isCollection && !isBaseCollection) {
                    candidates += CalDavCollectionCandidate(
                        displayName = displayName,
                        href = response.href.toString()
                    )
                }
            }
        } catch (exception: UnauthorizedException) {
            throw exception.toAuthenticationException()
        } catch (exception: ForbiddenException) {
            throw exception.toAuthenticationException()
        }

        return candidates
    }

    override suspend fun createTaskCollection(
        serverUrl: String,
        username: String,
        password: String,
        listName: String
    ): String {
        val baseUrl = serverUrl.normalizeBaseUrl()
        val collectionUrl = requireNotNull(baseUrl.resolve(listName.toListPath())) {
            "Invalid collection URL"
        }

        httpClient(username, password)
            .newCall(
                Request.Builder()
                    .url(collectionUrl)
                    .method("MKCOL", listName.createCollectionRequestBody())
                    .build()
            )
            .execute()
            .use { response ->
                when (response.code) {
                    HTTP_CREATED, HTTP_METHOD_NOT_ALLOWED -> Unit
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw CalDavAuthenticationException()
                    else -> error("MKCOL failed with HTTP ${response.code}")
                }
            }

        return collectionUrl.toString()
    }

    override suspend fun fetchTaskItems(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String
    ): List<RemoteShoppingItemSnapshot> {
        val collectionUrl = serverUrl.resolveCollectionUrl(collectionHref)
        val responseBody = httpClient(username, password)
            .newCall(
                Request.Builder()
                    .url(collectionUrl)
                    .header("Depth", "1")
                    .method("REPORT", calendarQueryRequestBody())
                    .build()
            )
            .execute()
            .use { response ->
                when (response.code) {
                    HTTP_MULTI_STATUS -> response.body?.string().orEmpty()
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw CalDavAuthenticationException()
                    else -> error("REPORT failed with HTTP ${response.code}")
                }
            }

        return responseBody.parseTaskItems(collectionUrl = collectionUrl)
    }

    override suspend fun upsertTaskItem(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String,
        item: ShoppingItem
    ): CalDavTaskUpsertResult {
        val collectionUrl = serverUrl.resolveCollectionUrl(collectionHref)
        val payload = mapper.toVTodo(item)
        val itemUrl = item.remoteMetadata.remoteHref
            ?.let(serverUrl::resolveItemUrl)
            ?: requireNotNull(collectionUrl.resolve("${payload.uid}.ics")) {
                "Invalid task item URL"
            }

        return httpClient(username, password)
            .newCall(
                Request.Builder()
                    .url(itemUrl)
                    .apply {
                        item.remoteMetadata.remoteEtag?.let { header("If-Match", it) }
                    }
                    .method("PUT", payload.body.toRequestBody(TEXT_CALENDAR_MEDIA_TYPE))
                    .build()
            )
            .execute()
            .use { response ->
                when (response.code) {
                    HTTP_OK, HTTP_CREATED, HTTP_NO_CONTENT -> CalDavTaskUpsertResult(
                        remoteUid = payload.uid,
                        href = itemUrl.toString(),
                        eTag = response.header("ETag") ?: item.remoteMetadata.remoteEtag,
                        lastModifiedAt = response.header("Last-Modified")?.parseHttpDate() ?: item.updatedAt
                    )

                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw CalDavAuthenticationException()
                    else -> error("PUT failed with HTTP ${response.code}")
                }
            }
    }

    override suspend fun deleteTaskItem(
        serverUrl: String,
        username: String,
        password: String,
        href: String,
        eTag: String?
    ): CalDavTaskDeleteResult {
        val itemUrl = serverUrl.resolveItemUrl(href)
        return httpClient(username, password)
            .newCall(
                Request.Builder()
                    .url(itemUrl)
                    .apply {
                        eTag?.let { header("If-Match", it) }
                    }
                    .delete()
                    .build()
            )
            .execute()
            .use { response ->
                when (response.code) {
                    HTTP_OK, HTTP_ACCEPTED, HTTP_NO_CONTENT, HTTP_NOT_FOUND -> CalDavTaskDeleteResult(
                        deletedAt = response.header("Last-Modified")?.parseHttpDate()
                    )

                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw CalDavAuthenticationException()
                    else -> error("DELETE failed with HTTP ${response.code}")
                }
            }
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

    private fun String.parseTaskItems(collectionUrl: HttpUrl): List<RemoteShoppingItemSnapshot> {
        if (isBlank()) return emptyList()

        return parseResponseElements().mapNotNull { element ->
            element.toRemoteSnapshotOrNull(collectionUrl)
        }.toList()
    }

    private fun String.parseResponseElements(): Sequence<Element> {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(trimStart().byteInputStream())

        return document
            .getElementsByTagNameNS(DAV_NAMESPACE, "response")
            .toElementSequence()
    }

    private fun Element.toRemoteSnapshotOrNull(collectionUrl: HttpUrl): RemoteShoppingItemSnapshot? {
        val normalizedHref = normalizedItemHref(collectionUrl) ?: return null
        val calendarData =
            firstDescendantText(CALDAV_NAMESPACE, "calendar-data")
                ?.trim()
                .orEmpty()
        if (calendarData.isBlank()) return null

        val eTag = firstDescendantText(DAV_NAMESPACE, "getetag")?.trim()
        return runCatching {
            mapper.toRemoteSnapshot(calendarData, href = normalizedHref, eTag = eTag)
        }.getOrNull()
    }

    private fun Element.normalizedItemHref(collectionUrl: HttpUrl): String? {
        val href = firstChildText(DAV_NAMESPACE, "href")?.trim().orEmpty()
        if (href.isBlank()) return null

        val normalizedHref = collectionUrl.resolve(href)?.toString() ?: href
        return normalizedHref.takeUnless { it == collectionUrl.toString() }
    }

    private fun Element.firstChildText(namespace: String, localName: String): String? =
        childNodes.toElementSequence().firstNotNullOfOrNull { child ->
            if (child.namespaceURI == namespace && child.localName == localName) child.textContent else null
        }

    private fun Element.firstDescendantText(namespace: String, localName: String): String? =
        getElementsByTagNameNS(namespace, localName).item(0)?.textContent

    private fun NodeList.toElementSequence(): Sequence<Element> = sequence {
        for (index in 0 until length) {
            val node = item(index)
            if (node is Element) {
                yield(node)
            }
        }
    }

    private fun Exception.toAuthenticationException(): CalDavAuthenticationException =
        CalDavAuthenticationException().also { authenticationException ->
            authenticationException.initCause(this)
        }

    private companion object {
        const val DAV_NAMESPACE = "DAV:"
        const val CALDAV_NAMESPACE = "urn:ietf:params:xml:ns:caldav"
        const val HTTP_CREATED = 201
        const val HTTP_ACCEPTED = 202
        const val HTTP_NO_CONTENT = 204
        const val HTTP_MULTI_STATUS = 207
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_METHOD_NOT_ALLOWED = 405
        val COLLECTION_PROPERTY_NAME: Property.Name = ResourceType.Companion.COLLECTION
    }
}

private fun String.normalizeBaseUrl(): HttpUrl =
    trim()
        .let { if (it.endsWith('/')) it else "$it/" }
        .toHttpUrl()

private fun String.resolveCollectionUrl(collectionHref: String): HttpUrl =
    normalizeBaseUrl().resolve(collectionHref)
        ?: error("Invalid collection URL")

private fun String.resolveItemUrl(itemHref: String): HttpUrl =
    runCatching { itemHref.toHttpUrl() }
        .getOrElse {
            normalizeBaseUrl().resolve(itemHref)
                ?: error("Invalid item URL")
        }

private fun String.toListPath(): String {
    val sanitizedName = trim().trim('/').ifBlank { error("List name is required") }
    val encodedSegment = URLEncoder.encode(sanitizedName, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
    return "$encodedSegment/"
}

private fun String.createCollectionRequestBody(): okhttp3.RequestBody {
    val displayName = trim().ifBlank { error("List name is required") }.escapeXml()
    return (
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<mkcol xmlns=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">" +
            "<set><prop>" +
            "<displayname>$displayName</displayname>" +
            "<resourcetype><collection/><C:calendar/></resourcetype>" +
            "<C:supported-calendar-component-set><C:comp name=\"VTODO\"/></C:supported-calendar-component-set>" +
            "</prop></set></mkcol>"
        )
        .toRequestBody(CALDAV_XML_MEDIA_TYPE)
}

private fun Request.Builder.delete(): Request.Builder = method("DELETE", null as RequestBody?)

private fun calendarQueryRequestBody() =
    (
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<C:calendar-query xmlns=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">" +
            "<prop><getetag/><C:calendar-data/></prop>" +
            "<C:filter>" +
            "<C:comp-filter name=\"VCALENDAR\">" +
            "<C:comp-filter name=\"VTODO\"/>" +
            "</C:comp-filter>" +
            "</C:filter>" +
            "</C:calendar-query>"
        )
        .toRequestBody(CALDAV_XML_MEDIA_TYPE)

private fun String.escapeXml(): String = buildString(this.length) {
    this@escapeXml.forEach { character ->
        append(
            when (character) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&apos;"
                else -> character.toString()
            }
        )
    }
}

private fun String.parseHttpDate(): Long? =
    runCatching {
        ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.getOrNull()

private val CALDAV_XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
private val TEXT_CALENDAR_MEDIA_TYPE = "text/calendar; charset=utf-8".toMediaType()
