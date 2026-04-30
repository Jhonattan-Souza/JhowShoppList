package com.jhow.shopplist.data.icon

import com.jhow.shopplist.domain.icon.IconBucket
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
private val dictionaryJson = Json {
    ignoreUnknownKeys = true
    allowComments = true
}

interface DictionaryLoader {
    suspend fun load(): Map<String, IconBucket>
}

class AssetDictionaryLoader(
    private val openAsset: suspend (String) -> InputStream
) : DictionaryLoader {

    @Volatile
    private var cachedDictionary: Map<String, IconBucket>? = null
    private val loadMutex = Mutex()

    override suspend fun load(): Map<String, IconBucket> {
        cachedDictionary?.let { return it }

        return withContext(Dispatchers.IO) {
            loadMutex.withLock {
                cachedDictionary?.let { return@withContext it }

                val pt = openAsset("icons/dictionary-pt.json").use {
                    parseDictionaryJson(it.bufferedReader().readText())
                }
                val en = openAsset("icons/dictionary-en.json").use {
                    parseDictionaryJson(it.bufferedReader().readText())
                }

                val merged = pt + en
                cachedDictionary = merged
                merged
            }
        }
    }
}

internal fun parseDictionaryJson(json: String): Map<String, IconBucket> {
    val parsedElement = dictionaryJson.parseToJsonElement(json)
    val dictionaryObject = parsedElement.asJsonObject()

    return buildMap {
        dictionaryObject.forEach { (key, value) ->
            val bucketValue = value.jsonPrimitive.content.uppercase().replace("-", "_")
            val bucket = runCatching { IconBucket.valueOf(bucketValue) }.getOrElse {
                throw IllegalArgumentException("Unknown icon bucket '$bucketValue' for term '$key'", it)
            }
            put(key, bucket)
        }
    }
}

private fun JsonElement.asJsonObject() = runCatching {
    jsonObject
}.getOrElse {
    throw IllegalArgumentException("Icon dictionary JSON must be an object", it)
}
