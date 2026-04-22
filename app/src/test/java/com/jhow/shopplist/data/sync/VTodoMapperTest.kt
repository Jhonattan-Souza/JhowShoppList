package com.jhow.shopplist.data.sync

import com.jhow.shopplist.domain.model.ShoppingItem
import com.jhow.shopplist.domain.model.ShoppingItemRemoteMetadata
import com.jhow.shopplist.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VTodoMapperTest {

    @Test
    fun `toVTodo maps purchased item to completed task`() {
        val mapper = VTodoMapper()

        val payload = mapper.toVTodo(
            ShoppingItem(
                id = "coffee",
                name = "Coffee",
                isPurchased = true,
                purchaseCount = 4,
                createdAt = 10L,
                updatedAt = 20L,
                isDeleted = false,
                syncStatus = SyncStatus.PENDING_UPDATE,
                remoteMetadata = ShoppingItemRemoteMetadata(remoteUid = "uid-coffee")
            )
        )

        assertEquals("uid-coffee", payload.uid)
        assertTrue("Body should contain STATUS:COMPLETED", payload.body.contains("STATUS:COMPLETED"))
        assertTrue("Body should contain COMPLETED:", payload.body.contains("COMPLETED:"))
        assertTrue("Body should contain SUMMARY:Coffee", payload.body.contains("SUMMARY:Coffee"))
    }

    @Test
    fun `toVTodo generates uid when remoteUid is absent`() {
        val mapper = VTodoMapper()

        val payload = mapper.toVTodo(
            ShoppingItem(
                id = "tea",
                name = "Tea",
                isPurchased = false,
                purchaseCount = 0,
                createdAt = 10L,
                updatedAt = 20L,
                isDeleted = false,
                syncStatus = SyncStatus.PENDING_INSERT,
                remoteMetadata = ShoppingItemRemoteMetadata(remoteUid = null)
            )
        )

        assertTrue("Generated uid should not be blank", payload.uid.isNotBlank())
        assertTrue("Body should contain generated uid", payload.body.contains("UID:${payload.uid}"))
    }

    @Test
    fun `toVTodo maps unpurchased item to needs-action task`() {
        val mapper = VTodoMapper()

        val payload = mapper.toVTodo(
            ShoppingItem(
                id = "bread",
                name = "Bread",
                isPurchased = false,
                purchaseCount = 0,
                createdAt = 10L,
                updatedAt = 20L,
                isDeleted = false,
                syncStatus = SyncStatus.PENDING_INSERT,
                remoteMetadata = ShoppingItemRemoteMetadata(remoteUid = "uid-bread")
            )
        )

        assertEquals("uid-bread", payload.uid)
        assertTrue("Body should contain STATUS:NEEDS-ACTION", payload.body.contains("STATUS:NEEDS-ACTION"))
        assertTrue("Body should contain SUMMARY:Bread", payload.body.contains("SUMMARY:Bread"))
    }

    @Test
    fun `toRemoteSnapshot parses completed vtodo body into snapshot`() {
        val mapper = VTodoMapper()
        val body = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//JhowShoppList//EN")
            appendLine("BEGIN:VTODO")
            appendLine("UID:uid-apples")
            appendLine("SUMMARY:Apples")
            appendLine("STATUS:COMPLETED")
            appendLine("LAST-MODIFIED:19700101T000020Z")
            appendLine("END:VTODO")
            appendLine("END:VCALENDAR")
        }

        val snapshot = mapper.toRemoteSnapshot(body, href = "/lists/groceries/apples.ics", eTag = "etag-1")

        assertEquals("uid-apples", snapshot.remoteUid)
        assertEquals("Apples", snapshot.summary)
        assertEquals(true, snapshot.isCompleted)
        assertEquals("/lists/groceries/apples.ics", snapshot.href)
        assertEquals("etag-1", snapshot.eTag)
        assertEquals(20_000L, snapshot.lastModifiedAt)
    }

    @Test
    fun `toVTodo escapes summary text and toRemoteSnapshot unescapes it`() {
        val mapper = VTodoMapper()
        val name = "Milk; 2%, Bread, Cheese\\Crackers\nNapkins"

        val payload = mapper.toVTodo(
            ShoppingItem(
                id = "party",
                name = name,
                isPurchased = false,
                purchaseCount = 0,
                createdAt = 10L,
                updatedAt = 20L,
                isDeleted = false,
                syncStatus = SyncStatus.PENDING_INSERT,
                remoteMetadata = ShoppingItemRemoteMetadata(remoteUid = "uid-party")
            )
        )

        assertTrue(
            "SUMMARY must escape special chars",
            payload.body.contains("SUMMARY:Milk\\; 2%\\, Bread\\, Cheese\\\\Crackers\\nNapkins")
        )

        val snapshot = mapper.toRemoteSnapshot(payload.body, href = "/party.ics", eTag = null)
        assertEquals(name, snapshot.summary)
    }

    @Test
    fun `toRemoteSnapshot handles SUMMARY with property parameters`() {
        val mapper = VTodoMapper()
        val body = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//JhowShoppList//EN")
            appendLine("BEGIN:VTODO")
            appendLine("UID:uid-coffee")
            appendLine("SUMMARY;LANGUAGE=en:Coffee")
            appendLine("STATUS:NEEDS-ACTION")
            appendLine("LAST-MODIFIED:19700101T000020Z")
            appendLine("END:VTODO")
            appendLine("END:VCALENDAR")
        }

        val snapshot = mapper.toRemoteSnapshot(body, href = "/coffee.ics", eTag = null)

        assertEquals("Coffee", snapshot.summary)
    }

    @Test
    fun `toRemoteSnapshot returns null lastModifiedAt for malformed timestamp`() {
        val mapper = VTodoMapper()
        val body = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//JhowShoppList//EN")
            appendLine("BEGIN:VTODO")
            appendLine("UID:uid-bad")
            appendLine("SUMMARY:Bad Item")
            appendLine("STATUS:NEEDS-ACTION")
            appendLine("LAST-MODIFIED:not-a-timestamp")
            appendLine("END:VTODO")
            appendLine("END:VCALENDAR")
        }

        val snapshot = mapper.toRemoteSnapshot(body, href = "/bad.ics", eTag = null)

        assertEquals(null, snapshot.lastModifiedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toRemoteSnapshot throws IllegalArgumentException when UID is missing`() {
        val mapper = VTodoMapper()
        val body = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//JhowShoppList//EN")
            appendLine("BEGIN:VTODO")
            appendLine("SUMMARY:Milk")
            appendLine("STATUS:NEEDS-ACTION")
            appendLine("END:VTODO")
            appendLine("END:VCALENDAR")
        }

        mapper.toRemoteSnapshot(body, href = "/milk.ics", eTag = null)
    }
}
