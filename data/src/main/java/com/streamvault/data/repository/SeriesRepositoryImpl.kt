package com.streamvault.data.repository

import com.streamvault.data.local.dao.*
import com.streamvault.data.local.entity.*
import com.streamvault.data.mapper.*
import com.streamvault.domain.model.*
import com.streamvault.domain.repository.SeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.streamvault.data.preferences.PreferencesRepository
import javax.inject.Singleton

@Singleton
class SeriesRepositoryImpl @Inject constructor(
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao,
    private val categoryDao: CategoryDao,
    private val preferencesRepository: PreferencesRepository
) : SeriesRepository {

    override fun getSeries(providerId: Long): Flow<List<Series>> =
        combine(
            seriesDao.getByProvider(providerId),
            preferencesRepository.parentalControlLevel
        ) { entities: List<SeriesEntity>, level: Int ->
            if (level == 2) {
                entities.filter { !it.isUserProtected }
            } else {
                entities
            }
        }.map { list: List<SeriesEntity> -> list.map { it.toDomain() } }

    override fun getSeriesByCategory(providerId: Long, categoryId: Long): Flow<List<Series>> =
        combine(
            seriesDao.getByCategory(providerId, categoryId),
            preferencesRepository.parentalControlLevel
        ) { entities: List<SeriesEntity>, level: Int ->
            if (level == 2) {
                entities.filter { !it.isUserProtected }
            } else {
                entities
            }
        }.map { list: List<SeriesEntity> -> list.map { it.toDomain() } }

    override fun getCategories(providerId: Long): Flow<List<Category>> =
        combine(
            categoryDao.getByProviderAndType(providerId, ContentType.SERIES.name),
            preferencesRepository.parentalControlLevel
        ) { entities: List<CategoryEntity>, level: Int ->
            val mapped = entities.map { it.toDomain() }
            if (level == 2) {
                mapped.filter { !it.isAdult && !it.isUserProtected }
            } else {
                mapped
            }
        }

    override fun searchSeries(providerId: Long, query: String): Flow<List<Series>> =
        combine(
            seriesDao.search(providerId, query),
            preferencesRepository.parentalControlLevel
        ) { entities: List<SeriesEntity>, level: Int ->
            if (level == 2) {
                entities.filter { !it.isUserProtected }
            } else {
                entities
            }
        }.map { list: List<SeriesEntity> -> list.map { it.toDomain() } }

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
