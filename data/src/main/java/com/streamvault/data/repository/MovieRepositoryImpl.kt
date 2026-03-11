package com.streamvault.data.repository

import com.streamvault.data.local.dao.CategoryDao
import com.streamvault.data.local.dao.MovieDao
import com.streamvault.data.local.entity.MovieEntity
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.mapper.toDomain
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.streamvault.data.preferences.PreferencesRepository
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val categoryDao: CategoryDao,
    private val preferencesRepository: PreferencesRepository
) : MovieRepository {

    override fun getMovies(providerId: Long): Flow<List<Movie>> =
        combine(
            movieDao.getByProvider(providerId),
            preferencesRepository.parentalControlLevel
        ) { entities: List<MovieEntity>, level: Int ->
            if (level == 2) {
                entities.filter { !it.isUserProtected }
            } else {
                entities
            }
        }.map { list -> list.map { it.toDomain() } }

    override fun getMoviesByCategory(providerId: Long, categoryId: Long): Flow<List<Movie>> =
        combine(
            movieDao.getByCategory(providerId, categoryId),
            preferencesRepository.parentalControlLevel
        ) { entities: List<MovieEntity>, level: Int ->
            if (level == 2) {
                entities.filter { !it.isUserProtected }
            } else {
                entities
            }
        }.map { list -> list.map { it.toDomain() } }

    override fun getMoviesByIds(ids: List<Long>): Flow<List<Movie>> =
        movieDao.getByIds(ids).map { entities -> entities.map { it.toDomain() } }

    override fun getCategories(providerId: Long): Flow<List<Category>> =
        combine(
            categoryDao.getByProviderAndType(providerId, ContentType.MOVIE.name),
            preferencesRepository.parentalControlLevel
        ) { entities: List<CategoryEntity>, level: Int ->
            val mapped = entities.map { it.toDomain() }
            if (level == 2) {
                mapped.filter { !it.isAdult && !it.isUserProtected }
            } else {
                mapped
            }
        }

    override fun searchMovies(providerId: Long, query: String): Flow<List<Movie>> =
        query.toFtsPrefixQuery().let { ftsQuery ->
            if (ftsQuery.isBlank()) {
            flowOf(emptyList())
            } else combine(
                movieDao.search(providerId, ftsQuery),
                preferencesRepository.parentalControlLevel
            ) { entities: List<MovieEntity>, level: Int ->
                if (level == 2) {
                    entities.filter { !it.isUserProtected }
                } else {
                    entities
                }
            }.map { list -> list.map { it.toDomain() } }
        }

    override suspend fun getMovie(movieId: Long): Movie? =
        movieDao.getById(movieId)?.toDomain()

    override suspend fun getMovieDetails(providerId: Long, movieId: Long): Result<Movie> {
        val movie = movieDao.getById(movieId)?.toDomain()
        return if (movie != null) Result.success(movie) else Result.error("Movie not found")
    }

    override suspend fun getStreamUrl(movie: Movie): Result<String> =
        if (movie.streamUrl.isNotBlank()) {
            Result.success(movie.streamUrl)
        } else {
            Result.error("No stream URL available for movie: ${movie.name}")
        }

    override suspend fun refreshMovies(providerId: Long): Result<Unit> =
        Result.success(Unit) // Handled by ProviderRepository

    override suspend fun updateWatchProgress(movieId: Long, progress: Long) {
        movieDao.updateWatchProgress(movieId, progress)
    }

    private fun String.toFtsPrefixQuery(): String {
        val tokens = trim()
            .split(Regex("\\s+"))
            .map { token -> token.replace(Regex("[^\\p{L}\\p{N}_]"), "") }
            .filter { it.length >= 2 }

        return tokens.joinToString(" AND ") { "$it*" }
    }
}
