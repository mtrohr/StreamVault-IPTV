package com.streamvault.data.repository

import com.streamvault.data.local.dao.*
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.mapper.toDomain
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao
) : CategoryRepository {

    override fun getCategories(providerId: Long): Flow<List<Category>> =
        categoryDao.getByProviderAndType(providerId, ContentType.LIVE.name)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun setCategoryProtection(categoryId: Long, isProtected: Boolean) {
        // Update category table
        categoryDao.updateProtectionStatus(categoryId, isProtected)
        
        // Update items tables
        channelDao.updateProtectionStatus(categoryId, isProtected)
        movieDao.updateProtectionStatus(categoryId, isProtected)
        seriesDao.updateProtectionStatus(categoryId, isProtected)
    }
}
