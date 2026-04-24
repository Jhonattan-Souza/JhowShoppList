package com.jhow.shopplist.core.search

import java.text.Normalizer
import java.util.Locale

object ShoppingSearch {
    private val combiningMarksRegex = "\\p{Mn}+".toRegex()

    fun normalize(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replace(combiningMarksRegex, "")
        .lowercase(Locale.ROOT)

    fun suggestionScore(candidate: String, normalizedQuery: String): Int? {
        val normalizedCandidate = normalize(candidate)
        val substringIndex = normalizedCandidate.indexOf(normalizedQuery)

        return when {
            normalizedQuery.length < MIN_SUGGESTION_QUERY_LENGTH -> null
            normalizedCandidate.isEmpty() -> null
            normalizedCandidate == normalizedQuery -> EXACT_MATCH_SCORE
            normalizedCandidate.startsWith(normalizedQuery) -> PREFIX_MATCH_SCORE
            hasWordPrefixMatch(normalizedCandidate, normalizedQuery) -> WORD_PREFIX_MATCH_SCORE
            substringIndex >= 0 -> SUBSTRING_MATCH_SCORE + substringIndex
            normalizedQuery.length < MIN_FUZZY_QUERY_LENGTH -> null
            else -> subsequencePenalty(
                candidate = normalizedCandidate,
                query = normalizedQuery
            )?.let { FUZZY_SUBSEQUENCE_MATCH_SCORE + it }
        }
    }

    private fun hasWordPrefixMatch(candidate: String, query: String): Boolean {
        for (index in 1..candidate.lastIndex) {
            if (!candidate[index - 1].isLetterOrDigit() && candidate.startsWith(query, startIndex = index)) {
                return true
            }
        }
        return false
    }

    private fun subsequencePenalty(candidate: String, query: String): Int? {
        var nextSearchStartIndex = 0
        var firstMatchIndex = -1
        var previousMatchIndex = -1
        var totalGapSize = 0

        for (queryCharacter in query) {
            val matchIndex = candidate.indexOf(queryCharacter, startIndex = nextSearchStartIndex)
            if (matchIndex < 0) {
                return null
            }

            if (firstMatchIndex < 0) {
                firstMatchIndex = matchIndex
            }
            if (previousMatchIndex >= 0) {
                totalGapSize += matchIndex - previousMatchIndex - 1
            }

            previousMatchIndex = matchIndex
            nextSearchStartIndex = matchIndex + 1
        }

        val unmatchedTailSize = candidate.length - previousMatchIndex - 1
        return (firstMatchIndex * FIRST_MATCH_WEIGHT) +
            (totalGapSize * GAP_WEIGHT) +
            unmatchedTailSize
    }

    private const val MIN_SUGGESTION_QUERY_LENGTH = 2
    private const val MIN_FUZZY_QUERY_LENGTH = 3

    private const val EXACT_MATCH_SCORE = 0
    private const val PREFIX_MATCH_SCORE = 100
    private const val WORD_PREFIX_MATCH_SCORE = 200
    private const val SUBSTRING_MATCH_SCORE = 300
    private const val FUZZY_SUBSEQUENCE_MATCH_SCORE = 400

    private const val FIRST_MATCH_WEIGHT = 4
    private const val GAP_WEIGHT = 2
}
