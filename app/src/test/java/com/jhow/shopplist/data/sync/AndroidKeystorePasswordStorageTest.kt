package com.jhow.shopplist.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidKeystorePasswordStorageTest {

    @Test
    fun `decodeUtf8AndClear returns string and zeroes bytes`() {
        val bytes = "secret".toByteArray(Charsets.UTF_8)

        val result = bytes.decodeUtf8AndClear()

        assertEquals("secret", result)
        assertTrue(bytes.all { it == 0.toByte() })
    }

    @Test
    fun `loadPasswordOrNull clears secure payload and returns null on decrypt failure`() {
        var cleared = false

        val result = loadPasswordOrNull(
            saved = PasswordEntry(cipherText = "cipher", iv = "iv"),
            clearStoredPassword = { cleared = true },
            decrypt = { throw IllegalStateException("boom") }
        )

        assertNull(result)
        assertTrue(cleared)
    }
}
