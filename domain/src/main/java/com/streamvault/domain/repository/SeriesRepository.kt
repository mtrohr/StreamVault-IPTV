package com.streamvault.domain.repository

import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Episode
import com.streamvault.domain.model.Result
import com.streamvault.domain.model.Series
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {
    fun getSeries(providerId: Long): Flow<List<Series>>
    fun getSeriesByCategory(providerId: Long, categoryId: Long): Flow<List<Series>>
    fun getSeriesByIds(ids: List<Long>): Flow<List<Series>>
    fun getCategories(providerId: Long): Flow<List<Category>>
    fun searchSeries(providerId: Long, query: String): Flow<List<Series>>
    suspend fun getSeriesById(seriesId: Long): Series?
    suspend fun getSeriesDetails(providerId: Long, seriesId: Long): Result<Series>
    suspend fun getEpisodeStreamUrl(episode: Episode): Result<String>
    suspend fun refreshSeries(providerId: Long): Result<Unit>
    suspend fun updateEpisodeWatchProgress(episodeId: Long, progress: Long)
}
