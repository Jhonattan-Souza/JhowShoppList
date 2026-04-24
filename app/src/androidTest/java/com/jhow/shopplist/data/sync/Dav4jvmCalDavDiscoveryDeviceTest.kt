package com.jhow.shopplist.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Dav4jvmCalDavDiscoveryDeviceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun findTaskCollections_usesMockServerInsteadOfHostCalDav() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setHeader("Content-Type", "application/xml")
                .setBody(
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
                )
        )

        val result = Dav4jvmCalDavDiscoveryService(VTodoMapper()).findTaskCollections(
            serverUrl = server.url("/").toString(),
            username = "testuser",
            password = "testpass123"
        )

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Groceries", result.single().displayName)
    }
}
