package com.jhow.shopplist.domain.icon

import java.text.Normalizer as JNormalizer

interface TextNormalizer {
    fun normalize(text: String): String
}

class DefaultTextNormalizer : TextNormalizer {

    private val quantityTokenRegex = Regex(
        """^\d+(?:[.,]\d+)?(?:kg|g|mg|l|ml|un|unidades?|x)?$""",
        RegexOption.IGNORE_CASE
    )

    private val standaloneUnitWords = setOf(
        "kg", "g", "mg", "l", "ml",
        "pacote", "pacotes", "pack", "packs", "unidade", "unidades"
    )

    private val stopwords = setOf("de", "do", "da", "dos", "das", "com", "the", "of", "and", "with")

    override fun normalize(text: String): String {
        val lowercased = text.trim().lowercase()
        val diacriticsStripped = stripDiacritics(lowercased)
        val tokens = diacriticsStripped.split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = tokens.filter { token ->
            !isQuantityToken(token) && token !in stopwords
        }
        return filtered.joinToString(" ")
    }

    private fun stripDiacritics(s: String): String {
        val nfd = JNormalizer.normalize(s, JNormalizer.Form.NFD)
        return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun isQuantityToken(token: String): Boolean =
        token.matches(quantityTokenRegex) || token in standaloneUnitWords
}
