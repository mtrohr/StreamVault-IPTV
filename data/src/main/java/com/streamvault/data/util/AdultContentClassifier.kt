package com.streamvault.data.util

import java.util.Locale

object AdultContentClassifier {
    private val keywords = listOf(
        "xxx",
        "adult",
        "18+",
        "18 plus",
        "18plus",
        "porn",
        "porno",
        "sex",
        "erotic",
        "hustler",
        "playboy",
        "redlight",
        "red light"
    )

    fun isAdultCategoryName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val normalized = normalize(name)
        return keywords.any { keyword ->
            normalized.contains(normalize(keyword))
        }
    }

    fun adultCategoryIds(namesById: Map<Long, String>): Set<Long> {
        return namesById
            .filterValues(::isAdultCategoryName)
            .keys
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9+]+"), " ")
            .trim()
    }
}
