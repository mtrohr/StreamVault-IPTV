package com.streamvault.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Favorite
import com.streamvault.domain.model.VirtualGroup
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.FavoriteRepository
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteUiModel(
    val favorite: Favorite,
    val title: String,
    val subtitle: String? = null,
    val streamUrl: String = ""
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val channelRepository: ChannelRepository,
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoriteRepository.getFavorites(null)
                .flatMapLatest { favorites ->
                    val channelIds = favorites.filter { it.contentType == ContentType.LIVE }.map { it.contentId }
                    val movieIds = favorites.filter { it.contentType == ContentType.MOVIE }.map { it.contentId }
                    val seriesIds = favorites.filter { it.contentType == ContentType.SERIES }.map { it.contentId }

                    combine(
                        if (channelIds.isEmpty()) flowOf(emptyList()) else channelRepository.getChannelsByIds(channelIds),
                        if (movieIds.isEmpty()) flowOf(emptyList()) else movieRepository.getMoviesByIds(movieIds),
                        if (seriesIds.isEmpty()) flowOf(emptyList()) else seriesRepository.getSeriesByIds(seriesIds)
                    ) { channels, movies, series ->
                        val channelsById = channels.associateBy { it.id }
                        val moviesById = movies.associateBy { it.id }
                        val seriesById = series.associateBy { it.id }

                        favorites.mapNotNull { favorite ->
                            when (favorite.contentType) {
                                ContentType.LIVE -> channelsById[favorite.contentId]?.let { channel ->
                                    FavoriteUiModel(
                                        favorite = favorite,
                                        title = channel.name,
                                        subtitle = "Channel ${channel.number}",
                                        streamUrl = channel.streamUrl
                                    )
                                }
                                ContentType.MOVIE -> moviesById[favorite.contentId]?.let { movie ->
                                    FavoriteUiModel(
                                        favorite = favorite,
                                        title = movie.name,
                                        subtitle = "Movie",
                                        streamUrl = movie.streamUrl
                                    )
                                }
                                ContentType.SERIES -> seriesById[favorite.contentId]?.let { seriesItem ->
                                    FavoriteUiModel(
                                        favorite = favorite,
                                        title = seriesItem.name,
                                        subtitle = "Series",
                                        streamUrl = "" // Series doesn't have a single stream URL
                                    )
                                }
                                else -> null
                            }
                        }
                    }
                }
                .collect { uiModels ->
                    _uiState.update { it.copy(favorites = uiModels, isLoading = false) }
                }
        }
        viewModelScope.launch {
            favoriteRepository.getGroups(ContentType.LIVE).collect { groups ->
                _uiState.update { it.copy(groups = groups) }
            }
        }
    }

    fun enterReorderMode(item: FavoriteUiModel) {
        _uiState.update { it.copy(isReorderMode = true, reorderItem = item.favorite) }
    }

    fun exitReorderMode() {
        _uiState.update { it.copy(isReorderMode = false, reorderItem = null) }
    }

    fun moveItem(direction: Int) {
        val currentList = _uiState.value.favorites.toMutableList()
        val reorderItem = _uiState.value.reorderItem ?: return
        val index = currentList.indexOfFirst { it.favorite.id == reorderItem.id }
        if (index == -1) return

        val newIndex = index + direction
        if (newIndex in 0 until currentList.size) {
            java.util.Collections.swap(currentList, index, newIndex)
            _uiState.update { it.copy(favorites = currentList) }
        }
    }

    fun saveReorder() {
        val currentModels = _uiState.value.favorites
        val updatedFavorites = currentModels.map { it.favorite }
        viewModelScope.launch {
            favoriteRepository.reorderFavorites(updatedFavorites)
            exitReorderMode()
        }
    }
}

data class FavoritesUiState(
    val favorites: List<FavoriteUiModel> = emptyList(),
    val groups: List<VirtualGroup> = emptyList(),
    val isLoading: Boolean = true,
    val isReorderMode: Boolean = false,
    val reorderItem: Favorite? = null
)
