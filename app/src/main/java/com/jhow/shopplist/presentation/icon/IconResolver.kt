package com.jhow.shopplist.presentation.icon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.jhow.shopplist.domain.icon.IconMatcher

class IconResolver(private val matcher: IconMatcher) {
    /** Main-thread only. */
    private val cache = mutableMapOf<String, ImageVector>()
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
