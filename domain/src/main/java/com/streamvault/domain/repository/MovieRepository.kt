package com.streamvault.domain.repository

import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getMovies(providerId: Long): Flow<List<Movie>>
    fun getMoviesByCategory(providerId: Long, categoryId: Long): Flow<List<Movie>>
    fun getMoviesByIds(ids: List<Long>): Flow<List<Movie>>
    fun getCategories(providerId: Long): Flow<List<Category>>
    fun searchMovies(providerId: Long, query: String): Flow<List<Movie>>
    suspend fun getMovie(movieId: Long): Movie?
    suspend fun getMovieDetails(providerId: Long, movieId: Long): Result<Movie>
    suspend fun getStreamUrl(movie: Movie): Result<String>
    suspend fun refreshMovies(providerId: Long): Result<Unit>
    suspend fun updateWatchProgress(movieId: Long, progress: Long)
}
