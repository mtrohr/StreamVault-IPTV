package com.streamvault.data.repository

import com.streamvault.data.local.dao.CategoryDao
import com.streamvault.data.local.dao.MovieDao
import com.streamvault.data.mapper.toDomain
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val categoryDao: CategoryDao
) : MovieRepository {

    override fun getMovies(providerId: Long): Flow<List<Movie>> =
        movieDao.getByProvider(providerId).map { entities -> entities.map { it.toDomain() } }

    override fun getMoviesByCategory(providerId: Long, categoryId: Long): Flow<List<Movie>> =
        movieDao.getByCategory(providerId, categoryId).map { entities -> entities.map { it.toDomain() } }

    override fun getCategories(providerId: Long): Flow<List<Category>> =
        categoryDao.getByProviderAndType(providerId, ContentType.MOVIE.name)
            .map { entities -> entities.map { it.toDomain() } }

    override fun searchMovies(providerId: Long, query: String): Flow<List<Movie>> =
        movieDao.search(providerId, query).map { entities -> entities.map { it.toDomain() } }

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
}
