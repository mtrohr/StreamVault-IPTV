package com.streamvault.domain.repository

import com.streamvault.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(providerId: Long): Flow<List<Category>>
    suspend fun setCategoryProtection(categoryId: Long, isProtected: Boolean)
}
