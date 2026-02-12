package com.streamvault.app.ui.screens.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Favorite
import com.streamvault.domain.model.VirtualGroup
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.FavoriteRepository
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
            favoriteRepository.getFavorites(null).collect { favorites ->
                val uiModels = favorites.mapNotNull { fav ->
                    // This is simple N+1 fetching for now. 
                    // Optimization: Bulk fetch or join in DB in future iterations.
                    try {
                        when (fav.contentType) {
                            ContentType.LIVE -> {
                                val channel = channelRepository.getChannel(fav.contentId)
                                if (channel != null) { 
                                    FavoriteUiModel(
                                        favorite = fav,
                                        title = channel.name,
                                        subtitle = "Channel ${channel.number}",
                                        streamUrl = channel.streamUrl
                                    )
                                } else null
                            }
                            ContentType.MOVIE -> {
                                val movie = movieRepository.getMovie(fav.contentId)
                                if (movie != null) {
                                    FavoriteUiModel(
                                        favorite = fav,
                                        title = movie.name,
                                        subtitle = "Movie",
                                        streamUrl = movie.streamUrl
                                    )
                                } else null
                            }
                            ContentType.SERIES -> {
                                val series = seriesRepository.getSeriesById(fav.contentId)
                                if (series != null) {
                                    FavoriteUiModel(
                                        favorite = fav,
                                        title = series.name,
                                        subtitle = "Series",
                                        streamUrl = "" // Series doesn't have a single stream URL
                                    )
                                } else null
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                _uiState.update { it.copy(favorites = uiModels, isLoading = false) }
            }
        }
        viewModelScope.launch {
            favoriteRepository.getGroups().collect { groups ->
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
        // We need to pass just the Favorites, with updated positions implicitly defined by list order?
        // Actually Repository expects a list and updates positions based on index.
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

@Composable
fun FavoritesScreen(
    onItemClick: (streamUrl: String, title: String) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopNavBar(currentRoute = currentRoute, onNavigate = onNavigate)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading favorites...", style = MaterialTheme.typography.bodyLarge, color = OnSurface)
            }
        } else if (uiState.favorites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("No favorites yet", style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                    Text(
                        "Long-press on any channel, movie, or series to add it to favorites",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Virtual groups
                if (uiState.groups.isNotEmpty()) {
                    items(uiState.groups, key = { it.id }) { group ->
                        Text(
                            text = "${group.iconEmoji ?: "📁"} ${group.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnBackground,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // All favorites
                items(uiState.favorites, key = { it.favorite.id }) { item ->
                    val isReorderingThis = uiState.isReorderMode && uiState.reorderItem?.id == item.favorite.id
                    val scale by animateFloatAsState(if (isReorderingThis) 1.05f else 1f)
                    
                    Surface(
                        onClick = {
                            if (uiState.isReorderMode) {
                                if (isReorderingThis) viewModel.saveReorder()
                            } else {
                                if (item.favorite.contentType == ContentType.SERIES) {
                                    onNavigate("series_detail/${item.favorite.contentId}")
                                } else {
                                    onItemClick(item.streamUrl, item.title)
                                }
                            }
                        },
                        onLongClick = {
                            viewModel.enterReorderMode(item)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(scale)
                            .then(
                                if (isReorderingThis) {
                                    Modifier.onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            when (event.nativeKeyEvent.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                    viewModel.moveItem(-1)
                                                    true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    viewModel.moveItem(1)
                                                    true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                                                    viewModel.saveReorder()
                                                    true
                                                }
                                                android.view.KeyEvent.KEYCODE_BACK -> {
                                                    viewModel.exitReorderMode()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                } else Modifier
                            ),
                        shape = ClickableSurfaceDefaults.shape(
                            RoundedCornerShape(8.dp)
                        ),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isReorderingThis) Primary.copy(alpha = 0.2f) else SurfaceElevated,
                            focusedContainerColor = if (isReorderingThis) Primary else SurfaceHighlight,
                            contentColor = if (isReorderingThis) Primary else OnSurface,
                            focusedContentColor = if (isReorderingThis) OnPrimary else OnSurface
                        ),
                        border = ClickableSurfaceDefaults.border(
                            border = Border(
                                border = BorderStroke(
                                    width = if (isReorderingThis) 2.dp else 0.dp,
                                    color = if (isReorderingThis) Primary else Color.Transparent
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isReorderingThis) Primary else OnSurface
                            )
                            Spacer(Modifier.weight(1f))
                            if (isReorderingThis) {
                                Text("↕ Reordering", style = MaterialTheme.typography.bodySmall, color = Primary)
                            } else {
                                Text(
                                    text = item.subtitle ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceDim
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (uiState.isReorderMode) {
        BackHandler {
            viewModel.exitReorderMode()
        }
    }
}
