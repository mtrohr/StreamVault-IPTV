package com.streamvault.app.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.ui.components.CategoryRow
import com.streamvault.app.ui.components.MovieCard
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Movie
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepository.getActiveProvider()
                .filterNotNull()
                .collectLatest { provider ->
                    movieRepository.getMovies(provider.id).collect { movies ->
                        val grouped = movies.groupBy { it.categoryName ?: "Uncategorized" }
                        _uiState.update {
                            it.copy(moviesByCategory = grouped, isLoading = false)
                        }
                    }
                }
        }
    }
}

data class MoviesUiState(
    val moviesByCategory: Map<String, List<Movie>> = emptyMap(),
    val isLoading: Boolean = true
)

@Composable
fun MoviesScreen(
    onMovieClick: (Movie) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopNavBar(currentRoute = currentRoute, onNavigate = onNavigate)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading movies...", style = MaterialTheme.typography.bodyLarge, color = OnSurface)
            }
        } else if (uiState.moviesByCategory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("No movies found", style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = uiState.moviesByCategory.entries.toList(),
                    key = { it.key }
                ) { (categoryName, movies) ->
                    CategoryRow(title = categoryName, items = movies) { movie ->
                        MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
            }
        }
    }
}
