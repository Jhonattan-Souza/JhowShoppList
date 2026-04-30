package com.jhow.shopplist.presentation.icon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.jhow.shopplist.domain.icon.IconMatcher

private const val DEFAULT_CACHE_SIZE = 500
private const val INITIAL_CAPACITY = 64
private const val LOAD_FACTOR = 0.75f

class IconResolver(
    private val matcher: IconMatcher,
    private val maxCacheSize: Int = DEFAULT_CACHE_SIZE
) {
    /** Main-thread only. */
    private val cache: LinkedHashMap<String, ImageVector> =
        object : LinkedHashMap<String, ImageVector>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageVector>?) =
                size > maxCacheSize
        }

    private var cacheVersionState by mutableIntStateOf(0)

    val version: Int
        get() = cacheVersionState

    fun resolveIcon(itemName: String): ImageVector {
        return cache.getOrPut(itemName) {
            val bucket = matcher.match(itemName)
            BucketIcons.forBucket(bucket)
        }
    }

    fun clearCache() {
        cache.clear()
        cacheVersionState += 1
    }
}
