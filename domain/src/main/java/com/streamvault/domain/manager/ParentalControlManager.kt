package com.streamvault.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalControlManager @Inject constructor() {
    private val _unlockedCategoriesByProvider = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    val unlockedCategoriesByProvider: StateFlow<Map<Long, Set<Long>>> =
        _unlockedCategoriesByProvider.asStateFlow()

    fun unlockedCategoriesForProvider(providerId: Long) =
        unlockedCategoriesByProvider.map { it[providerId] ?: emptySet() }

    fun unlockCategory(providerId: Long, categoryId: Long) {
        val current = _unlockedCategoriesByProvider.value.toMutableMap()
        val providerSet = (current[providerId] ?: emptySet()).toMutableSet()
        providerSet.add(categoryId)
        current[providerId] = providerSet
        _unlockedCategoriesByProvider.value = current
    }

    fun isCategoryUnlocked(providerId: Long, categoryId: Long): Boolean {
        return _unlockedCategoriesByProvider.value[providerId]?.contains(categoryId) == true
    }

    fun clearUnlockedCategories(providerId: Long? = null) {
        if (providerId == null) {
            _unlockedCategoriesByProvider.value = emptyMap()
            return
        }

        val current = _unlockedCategoriesByProvider.value.toMutableMap()
        current.remove(providerId)
        _unlockedCategoriesByProvider.value = current
    }
}
