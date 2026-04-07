package com.streamvault.data.util

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object AdultContentClassifier {
    private const val MAX_CLASSIFICATION_CACHE_SIZE = 4096
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val separatorsRegex = Regex("[^a-z0-9+]+")

    // Strong tags are matched via normalized substring contains (case-insensitive by normalization).
    private val strongContainsKeywords = listOf(
        "xxx",
        "18+",
        "+18",
        "+ 18",
        "18 +",
        "18 plus",
        "plus 18",
        "18plus"
    )

    // Softer tags are matched on normalized word boundaries to avoid overmatching.
    private val boundaryKeywords = listOf(
        // English
        "adult",
        "adults",
        "porn",
        "porno",
        "milf",
        "gay",
        "lesbian",
        "sex",
        "erotic",
        "hustler",
        "playboy",
        "hanime",
        "live cam",
        "live cams",
        "redlight",
        "red light",
        // Spanish
        "adulto",
        "adultos",
        // French
        "adulte",
        "adultes",
        "erotique",
        // German
        "erwachsene",
        "erotik",
        // Russian (transliterated)
        "vzroslye",
        "dlya vzroslykh",
        // Arabic (transliterated)
        "lilkbar",
        // Turkish
        "yetiskin",
        // Anime adult content
        "hentai",
        // Common IPTV labels
        "for adults",
        "pour adultes",
        "para adultos"
    )
    private val normalizedStrongKeywords = strongContainsKeywords.map(::normalize).distinct()
    private val normalizedBoundaryKeywords = boundaryKeywords.map(::normalize).distinct()
    private val classificationCache = ConcurrentHashMap<String, Boolean>()

    fun isAdultCategoryName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val normalized = normalize(name)
        classificationCache[normalized]?.let { return it }
        val normalizedPadded = " $normalized "
        val isAdult = normalizedStrongKeywords.any { keyword ->
            normalized.contains(keyword)
        } || normalizedBoundaryKeywords.any { keyword ->
            normalizedPadded.contains(" $keyword ")
        }
        if (classificationCache.size >= MAX_CLASSIFICATION_CACHE_SIZE) {
            classificationCache.clear()
        }
        classificationCache[normalized] = isAdult
        return isAdult
    }

    fun adultCategoryIds(namesById: Map<Long, String>): Set<Long> {
        return namesById
            .filterValues(::isAdultCategoryName)
            .keys
    }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(diacriticsRegex, "")
            .lowercase(Locale.ROOT)
            .replace(separatorsRegex, " ")
            .trim()
    }
}
