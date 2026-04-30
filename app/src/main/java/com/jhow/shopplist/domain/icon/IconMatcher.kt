package com.jhow.shopplist.domain.icon

interface IconMatcher {
    fun match(itemName: String): IconBucket
}

class DefaultIconMatcher(
    private val dictionary: Map<String, IconBucket>,
    private val normalizer: TextNormalizer
) : IconMatcher {
    override fun match(itemName: String): IconBucket {
        val normalized = normalizer.normalize(itemName)
        return dictionary[normalized] ?: IconBucket.GENERIC
    }
}
