package com.streamvault.data.repository

import com.streamvault.data.local.dao.CategoryDao
import com.streamvault.data.local.dao.ChannelDao
import com.streamvault.data.local.entity.CategoryCount
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.local.entity.ChannelEntity
import com.streamvault.data.mapper.toDomain
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import com.streamvault.data.preferences.PreferencesRepository
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val preferencesRepository: PreferencesRepository,
    private val parentalControlManager: com.streamvault.domain.manager.ParentalControlManager
) : ChannelRepository {

    override fun getChannels(providerId: Long): Flow<List<Channel>> =
        combine(
            channelDao.getByProvider(providerId),
            preferencesRepository.parentalControlLevel,
            parentalControlManager.unlockedCategories
        ) { entities, level, unlockedCats ->
            // Level 2 = HIDDEN. 
            // If hidden, filter out adult/protected UNLESS they are in unlockedCats.
            val filtered = if (level == 2) {
                entities.filter { entity ->
                    val isUnlocked = entity.categoryId != null && unlockedCats.contains(entity.categoryId)
                    (!entity.isAdult && !entity.isUserProtected) || isUnlocked
                }
            } else {
                entities
            }
            
            groupAndMapChannels(filtered, unlockedCats)
        }

    override fun getChannelsByCategory(providerId: Long, categoryId: Long): Flow<List<Channel>> =
        combine(
            if (categoryId == ChannelRepository.ALL_CHANNELS_ID) {
                channelDao.getByProvider(providerId)
            } else {
                channelDao.getByCategory(providerId, categoryId)
            },
            preferencesRepository.parentalControlLevel,
            parentalControlManager.unlockedCategories
        ) { entities, level, unlockedCats ->
             // Level 2 = HIDDEN. 
            // If hidden, filter out adult/protected UNLESS they are in unlockedCats.
            val filtered = if (level == 2) {
                entities.filter { entity ->
                    val isUnlocked = entity.categoryId != null && unlockedCats.contains(entity.categoryId)
                    (!entity.isAdult && !entity.isUserProtected) || isUnlocked
                }
            } else {
                entities
            }
            
            groupAndMapChannels(filtered, unlockedCats)
        }

    override fun getCategories(providerId: Long): Flow<List<Category>> =
        combine(
            categoryDao.getByProviderAndType(providerId, ContentType.LIVE.name),
            channelDao.getCategoryCounts(providerId),
            channelDao.getCount(providerId),
            preferencesRepository.parentalControlLevel,
            parentalControlManager.unlockedCategories
        ) { categories: List<CategoryEntity>, counts: List<CategoryCount>, totalCount: Int, level: Int, unlockedCats: Set<Long> ->
            android.util.Log.d("ChannelRepo", "getCategories: ${categories.size} cats, ${counts.size} counts, total: $totalCount")
            val countMap = counts.associate { it.categoryId to it.item_count }
            
            val allChannelsCategory = Category(
                id = ChannelRepository.ALL_CHANNELS_ID,
                name = "All Channels",
                type = ContentType.LIVE,
                count = totalCount
            )
            
            val mappedCategories = categories.map { entity ->
                val domain = entity.toDomain().copy(count = countMap[entity.categoryId] ?: 0)
                // If unlocked, update domain model (though Category model doesn't strictly need it if we trust the check elsewhere, 
                // but nice for UI to show 'unlocked' icon if we had one. For now just passing through).
                 if (unlockedCats.contains(entity.categoryId)) {
                    domain.copy(isUserProtected = false)
                } else {
                    domain
                }
            }
            
            // Filter categories if level is HIDDEN
            val filteredCategories = if (level == 2) {
                mappedCategories.filter { category ->
                    (!category.isAdult && !category.isUserProtected) || unlockedCats.contains(category.id)
                }
            } else {
                mappedCategories
            }
            
            listOf(allChannelsCategory) + filteredCategories
        }

    override fun searchChannels(providerId: Long, query: String): Flow<List<Channel>> =
        combine(
            channelDao.search(providerId, query),
            preferencesRepository.parentalControlLevel,
            parentalControlManager.unlockedCategories
        ) { entities, level, unlockedCats ->
            val filtered = if (level == 2) {
                entities.filter { entity ->
                     val isUnlocked = entity.categoryId != null && unlockedCats.contains(entity.categoryId)
                    (!entity.isAdult && !entity.isUserProtected) || isUnlocked
                }
            } else {
                entities
            }
            
             groupAndMapChannels(filtered, unlockedCats)
        }

    override suspend fun getChannel(channelId: Long): Channel? =
        channelDao.getById(channelId)?.toDomain()

    override suspend fun getStreamUrl(channel: Channel): Result<String> =
        if (channel.streamUrl.isNotBlank()) {
            Result.success(channel.streamUrl)
        } else {
            Result.error("No stream URL available for channel: ${channel.name}")
        }

    override suspend fun refreshChannels(providerId: Long): Result<Unit> {
        // Refresh is handled by ProviderRepository.refreshProviderData
        return Result.success(Unit)
    }

    override fun getChannelsByIds(ids: List<Long>): Flow<List<Channel>> =
        channelDao.getByIds(ids).map { entities -> entities.map { it.toDomain() } }

    override suspend fun incrementChannelErrorCount(channelId: Long) {
        channelDao.incrementErrorCount(channelId)
    }

    override suspend fun resetChannelErrorCount(channelId: Long) {
        channelDao.resetErrorCount(channelId)
    }

    private fun groupAndMapChannels(entities: List<ChannelEntity>, unlockedCats: Set<Long>): List<Channel> {
        return entities.groupBy { 
            if (it.logicalGroupId.isNotBlank()) it.logicalGroupId else it.id.toString() 
        }.values.map { group ->
            // Sort group to pick the primary channel based on reliability and name length
            val sortedGroup = group.sortedWith(compareBy({ it.errorCount }, { it.name.length }))
            val primaryEntity = sortedGroup.first()
            val alternativeStreams = sortedGroup.drop(1).map { it.streamUrl }
            
            val domain = primaryEntity.toDomain().copy(
                alternativeStreams = alternativeStreams
            )
            
            // If category is unlocked, mark channel as NOT protected/ADULT for this session
            if (primaryEntity.categoryId != null && unlockedCats.contains(primaryEntity.categoryId)) {
                domain.copy(isUserProtected = false, isAdult = false)
            } else {
                domain
            }
        }
    }
}
