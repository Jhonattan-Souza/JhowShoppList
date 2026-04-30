package com.jhow.shopplist.presentation.icon

import com.jhow.shopplist.domain.icon.DefaultIconMatcher
import com.jhow.shopplist.domain.icon.DefaultTextNormalizer
import com.jhow.shopplist.domain.icon.IconBucket
import com.jhow.shopplist.domain.icon.IconMatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class IconResolverTest {

    private val dictionary = mapOf(
        "leite" to IconBucket.DAIRY,
        "milk" to IconBucket.DAIRY,
        "arroz" to IconBucket.PANTRY_CANNED,
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

    @Test
    fun `lru cache does not call matcher again for cached entry`() {
        val counting = CountingMatcher(matcher)
        val r = IconResolver(counting)

        r.resolveIcon("leite")
        r.resolveIcon("leite")

        assertEquals("cache hit should not call matcher again", 1, counting.callCount)
    }

    @Test
    fun `lru evicts least recently used entry when capacity exceeded`() {
        val counting = CountingMatcher(matcher)
        val r = IconResolver(counting, maxCacheSize = 2)

        r.resolveIcon("leite")   // miss; count=1; cache: [leite]
        r.resolveIcon("milk")    // miss; count=2; cache order: leite(LRU) < milk(MRU)
        assertEquals(2, counting.callCount)

        // Promote leite to MRU so milk becomes the new LRU
        r.resolveIcon("leite")   // hit; order: milk(LRU) < leite(MRU)
        assertEquals("cache hit should not call matcher", 2, counting.callCount)

        // Adding arroz evicts milk (LRU), not leite (MRU)
        r.resolveIcon("arroz")   // miss; count=3; evicts milk; cache: [leite, arroz]
        assertEquals(3, counting.callCount)

        // leite survived the eviction (it was MRU before arroz was added)
        r.resolveIcon("leite")   // hit; count stays 3
        assertEquals("leite was MRU and should still be cached", 3, counting.callCount)

        // milk was evicted and must be re-resolved
        r.resolveIcon("milk")    // miss; count=4
        assertEquals("milk was evicted and requires a new matcher call", 4, counting.callCount)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private class CountingMatcher(
        private val delegate: IconMatcher,
        var callCount: Int = 0
    ) : IconMatcher {
        override fun match(itemName: String): IconBucket {
            callCount++
            return delegate.match(itemName)
        }
        override fun updateDictionary(newDictionary: Map<String, IconBucket>) =
            delegate.updateDictionary(newDictionary)
    }
}
