package com.jhow.shopplist.domain.icon

interface IconMatcher {
    fun match(itemName: String): IconBucket
}

class DefaultIconMatcher(
    dictionary: Map<String, IconBucket>,
    private val normalizer: TextNormalizer
) : IconMatcher {

    @Volatile
    private var dictionaryRef: Map<String, IconBucket> = dictionary

    fun updateDictionary(newDictionary: Map<String, IconBucket>): Boolean {
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
