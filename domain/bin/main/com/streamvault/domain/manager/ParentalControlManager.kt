package com.streamvault.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalControlManager @Inject constructor() {
    private val _unlockedCategories = MutableStateFlow<Set<Long>>(emptySet())
    val unlockedCategories: StateFlow<Set<Long>> = _unlockedCategories.asStateFlow()

    fun unlockCategory(categoryId: Long) {
        val current = _unlockedCategories.value.toMutableSet()
        current.add(categoryId)
        _unlockedCategories.value = current
    }

    fun isCategoryUnlocked(categoryId: Long): Boolean {
        return _unlockedCategories.value.contains(categoryId)
    }

    fun clearUnlockedCategories() {
        _unlockedCategories.value = emptySet()
    }
}
