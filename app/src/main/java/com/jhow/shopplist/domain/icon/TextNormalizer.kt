package com.jhow.shopplist.domain.icon

interface TextNormalizer {
    fun normalize(text: String): String
}

class DefaultTextNormalizer : TextNormalizer {
    override fun normalize(text: String): String = text.trim().lowercase()
}
