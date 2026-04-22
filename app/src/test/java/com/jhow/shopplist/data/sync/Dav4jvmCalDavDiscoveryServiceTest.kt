package com.jhow.shopplist.data.sync

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            val service = Dav4jvmCalDavDiscoveryService()

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
        val contentLength = AtomicReference<Long>()
        val contentLengthHeader = AtomicReference<String?>()
        val contentType = AtomicReference<String?>()
        server.createContext("/Groceries/") { exchange ->
            assertEquals("MKCOL", exchange.requestMethod)
            contentLength.set(exchange.requestBody.use { it.readAllBytes().size.toLong() })
            contentLengthHeader.set(exchange.requestHeaders.getFirst("Content-Length"))
            contentType.set(exchange.requestHeaders.getFirst("Content-Type"))
            exchange.sendResponseHeaders(201, -1)
            exchange.close()
        }
        server.start()

        try {
            val service = Dav4jvmCalDavDiscoveryService()

            val href = service.createTaskCollection(
                serverUrl = "http://127.0.0.1:${server.address.port}/",
                username = "testuser",
                password = "testpass123",
                listName = "Groceries"
            )

            assertEquals("http://127.0.0.1:${server.address.port}/Groceries/", href)
            assertEquals(0L, contentLength.get())
            assertNull(contentLengthHeader.get())
            assertNull(contentType.get())
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
            val service = Dav4jvmCalDavDiscoveryService()

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
}
