package com.jhow.shopplist.data.sync

import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.exception.ForbiddenException
import at.bitfire.dav4jvm.exception.UnauthorizedException
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.ResourceType
import com.jhow.shopplist.domain.model.RemoteShoppingItemSnapshot
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
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
                    .method("MKCOL", createCollectionRequestBody(listName))
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

    override suspend fun fetchTaskItems(
        serverUrl: String,
        username: String,
        password: String,
        collectionHref: String
    ): List<RemoteShoppingItemSnapshot> {
        val collectionUrl = resolveCollectionUrl(serverUrl, collectionHref)
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
                    207 -> response.body?.string().orEmpty()
                    401, 403 -> throw CalDavAuthenticationException()
                    else -> error("REPORT failed with HTTP ${response.code}")
                }
            }

        return parseTaskItems(responseBody = responseBody, collectionUrl = collectionUrl)
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

    private fun resolveCollectionUrl(serverUrl: String, collectionHref: String) =
        normalizeBaseUrl(serverUrl).resolve(collectionHref)
            ?: error("Invalid collection URL")

    private fun listPath(listName: String): String {
        val sanitizedName = listName.trim().trim('/').ifBlank { error("List name is required") }
        val encodedSegment = URLEncoder.encode(sanitizedName, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "$encodedSegment/"
    }

    private fun createCollectionRequestBody(listName: String) =
        (
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<mkcol xmlns=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">" +
                "<set><prop>" +
                "<displayname>${escapeXml(listName.trim())}</displayname>" +
                "<resourcetype><collection/><C:calendar/></resourcetype>" +
                "<C:supported-calendar-component-set><C:comp name=\"VTODO\"/></C:supported-calendar-component-set>" +
                "</prop></set></mkcol>"
            )
            .toRequestBody(CALDAV_XML_MEDIA_TYPE)

    private fun calendarQueryRequestBody() =
        (
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<C:calendar-query xmlns=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">" +
                "<prop><getetag/><C:calendar-data/></prop>" +
                "<C:filter><C:comp-filter name=\"VCALENDAR\"><C:comp-filter name=\"VTODO\"/></C:comp-filter></C:filter>" +
                "</C:calendar-query>"
            )
            .toRequestBody(CALDAV_XML_MEDIA_TYPE)

    private fun parseTaskItems(
        responseBody: String,
        collectionUrl: okhttp3.HttpUrl
    ): List<RemoteShoppingItemSnapshot> {
        if (responseBody.isBlank()) return emptyList()

        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(responseBody.trimStart().byteInputStream())
        val responses = document.getElementsByTagNameNS(DAV_NAMESPACE, "response")
        val items = mutableListOf<RemoteShoppingItemSnapshot>()

        for (index in 0 until responses.length) {
            val response = responses.item(index) as? Element ?: continue
            val href = response.firstChildText(DAV_NAMESPACE, "href")?.trim().orEmpty()
            if (href.isBlank()) continue

            val normalizedHref = collectionUrl.resolve(href)?.toString() ?: href
            if (normalizedHref == collectionUrl.toString()) continue

            val eTag = response.firstDescendantText(DAV_NAMESPACE, "getetag")?.trim()
            val calendarData = response.firstDescendantText(CALDAV_NAMESPACE, "calendar-data")?.trim().orEmpty()
            if (calendarData.isBlank()) continue

            val snapshot = try {
                mapper.toRemoteSnapshot(calendarData, href = normalizedHref, eTag = eTag)
            } catch (_: IllegalArgumentException) {
                null
            }

            if (snapshot != null) {
                items += snapshot
            }
        }

        return items
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

    private fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { character ->
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

    private fun collectionPropertyName(): Property.Name = ResourceType.Companion.COLLECTION

    private companion object {
        const val DAV_NAMESPACE = "DAV:"
        const val CALDAV_NAMESPACE = "urn:ietf:params:xml:ns:caldav"
        const val CALDAV_XML_MEDIA_TYPE_VALUE = "application/xml; charset=utf-8"
        val CALDAV_XML_MEDIA_TYPE = CALDAV_XML_MEDIA_TYPE_VALUE.toMediaType()
    }
}
