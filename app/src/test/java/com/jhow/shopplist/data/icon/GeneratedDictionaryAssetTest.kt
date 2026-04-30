package com.jhow.shopplist.data.icon

import com.jhow.shopplist.domain.icon.DefaultIconMatcher
import com.jhow.shopplist.domain.icon.DefaultTextNormalizer
import com.jhow.shopplist.domain.icon.IconBucket
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratedDictionaryAssetTest {

    private val normalizer = DefaultTextNormalizer()
    private val mergedDictionary = loadMergedDictionary()
    private val matcher = DefaultIconMatcher(mergedDictionary, normalizer)

    @Test
    fun `generated dictionaries preserve every curated overlay term and bucket`() {
        val overlays = loadOverlayDictionary("pt.overlay.json") + loadOverlayDictionary("en.overlay.json")

        overlays.forEach { (term, expectedBucket) ->
            val normalizedTerm = normalizer.normalize(term)
            assertEquals(term, expectedBucket, mergedDictionary[normalizedTerm])
            assertEquals(term, expectedBucket, matcher.match(term))
        }
    }

    @Test
    fun `generated dictionaries cover OFF derived terms beyond the overlay`() {
        assertResolves(
            "lettuces" to IconBucket.VEGETABLE,
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
            "brócolos" to IconBucket.VEGETABLE,
            "filetes de peixe" to IconBucket.FISH,
            "peitos de frango" to IconBucket.MEAT,
            "salsichas" to IconBucket.DELI_COLD_CUTS,
            "águas" to IconBucket.BEVERAGES_COLD,
            "cafés" to IconBucket.BEVERAGES_HOT,
            "vinhos" to IconBucket.ALCOHOL,
            "gelado" to IconBucket.FROZEN,
            "azeite de oliva" to IconBucket.CONDIMENTS,
            "mixed yogurts" to IconBucket.DAIRY,
            "plain wheat biscuits" to IconBucket.SNACKS,
            "tea seed oils" to IconBucket.CONDIMENTS
        )
    }

    @Test
    fun `generated dictionaries keep known overlay bucket regressions`() {
        assertResolves(
            "whey" to IconBucket.STAPLES,
            "cappuccino" to IconBucket.BEVERAGES_HOT,
            "bolo" to IconBucket.BREAD,
            "hambúrguer" to IconBucket.FROZEN
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
        return (pt + en).mapKeys { (key, _) -> normalizer.normalize(key) }
    }

    private fun loadOverlayDictionary(fileName: String): Map<String, IconBucket> =
        parseDictionaryJson(stripLineComments(readOverlay(fileName)))

    private fun readOverlay(fileName: String): String {
        val candidates = listOf(
            Path.of("tools", "build-dictionary", "overlays", fileName),
            Path.of("..", "tools", "build-dictionary", "overlays", fileName)
        )

        val overlayPath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Unable to find overlay $fileName from ${candidates.joinToString()}")

        return Files.readAllBytes(overlayPath).toString(Charsets.UTF_8)
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

    private fun stripLineComments(jsonWithComments: String): String =
        jsonWithComments.lineSequence()
            .filterNot { it.trimStart().startsWith("//") }
            .joinToString("\n")
}
