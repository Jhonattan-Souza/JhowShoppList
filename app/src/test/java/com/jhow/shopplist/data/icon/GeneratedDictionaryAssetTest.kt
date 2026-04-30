package com.jhow.shopplist.data.icon

import com.jhow.shopplist.domain.icon.DefaultIconMatcher
import com.jhow.shopplist.domain.icon.DefaultTextNormalizer
import com.jhow.shopplist.domain.icon.IconBucket
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratedDictionaryAssetTest {

    private val matcher = DefaultIconMatcher(loadMergedDictionary(), DefaultTextNormalizer())

    @Test
    fun `generated dictionaries preserve curated overlay terms`() {
        assertResolves(
            "leite" to IconBucket.DAIRY,
            "iogurte" to IconBucket.DAIRY,
            "maçã" to IconBucket.FRUIT,
            "pão" to IconBucket.BREAD,
            "arroz" to IconBucket.PANTRY,
            "feijão" to IconBucket.PANTRY,
            "milk" to IconBucket.DAIRY,
            "yogurt" to IconBucket.DAIRY,
            "apple" to IconBucket.FRUIT,
            "bread" to IconBucket.BREAD,
            "rice" to IconBucket.PANTRY,
            "beans" to IconBucket.PANTRY
        )
    }

    @Test
    fun `generated dictionaries cover OFF derived terms beyond the overlay`() {
        assertResolves(
            "provolone" to IconBucket.CHEESE,
            "lettuces" to IconBucket.VEGETABLE,
            "broccoli" to IconBucket.VEGETABLE,
            "fish fillets" to IconBucket.FISH,
            "chicken breasts" to IconBucket.MEAT,
            "hams" to IconBucket.DELI_COLD_CUTS,
            "waters" to IconBucket.BEVERAGES_COLD,
            "coffees" to IconBucket.BEVERAGES_HOT,
            "wines" to IconBucket.ALCOHOL,
            "ice creams" to IconBucket.FROZEN,
            "chocolates" to IconBucket.SWEETS,
            "olive oils" to IconBucket.CONDIMENTS,
            "queijos" to IconBucket.CHEESE,
            "alface" to IconBucket.VEGETABLE,
            "brócolos" to IconBucket.VEGETABLE,
            "filetes de peixe" to IconBucket.FISH,
            "peitos de frango" to IconBucket.MEAT,
            "salsichas" to IconBucket.DELI_COLD_CUTS,
            "águas" to IconBucket.BEVERAGES_COLD,
            "cafés" to IconBucket.BEVERAGES_HOT,
            "vinhos" to IconBucket.ALCOHOL,
            "gelado" to IconBucket.FROZEN,
            "azeite de oliva" to IconBucket.CONDIMENTS
        )
    }

    @Test
    fun `generated dictionaries keep overlay only corrections and additions`() {
        assertResolves(
            "shampoo" to IconBucket.PERSONAL_CARE,
            "toothpaste" to IconBucket.PERSONAL_CARE,
            "ração" to IconBucket.PET,
            "dog food" to IconBucket.PET,
            "papinha" to IconBucket.BABY,
            "baby formula" to IconBucket.BABY
        )
    }

    private fun assertResolves(vararg expectations: Pair<String, IconBucket>) {
        expectations.forEach { (term, expectedBucket) ->
            assertEquals(term, expectedBucket, matcher.match(term))
        }
    }

    private fun loadMergedDictionary(): Map<String, IconBucket> {
        val pt = parseDictionaryJson(readAsset("dictionary-pt.json"))
        val en = parseDictionaryJson(readAsset("dictionary-en.json"))
        return pt + en
    }

    private fun readAsset(fileName: String): String {
        val candidates = listOf(
            Path.of("app", "src", "main", "assets", "icons", fileName),
            Path.of("src", "main", "assets", "icons", fileName)
        )

        val assetPath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Unable to find asset $fileName from ${candidates.joinToString()}")

        return Files.readAllBytes(assetPath).toString(Charsets.UTF_8)
    }
}
