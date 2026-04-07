package com.streamvault.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalControlManager @Inject constructor(
    private val sessionStore: ParentalControlSessionStore
) {
    private val stateLock = Any()
    private val _unlockedCategoriesByProvider = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    val unlockedCategoriesByProvider: StateFlow<Map<Long, Set<Long>>> =
        _unlockedCategoriesByProvider.asStateFlow()

    init {
        if (sessionStore.readSessionState().unlockedCategoryIdsByProvider.isNotEmpty()) {
            sessionStore.writeSessionState(ParentalControlSessionState())
        }
    }

    fun unlockedCategoriesForProvider(providerId: Long) =
        unlockedCategoriesByProvider.map { it[providerId] ?: emptySet() }

    fun unlockCategory(providerId: Long, categoryId: Long) {
        synchronized(stateLock) {
            publishState(
                _unlockedCategoriesByProvider.value + (providerId to setOf(categoryId))
            )
        }
    }

    fun isCategoryUnlocked(providerId: Long, categoryId: Long): Boolean {
        synchronized(stateLock) {
            return _unlockedCategoriesByProvider.value[providerId]?.contains(categoryId) == true
        }
    }

    fun retainUnlockedCategory(providerId: Long, categoryId: Long?) {
        synchronized(stateLock) {
            val retainedCategoryIds = categoryId
                ?.takeIf { unlockedCategoryIds ->
                    _unlockedCategoriesByProvider.value[providerId]?.contains(unlockedCategoryIds) == true
                }
                ?.let(::setOf)
                .orEmpty()
            val updatedUnlocks = if (retainedCategoryIds.isEmpty()) {
                _unlockedCategoriesByProvider.value - providerId
            } else {
                _unlockedCategoriesByProvider.value + (providerId to retainedCategoryIds)
            }
            publishState(updatedUnlocks)
        }
    }

    fun clearUnlockedCategories(providerId: Long? = null) {
        synchronized(stateLock) {
            val updatedUnlocks = if (providerId == null) {
                emptyMap()
            } else {
                _unlockedCategoriesByProvider.value - providerId
            }
            publishState(updatedUnlocks)
        }
    }

    private fun publishState(
        state: Map<Long, Set<Long>>
    ) {
        _unlockedCategoriesByProvider.value = state
            .filterValues { it.isNotEmpty() }
        sessionStore.writeSessionState(ParentalControlSessionState())
    }
}
