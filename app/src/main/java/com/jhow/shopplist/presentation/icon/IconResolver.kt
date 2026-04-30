package com.jhow.shopplist.presentation.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.jhow.shopplist.domain.icon.IconMatcher

class IconResolver(private val matcher: IconMatcher) {
    private val cache = mutableMapOf<String, ImageVector>()

    fun resolveIcon(itemName: String): ImageVector {
        return cache.getOrPut(itemName) {
            val bucket = matcher.match(itemName)
            BucketIcons.forBucket(bucket)
        }
    }
}
