package com.streamvault.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.ui.components.ChannelCard
import com.streamvault.app.ui.components.MovieCard
import com.streamvault.app.ui.components.SeriesCard
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Series
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val channelRepository: ChannelRepository,
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedTab = MutableStateFlow(SearchTab.ALL)
    val selectedTab: StateFlow<SearchTab> = _selectedTab.asStateFlow()

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<SearchUiState> = combine(
        providerRepository.getActiveProvider(),
        _query.debounce(300),
        _selectedTab
    ) { provider, query, tab ->
        Triple(provider, query, tab)
    }.flatMapLatest { (provider, query, tab) ->
        if (provider == null || query.length < 2) {
            flowOf(SearchUiState())
        } else {
            val providerId = provider.id
            combine(
                if (tab == SearchTab.ALL || tab == SearchTab.LIVE) 
                    channelRepository.searchChannels(providerId, query) else flowOf(emptyList()),
                if (tab == SearchTab.ALL || tab == SearchTab.MOVIES) 
                    movieRepository.searchMovies(providerId, query) else flowOf(emptyList()),
                if (tab == SearchTab.ALL || tab == SearchTab.SERIES) 
                    seriesRepository.searchSeries(providerId, query) else flowOf(emptyList())
            ) { channels, movies, series ->
                SearchUiState(
                    channels = channels,
                    movies = movies,
                    series = series,
                    isLoading = false,
                    hasSearched = true
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onTabSelected(tab: SearchTab) {
        _selectedTab.value = tab
    }
}

enum class SearchTab(val title: String) {
    ALL("All"),
    LIVE("Live TV"),
    MOVIES("Movies"),
    SERIES("Series")
}

data class SearchUiState(
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false
) {
    val isEmpty: Boolean get() = hasSearched && channels.isEmpty() && movies.isEmpty() && series.isEmpty()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Search Bar
        SearchTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            onDone = { focusManager.clearFocus() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SearchTab.values().forEach { tab ->
                FilterChip(
                    selected = tab == selectedTab,
                    onClick = { viewModel.onTabSelected(tab) },
                    content = { Text(tab.title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Results
        if (uiState.isEmpty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (query.length < 2) "Type to search..." else "No results found for '$query'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceDim
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Channels
                if (uiState.channels.isNotEmpty()) {
                    if (selectedTab == SearchTab.ALL) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Live TV")
                        }
                    }
                    items(uiState.channels) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            modifier = Modifier.aspectRatio(16f/9f)
                        )
                    }
                }

                // Movies
                if (uiState.movies.isNotEmpty()) {
                    if (selectedTab == SearchTab.ALL) {
                         item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Movies")
                        }
                    }
                    items(uiState.movies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) },
                            modifier = Modifier.aspectRatio(2f/3f)
                        )
                    }
                }

                // Series
                if (uiState.series.isNotEmpty()) {
                    if (selectedTab == SearchTab.ALL) {
                         item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Series")
                        }
                    }
                    items(uiState.series) { series ->
                        SeriesCard(
                            series = series,
                            onClick = { onSeriesClick(series) },
                            modifier = Modifier.aspectRatio(2f/3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (isFocused) Surface else SurfaceElevated, RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Primary else SurfaceHighlight,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty() && !isFocused) {
            Text("Search...", color = OnSurfaceDim)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnBackground),
            singleLine = true,
            cursorBrush = SolidColor(Primary)
        )
    }
}
