package com.jhow.shopplist.data.sync

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Dav4jvmCalDavDiscoveryServiceTest {

    @Test
    fun `findTaskCollections returns named collections from DAV multistatus`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val responseBody =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <multistatus xmlns="DAV:">
              <response>
                <href>/</href>
                <propstat>
                  <prop>
                    <displayname>Root</displayname>
                    <resourcetype><collection/></resourcetype>
                  </prop>
                  <status>HTTP/1.1 200 OK</status>
                </propstat>
              </response>
              <response>
                <href>/groceries/</href>
                <propstat>
                  <prop>
                    <displayname>Groceries</displayname>
                    <resourcetype><collection/></resourcetype>
                  </prop>
                  <status>HTTP/1.1 200 OK</status>
                </propstat>
              </response>
            </multistatus>
            """.trimIndent()

        server.createContext("/") { exchange ->
            assertEquals("PROPFIND", exchange.requestMethod)
            exchange.responseHeaders.add("Content-Type", "application/xml")
            exchange.sendResponseHeaders(207, responseBody.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(responseBody.toByteArray()) }
        }
        server.start()

        try {
            val service = Dav4jvmCalDavDiscoveryService(VTodoMapper())

            val result = service.findTaskCollections(
                serverUrl = "http://127.0.0.1:${server.address.port}/",
                username = "testuser",
                password = "testpass123"
            )

            assertEquals(
                listOf(
                    CalDavCollectionCandidate(
                        displayName = "Groceries",
                        href = "http://127.0.0.1:${server.address.port}/groceries/"
                    )
                ),
                result
            )
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `createTaskCollection returns created collection url`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val requestBody = AtomicReference<String>()
        val contentType = AtomicReference<String?>()
        server.createContext("/Groceries/") { exchange ->
            assertEquals("MKCOL", exchange.requestMethod)
            requestBody.set(exchange.requestBody.use { it.readBytes().toString(Charsets.UTF_8) })
            contentType.set(exchange.requestHeaders.getFirst("Content-Type"))
            exchange.sendResponseHeaders(201, -1)
            exchange.close()
        }
        server.start()

        try {
            val service = Dav4jvmCalDavDiscoveryService(VTodoMapper())

            val href = service.createTaskCollection(
                serverUrl = "http://127.0.0.1:${server.address.port}/",
                username = "testuser",
                password = "testpass123",
                listName = "Groceries"
            )

            assertEquals("http://127.0.0.1:${server.address.port}/Groceries/", href)
            assertEquals("application/xml; charset=utf-8", contentType.get())
            assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<mkcol xmlns=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">" +
                    "<set><prop>" +
                    "<displayname>Groceries</displayname>" +
                    "<resourcetype><collection/><C:calendar/></resourcetype>" +
                    "<C:supported-calendar-component-set><C:comp name=\"VTODO\"/></C:supported-calendar-component-set>" +
                    "</prop></set></mkcol>",
                requestBody.get()
            )
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `createTaskCollection encodes list name into safe path segment`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val requestPath = AtomicReference<String>()
        server.createContext("/") { exchange ->
            requestPath.set(exchange.requestURI.rawPath)
            exchange.sendResponseHeaders(201, -1)
            exchange.close()
        }
        server.start()

        try {
            val service = Dav4jvmCalDavDiscoveryService(VTodoMapper())

            val href = service.createTaskCollection(
                serverUrl = "http://127.0.0.1:${server.address.port}/",
                username = "testuser",
                password = "testpass123",
                listName = "Groceries & Home/Weekly"
            )

            assertEquals("/Groceries%20%26%20Home%2FWeekly/", requestPath.get())
            assertEquals(
                "http://127.0.0.1:${server.address.port}/Groceries%20%26%20Home%2FWeekly/",
                href
            )
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchTaskItems returns parsed remote snapshots and skips malformed vtodos`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val depthHeader = AtomicReference<String?>()
        val contentType = AtomicReference<String?>()
        val requestBody = AtomicReference<String>()
        val responseBody =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <multistatus xmlns="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
              <response>
                <href>/Groceries/</href>
                <propstat>
                  <prop>
                    <getetag>"collection-etag"</getetag>
                  </prop>
                  <status>HTTP/1.1 200 OK</status>
                </propstat>
              </response>
              <response>
                <href>/Groceries/apples.ics</href>
                <propstat>
                  <prop>
                    <getetag>"etag-apples"</getetag>
                    <C:calendar-data><![CDATA[BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//JhowShoppList//EN
BEGIN:VTODO
UID:uid-apples
SUMMARY:Apples
STATUS:COMPLETED
LAST-MODIFIED:19700101T000020Z
END:VTODO
END:VCALENDAR
]]></C:calendar-data>
                  </prop>
                  <status>HTTP/1.1 200 OK</status>
                </propstat>
              </response>
              <response>
                <href>/Groceries/invalid.ics</href>
                <propstat>
                  <prop>
                    <getetag>"etag-invalid"</getetag>
                    <C:calendar-data><![CDATA[BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//JhowShoppList//EN
BEGIN:VTODO
SUMMARY:Missing UID
STATUS:NEEDS-ACTION
END:VTODO
END:VCALENDAR
]]></C:calendar-data>
                  </prop>
                  <status>HTTP/1.1 200 OK</status>
                </propstat>
              </response>
            </multistatus>
            """.trimIndent()

        server.createContext("/Groceries/") { exchange ->
            assertEquals("REPORT", exchange.requestMethod)
            depthHeader.set(exchange.requestHeaders.getFirst("Depth"))
            contentType.set(exchange.requestHeaders.getFirst("Content-Type"))
            requestBody.set(exchange.requestBody.use { it.readBytes().toString(Charsets.UTF_8) })
            exchange.responseHeaders.add("Content-Type", "application/xml")
            exchange.sendResponseHeaders(207, responseBody.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(responseBody.toByteArray()) }
        }
        server.start()

        try {
            val service = Dav4jvmCalDavDiscoveryService(VTodoMapper())

            val result = service.fetchTaskItems(
                serverUrl = "http://127.0.0.1:${server.address.port}/",
                username = "testuser",
                password = "testpass123",
                collectionHref = "/Groceries/"
            )

            assertEquals("1", depthHeader.get())
            assertEquals("application/xml; charset=utf-8", contentType.get())
            assertTrue(requestBody.get().contains("calendar-query"))
            assertEquals(1, result.size)
            assertEquals("uid-apples", result.single().remoteUid)
            assertEquals("Apples", result.single().summary)
            assertEquals(true, result.single().isCompleted)
            assertEquals("http://127.0.0.1:${server.address.port}/Groceries/apples.ics", result.single().href)
            assertEquals("\"etag-apples\"", result.single().eTag)
            assertEquals(20_000L, result.single().lastModifiedAt)
        } finally {
            server.stop(0)
        }
    }
}
