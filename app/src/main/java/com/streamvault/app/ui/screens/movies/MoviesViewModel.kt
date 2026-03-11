package com.streamvault.app.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.PlaybackHistoryRepository
import com.streamvault.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.repository.FavoriteRepository
import com.streamvault.domain.usecase.GetCustomCategories

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val movieRepository: MovieRepository,
    private val preferencesRepository: PreferencesRepository,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val favoriteRepository: FavoriteRepository,
    private val getCustomCategories: GetCustomCategories
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _localMovies = MutableStateFlow<List<Movie>>(emptyList())

    init {
        viewModelScope.launch {
            providerRepository.getActiveProvider()
                .filterNotNull()
                .collectLatest { provider ->
                    // Movies by category
                    launch {
                        combine(
                            movieRepository.getMovies(provider.id),
                            favoriteRepository.getAllFavorites(ContentType.MOVIE),
                            getCustomCategories(ContentType.MOVIE),
                            _searchQuery
                        ) { movies, allFavorites, customCats, query ->
                            // 1. Mark global favorites
                            val globalFavoriteIds = allFavorites.filter { it.groupId == null }.map { it.contentId }.toSet()
                            val enrichedMovies = movies.map { it.copy(isFavorite = globalFavoriteIds.contains(it.id)) }

                            // 2. Filter by search query
                            val filtered = if (query.isBlank()) enrichedMovies
                            else enrichedMovies.filter { it.name.contains(query, ignoreCase = true) }

                            // 3. Group by default category
                            val grouped = filtered.groupBy { it.categoryName ?: "Uncategorized" }.toMutableMap()

                            // 4. Always add "★ Favorites" even if empty
                            grouped["★ Favorites"] = enrichedMovies.filter { it.isFavorite }

                            // 5. Always add Custom Groups even if empty, and populate them
                            customCats.filter { it.id != -999L }.forEach { customCategory ->
                                val groupId = -customCategory.id // Restore actual group DB ID
                                val movieIdsInGroup = allFavorites.filter { it.groupId == groupId }.map { it.contentId }.toSet()
                                grouped[customCategory.name] = enrichedMovies.filter { movieIdsInGroup.contains(it.id) }
                            }

                            // 6. Sort category names, ensuring "★ Favorites" is first, followed by custom groups
                            val customNames = customCats.map { it.name }
                            val categoryNames = grouped.keys.sortedWith(compareBy<String> {
                                when (it) {
                                    "★ Favorites" -> 0
                                    in customNames -> 1
                                    else -> 2
                                }
                            }.thenBy { it })

                            grouped to categoryNames
                        }.collect { (grouped, categoryNames) ->
                            val isReordering = _uiState.value.isReorderMode
                            _uiState.update { it.copy(
                                moviesByCategory = grouped,
                                categoryNames = categoryNames,
                                filteredMovies = if (isReordering) it.filteredMovies else emptyList(),
                                isLoading = false
                            ) }
                        }
                    }
                    // Continue Watching — last 20 movie/series items for this provider
                    launch {
                        playbackHistoryRepository.getRecentlyWatchedByProvider(provider.id, limit = 20)
                            .collect { history ->
                                _uiState.update { it.copy(continueWatching = history) }
                            }
                    }
                }
        }

        viewModelScope.launch {
            preferencesRepository.parentalControlLevel.collect { level ->
                _uiState.update { it.copy(parentalControlLevel = level) }
            }
        }
        
        viewModelScope.launch {
            getCustomCategories(ContentType.MOVIE).collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun selectCategory(categoryName: String?) {
        _uiState.update { it.copy(selectedCategory = categoryName) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return preferencesRepository.verifyParentalPin(pin)
    }

    fun onShowDialog(movie: Movie) {
        viewModelScope.launch {
            val memberships = favoriteRepository.getGroupMemberships(movie.id, ContentType.MOVIE)
            val isFav = favoriteRepository.isFavorite(movie.id, ContentType.MOVIE)
            val updatedMovie = movie.copy(isFavorite = isFav)
            _uiState.update { it.copy(
                showDialog = true, 
                selectedMovieForDialog = updatedMovie,
                dialogGroupMemberships = memberships
            ) }
        }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showDialog = false, selectedMovieForDialog = null) }
    }

    fun addFavorite(movie: Movie) {
        viewModelScope.launch {
            favoriteRepository.addFavorite(movie.id, ContentType.MOVIE)
            _uiState.update { it.copy(selectedMovieForDialog = movie.copy(isFavorite = true)) }
        }
    }

    fun removeFavorite(movie: Movie) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(movie.id, ContentType.MOVIE)
            _uiState.update { it.copy(selectedMovieForDialog = movie.copy(isFavorite = false)) }
        }
    }

    fun addToGroup(movie: Movie, group: Category) {
        viewModelScope.launch {
            val groupId = -group.id
            favoriteRepository.addFavorite(movie.id, ContentType.MOVIE, groupId)
            val memberships = favoriteRepository.getGroupMemberships(movie.id, ContentType.MOVIE)
            _uiState.update { it.copy(dialogGroupMemberships = memberships) }
        }
    }

    fun removeFromGroup(movie: Movie, group: Category) {
        viewModelScope.launch {
            val groupId = -group.id
            favoriteRepository.removeFavorite(movie.id, ContentType.MOVIE, groupId)
            val memberships = favoriteRepository.getGroupMemberships(movie.id, ContentType.MOVIE)
            _uiState.update { it.copy(dialogGroupMemberships = memberships) }
        }
    }

    fun createCustomGroup(name: String) {
        viewModelScope.launch {
            favoriteRepository.createGroup(name, contentType = ContentType.MOVIE)
        }
    }

    // ── Category Options (Long-Press) ──────────────────────────────────
    fun showCategoryOptions(categoryName: String) {
        // Find matching Category object or construct a dummy Virtual mapping
        val matchedCategory = _uiState.value.categories.find { it.name == categoryName }
            ?: if (categoryName == "★ Favorites") Category(id = -999L, name = "★ Favorites", type = ContentType.MOVIE, isVirtual = true) else null
        
        if (matchedCategory != null) {
            _uiState.update { it.copy(selectedCategoryForOptions = matchedCategory) }
        }
    }

    fun dismissCategoryOptions() {
        _uiState.update { it.copy(selectedCategoryForOptions = null) }
    }

    fun requestDeleteGroup(category: Category) {
        if (!category.isVirtual || category.id == -999L) return
        _uiState.update { it.copy(showDeleteGroupDialog = true, groupToDelete = category) }
    }

    fun cancelDeleteGroup() {
        _uiState.update { it.copy(showDeleteGroupDialog = false, groupToDelete = null) }
    }

    fun confirmDeleteGroup() {
        val category = _uiState.value.groupToDelete ?: return
        viewModelScope.launch {
            favoriteRepository.deleteGroup(-category.id)
            _uiState.update {
                it.copy(
                    showDeleteGroupDialog = false,
                    groupToDelete = null
                )
            }
        }
    }

    // ── Reorder ────────────────────────────────────────────────────────
    fun enterCategoryReorderMode(category: Category) {
        dismissCategoryOptions()
        // Capture a local snapshot of the current view to allow instant drag-and-drop
        val currentCategoryName = category.name
        val moviesInView = _uiState.value.moviesByCategory[currentCategoryName] ?: emptyList()
        _localMovies.value = moviesInView
        
        _uiState.update { it.copy(
            isReorderMode = true, 
            reorderCategory = category,
            filteredMovies = moviesInView
        ) }
    }

    fun exitCategoryReorderMode() {
        _uiState.update { it.copy(
            isReorderMode = false, 
            reorderCategory = null,
            filteredMovies = emptyList() // Clear local snapshot rendering
        ) }
    }

    fun moveItemUp(movie: Movie) {
        val state = _uiState.value
        val list = state.filteredMovies.toMutableList()
        val idx = list.indexOf(movie)
        if (idx > 0) {
            list.removeAt(idx)
            list.add(idx - 1, movie)
            _uiState.update { it.copy(filteredMovies = list) }
        }
    }

    fun moveItemDown(movie: Movie) {
        val state = _uiState.value
        val list = state.filteredMovies.toMutableList()
        val idx = list.indexOf(movie)
        if (idx >= 0 && idx < list.size - 1) {
            list.removeAt(idx)
            list.add(idx + 1, movie)
            _uiState.update { it.copy(filteredMovies = list) }
        }
    }

    fun saveReorder() {
        val state = _uiState.value
        val category = state.reorderCategory ?: return
        val currentList = state.filteredMovies

        // Provide an instant UI reversion out of reorder state
        exitCategoryReorderMode()

        viewModelScope.launch {
            try {
                val groupId = if (category.id == -999L) null else -category.id
                val favoritesFlow = if (groupId == null) {
                    favoriteRepository.getFavorites(ContentType.MOVIE)
                } else {
                    favoriteRepository.getFavoritesByGroup(groupId)
                }

                val favorites = favoritesFlow.first()
                val favoriteMap = favorites.associateBy { it.contentId }

                val reorderedFavorites = currentList.mapNotNull { mov ->
                    favoriteMap[mov.id]
                }.mapIndexed { i, fav ->
                    fav.copy(position = i)
                }

                favoriteRepository.reorderFavorites(reorderedFavorites)
            } catch (e: Exception) {
                // Ignore silently for now
            }
        }
    }
}

data class MoviesUiState(
    val moviesByCategory: Map<String, List<Movie>> = emptyMap(),
    val categoryNames: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val continueWatching: List<PlaybackHistory> = emptyList(),
    val isLoading: Boolean = true,
    val parentalControlLevel: Int = 0,
    val showDialog: Boolean = false,
    val selectedMovieForDialog: Movie? = null,
    val categories: List<Category> = emptyList(),
    val dialogGroupMemberships: List<Long> = emptyList(),
    
    // Category Options & Deletion
    val selectedCategoryForOptions: Category? = null,
    val showDeleteGroupDialog: Boolean = false,
    val groupToDelete: Category? = null,
    
    // Reorder State
    val isReorderMode: Boolean = false,
    val reorderCategory: Category? = null,
    val filteredMovies: List<Movie> = emptyList() // The active sortable list
)
