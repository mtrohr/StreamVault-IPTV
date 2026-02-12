package com.streamvault.data.repository

import com.streamvault.data.local.dao.CategoryDao
import com.streamvault.data.local.dao.ChannelDao
import com.streamvault.data.mapper.toDomain
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao
) : ChannelRepository {

    override fun getChannels(providerId: Long): Flow<List<Channel>> =
        channelDao.getByProvider(providerId).map { entities -> entities.map { it.toDomain() } }

    override fun getChannelsByCategory(providerId: Long, categoryId: Long): Flow<List<Channel>> =
        channelDao.getByCategory(providerId, categoryId).map { entities -> entities.map { it.toDomain() } }

    override fun getCategories(providerId: Long): Flow<List<Category>> =
        categoryDao.getByProviderAndType(providerId, ContentType.LIVE.name)
            .map { entities -> entities.map { it.toDomain() } }

    override fun searchChannels(providerId: Long, query: String): Flow<List<Channel>> =
        channelDao.search(providerId, query).map { entities -> entities.map { it.toDomain() } }

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
}
