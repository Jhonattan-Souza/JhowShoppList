package com.jhow.shopplist.domain.icon

interface IconMatcher {
    fun match(itemName: String): IconBucket
    fun updateDictionary(newDictionary: Map<String, IconBucket>): Boolean
}

/**
 * Stateful singleton that resolves item names to icon buckets via a shallow cascade:
 * exact → alias → token-per-token → generic.
 *
 * Dictionary keys are normalized on ingestion so queries and keys use the same
 * representation. The cascade never uses stemming, fuzzy matching, or ML.
 */
class DefaultIconMatcher(
    dictionary: Map<String, IconBucket>,
    private val normalizer: TextNormalizer,
    private val aliasMap: Map<String, IconBucket> = emptyMap()
) : IconMatcher {

    @Volatile
    private var dictionaryRef: Map<String, IconBucket> = dictionary.normalizeKeys()

    override fun updateDictionary(newDictionary: Map<String, IconBucket>): Boolean {
        val normalized = newDictionary.normalizeKeys()
        if (dictionaryRef == normalized) return false
        dictionaryRef = normalized
        return true
    }

    override fun match(itemName: String): IconBucket {
        val normalized = normalizer.normalize(itemName)
        if (normalized.isEmpty()) return IconBucket.GENERIC

        return dictionaryRef[normalized]
            ?: aliasMap[normalized]
            ?: normalized.split(" ").filter { it.isNotBlank() }
                .firstNotNullOfOrNull { token -> dictionaryRef[token] ?: aliasMap[token] }
            ?: IconBucket.GENERIC
    }

    private fun Map<String, IconBucket>.normalizeKeys(): Map<String, IconBucket> =
        entries.associate { (k, v) -> normalizer.normalize(k) to v }
}
