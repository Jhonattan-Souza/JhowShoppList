package com.jhow.shopplist.presentation.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.WaterDrop
import com.jhow.shopplist.domain.icon.DefaultIconMatcher
import com.jhow.shopplist.domain.icon.DefaultTextNormalizer
import com.jhow.shopplist.domain.icon.IconBucket
import org.junit.Assert.assertEquals
import org.junit.Test

class IconResolverTest {

    private val dictionary = mapOf(
        "leite" to IconBucket.DAIRY,
        "milk" to IconBucket.DAIRY
    )
    private val matcher = DefaultIconMatcher(dictionary, DefaultTextNormalizer())
    private val resolver = IconResolver(matcher)

    @Test
    fun `resolves known item to its bucket icon`() {
        val icon = resolver.resolveIcon("leite")
        assertEquals(BucketIcons.forBucket(IconBucket.DAIRY), icon)
    }

    @Test
    fun `resolves unknown item to generic icon`() {
        val icon = resolver.resolveIcon("parafuso")
        assertEquals(BucketIcons.forBucket(IconBucket.GENERIC), icon)
    }

    @Test
    fun `memoizes repeated lookups`() {
        val icon1 = resolver.resolveIcon("leite")
        val icon2 = resolver.resolveIcon("leite")
        assertEquals(icon1, icon2)
    }

    @Test
    fun `clearing cache allows re resolution after dictionary update`() {
        val emptyMatcher = DefaultIconMatcher(emptyMap(), DefaultTextNormalizer())
        val dynamicResolver = IconResolver(emptyMatcher)

        val genericIcon = dynamicResolver.resolveIcon("leite")
        assertEquals(BucketIcons.forBucket(IconBucket.GENERIC), genericIcon)

        emptyMatcher.updateDictionary(mapOf("leite" to IconBucket.DAIRY))
        dynamicResolver.clearCache()

        val dairyIcon = dynamicResolver.resolveIcon("leite")
        assertEquals(BucketIcons.forBucket(IconBucket.DAIRY), dairyIcon)
    }

    @Test
    fun `clearing cache increments resolver version`() {
        val dynamicResolver = IconResolver(DefaultIconMatcher(emptyMap(), DefaultTextNormalizer()))

        assertEquals(0, dynamicResolver.version)

        dynamicResolver.clearCache()

        assertEquals(1, dynamicResolver.version)
    }
}
