package com.streamvault.data.repository

import com.streamvault.data.local.dao.*
import com.streamvault.data.mapper.*
import com.streamvault.domain.model.*
import com.streamvault.domain.repository.SeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeriesRepositoryImpl @Inject constructor(
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao,
    private val categoryDao: CategoryDao
) : SeriesRepository {

    override fun getSeries(providerId: Long): Flow<List<Series>> =
        seriesDao.getByProvider(providerId).map { entities -> entities.map { it.toDomain() } }

    override fun getSeriesByCategory(providerId: Long, categoryId: Long): Flow<List<Series>> =
        seriesDao.getByCategory(providerId, categoryId).map { entities -> entities.map { it.toDomain() } }

    override fun getCategories(providerId: Long): Flow<List<Category>> =
        categoryDao.getByProviderAndType(providerId, ContentType.SERIES.name)
            .map { entities -> entities.map { it.toDomain() } }

    override fun searchSeries(providerId: Long, query: String): Flow<List<Series>> =
        seriesDao.search(providerId, query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSeriesById(seriesId: Long): Series? =
        seriesDao.getById(seriesId)?.toDomain()

    override suspend fun getSeriesDetails(providerId: Long, seriesId: Long): Result<Series> {
        val series = seriesDao.getById(seriesId)?.toDomain()
        return if (series != null) Result.success(series) else Result.error("Series not found")
    }

    override suspend fun getEpisodeStreamUrl(episode: Episode): Result<String> =
        if (episode.streamUrl.isNotBlank()) {
            Result.success(episode.streamUrl)
        } else {
            Result.error("No stream URL available for episode: ${episode.title}")
        }

    override suspend fun refreshSeries(providerId: Long): Result<Unit> =
        Result.success(Unit) // Handled by ProviderRepository

    override suspend fun updateEpisodeWatchProgress(episodeId: Long, progress: Long) {
        episodeDao.updateWatchProgress(episodeId, progress)
    }
}
