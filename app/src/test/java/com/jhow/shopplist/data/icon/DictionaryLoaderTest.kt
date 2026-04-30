package com.jhow.shopplist.data.icon

import com.jhow.shopplist.domain.icon.IconBucket
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.serialization.SerializationException

class DictionaryLoaderTest {

    @Test
    fun `loads and merges pt and en dictionaries end to end`() = runTest {
        val ptJson = """{"leite":"dairy","pao":"bread"}"""
        val enJson = """{"milk":"dairy","bread":"bread"}"""

        val loader = AssetDictionaryLoader { name ->
            when (name) {
                "icons/dictionary-pt.json" -> ptJson.byteInputStream()
                "icons/dictionary-en.json" -> enJson.byteInputStream()
                else -> error("Unknown asset: $name")
            }
        }

        val dictionary = loader.load()

        assertEquals(IconBucket.DAIRY, dictionary["leite"])
        assertEquals(IconBucket.BREAD, dictionary["pao"])
        assertEquals(IconBucket.DAIRY, dictionary["milk"])
        assertEquals(IconBucket.BREAD, dictionary["bread"])
    }

    @Test
    fun `en terms override pt terms on collision`() = runTest {
        val ptJson = """{"leite":"dairy"}"""
        val enJson = """{"leite":"bread"}"""

        val loader = AssetDictionaryLoader { name ->
            when (name) {
                "icons/dictionary-pt.json" -> ptJson.byteInputStream()
                "icons/dictionary-en.json" -> enJson.byteInputStream()
                else -> error("Unknown asset: $name")
            }
        }

        val dictionary = loader.load()

        assertEquals(IconBucket.BREAD, dictionary["leite"])
    }

    @Test
    fun `unknown bucket values throw illegal argument exception`() = runTest {
        val ptJson = """{"leite":"dairy","unknown":"not-a-bucket"}"""
        val enJson = """{}"""

        val loader = AssetDictionaryLoader { name ->
            when (name) {
                "icons/dictionary-pt.json" -> ptJson.byteInputStream()
                "icons/dictionary-en.json" -> enJson.byteInputStream()
                else -> error("Unknown asset: $name")
            }
        }

        try {
            loader.load()
            fail("Expected loader.load() to throw IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `caches result so second load does not re read assets`() = runTest {
        var openCount = 0
        val ptJson = """{"leite":"dairy"}"""
        val enJson = """{"milk":"dairy"}"""

        val loader = AssetDictionaryLoader { name ->
            openCount++
            when (name) {
                "icons/dictionary-pt.json" -> ptJson.byteInputStream()
                "icons/dictionary-en.json" -> enJson.byteInputStream()
                else -> error("Unknown asset: $name")
            }
        }

        loader.load()
        loader.load()

        assertEquals(2, openCount)
    }

    @Test
    fun `concurrent first loads only read each asset once`() = runTest {
        var openCount = 0
        val gate = CompletableDeferred<Unit>()
        val ptJson = """{"leite":"dairy"}"""
        val enJson = """{"milk":"dairy"}"""

        val loader = AssetDictionaryLoader { name ->
            gate.await()
            openCount++
            when (name) {
                "icons/dictionary-pt.json" -> ptJson.byteInputStream()
                "icons/dictionary-en.json" -> enJson.byteInputStream()
                else -> error("Unknown asset: $name")
            }
        }

        val deferredLoads = List(2) { async { loader.load() } }
        gate.complete(Unit)

        deferredLoads.awaitAll()

        assertEquals(2, openCount)
    }

    @Test
    fun `hyphenated bucket ids map to underscore enum names`() {
        val ptJson = """{"arroz":"pantry-canned"}"""
        val enJson = """{}"""
        val result = parseDictionaryJson(ptJson) + parseDictionaryJson(enJson)
        assertEquals(IconBucket.PANTRY_CANNED, result["arroz"])
    }

    @Test
    fun `unicode escaped keys are decoded`() {
        val cedilla = "\\" + "u00e7"
        val tilde = "\\" + "u00e3"
        val escapedJson = "{\"ma${cedilla}${tilde}\":\"fruit\",\"p${tilde}o\":\"bread\"}"

        val result = parseDictionaryJson(escapedJson)

        assertEquals(IconBucket.FRUIT, result["maçã"])
        assertEquals(IconBucket.BREAD, result["pão"])
    }

    @Test
    fun `malformed json throws serialization exception`() {
        assertThrows(SerializationException::class.java) {
            parseDictionaryJson("{\"leite\":")
        }
    }

    @Test
    fun `non object json throws illegal argument exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseDictionaryJson("[\"leite\"]")
        }
    }

    @Test
    fun `non primitive values throw illegal argument exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseDictionaryJson("{\"leite\":{\"bucket\":\"dairy\"}}")
        }
    }
}
