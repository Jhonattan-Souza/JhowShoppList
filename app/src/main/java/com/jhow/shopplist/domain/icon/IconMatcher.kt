package com.jhow.shopplist.domain.icon

interface IconMatcher {
    fun match(itemName: String): IconBucket
    fun updateDictionary(newDictionary: Map<String, IconBucket>): Boolean
}

/**
 * Stateful singleton that delegates matching to a mutable dictionary reference.
 *
 * While the class holds mutable state, it is effectively a pure function for any
 * given dictionary version: the same inputs always produce the same outputs until
 * [updateDictionary] is called.
 */
class DefaultIconMatcher(
    dictionary: Map<String, IconBucket>,
    private val normalizer: TextNormalizer
) : IconMatcher {

    @Volatile
    private var dictionaryRef: Map<String, IconBucket> = dictionary

    override fun updateDictionary(newDictionary: Map<String, IconBucket>): Boolean {
        if (dictionaryRef == newDictionary) {
            return false
        }
        dictionaryRef = newDictionary
        return true
    }

    override fun match(itemName: String): IconBucket {
        val normalized = normalizer.normalize(itemName)
        return dictionaryRef[normalized] ?: IconBucket.GENERIC
    }
}
