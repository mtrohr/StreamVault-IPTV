package com.streamvault.app.ui.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.model.Series
import com.streamvault.domain.repository.PlaybackHistoryRepository
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.repository.FavoriteRepository
import com.streamvault.domain.usecase.GetCustomCategories

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val seriesRepository: SeriesRepository,
    private val preferencesRepository: PreferencesRepository,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val favoriteRepository: FavoriteRepository,
    private val getCustomCategories: GetCustomCategories
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _localSeries = MutableStateFlow<List<Series>>(emptyList())

    init {
        viewModelScope.launch {
            providerRepository.getActiveProvider()
                .filterNotNull()
                .collectLatest { provider ->
                    // Series by category
                    launch {
                        combine(
                            seriesRepository.getSeries(provider.id),
                            favoriteRepository.getAllFavorites(ContentType.SERIES),
                            getCustomCategories(ContentType.SERIES),
                            _searchQuery
                        ) { seriesList, allFavorites, customCats, query ->
                            // 1. Mark global favorites
                            val globalFavoriteIds = allFavorites.filter { it.groupId == null }.map { it.contentId }.toSet()
                            val enrichedSeries = seriesList.map { it.copy(isFavorite = globalFavoriteIds.contains(it.id)) }

                            // 2. Filter by search query
                            val filtered = if (query.isBlank()) enrichedSeries
                            else enrichedSeries.filter { it.name.contains(query, ignoreCase = true) }

                            // 3. Group by default category
                            val grouped = filtered.groupBy { it.categoryName ?: "Uncategorized" }.toMutableMap()

                            // 4. Always add "★ Favorites" even if empty
                            grouped["★ Favorites"] = enrichedSeries.filter { it.isFavorite }

                            // 5. Always add Custom Groups even if empty, and populate them
                            customCats.filter { it.id != -999L }.forEach { customCategory ->
                                val groupId = -customCategory.id // Restore actual group DB ID
                                val seriesIdsInGroup = allFavorites.filter { it.groupId == groupId }.map { it.contentId }.toSet()
                                grouped[customCategory.name] = enrichedSeries.filter { seriesIdsInGroup.contains(it.id) }
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
                                seriesByCategory = grouped,
                                categoryNames = categoryNames,
                                filteredSeries = if (isReordering) it.filteredSeries else emptyList(),
                                isLoading = false
                            ) }
                        }
                    }
                    // Continue Watching — last 20 items for this provider
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
            getCustomCategories(ContentType.SERIES).collect { cats ->
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

    fun onShowDialog(series: Series) {
        viewModelScope.launch {
            val memberships = favoriteRepository.getGroupMemberships(series.id, ContentType.SERIES)
            val isFav = favoriteRepository.isFavorite(series.id, ContentType.SERIES)
            val updatedSeries = series.copy(isFavorite = isFav)
            _uiState.update { it.copy(
                showDialog = true, 
                selectedSeriesForDialog = updatedSeries,
                dialogGroupMemberships = memberships
            ) }
        }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showDialog = false, selectedSeriesForDialog = null) }
    }

    fun addFavorite(series: Series) {
        viewModelScope.launch {
            favoriteRepository.addFavorite(series.id, ContentType.SERIES)
            _uiState.update { it.copy(selectedSeriesForDialog = series.copy(isFavorite = true)) }
        }
    }

    fun removeFavorite(series: Series) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(series.id, ContentType.SERIES)
            _uiState.update { it.copy(selectedSeriesForDialog = series.copy(isFavorite = false)) }
        }
    }

    fun addToGroup(series: Series, group: Category) {
        viewModelScope.launch {
            val groupId = -group.id
            favoriteRepository.addFavorite(series.id, ContentType.SERIES, groupId)
            val memberships = favoriteRepository.getGroupMemberships(series.id, ContentType.SERIES)
            _uiState.update { it.copy(dialogGroupMemberships = memberships) }
        }
    }

    fun removeFromGroup(series: Series, group: Category) {
        viewModelScope.launch {
            val groupId = -group.id
            favoriteRepository.removeFavorite(series.id, ContentType.SERIES, groupId)
            val memberships = favoriteRepository.getGroupMemberships(series.id, ContentType.SERIES)
            _uiState.update { it.copy(dialogGroupMemberships = memberships) }
        }
    }

    fun createCustomGroup(name: String) {
        viewModelScope.launch {
            favoriteRepository.createGroup(name, contentType = ContentType.SERIES)
        }
    }

    // ── Category Options (Long-Press) ──────────────────────────────────
    fun showCategoryOptions(categoryName: String) {
        val customCats = _uiState.value.categories.filter { it.isVirtual }
        val matchedCategory = customCats.find { it.name == categoryName }
            ?: if (categoryName == "★ Favorites") Category(id = -999L, name = "★ Favorites", type = ContentType.SERIES, isVirtual = true) else null
        
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
        val currentCategoryName = category.name
        val seriesInView = _uiState.value.seriesByCategory[currentCategoryName] ?: emptyList()
        _localSeries.value = seriesInView
        
        _uiState.update { it.copy(
            isReorderMode = true, 
            reorderCategory = category,
            filteredSeries = seriesInView
        ) }
    }

    fun exitCategoryReorderMode() {
        _uiState.update { it.copy(
            isReorderMode = false, 
            reorderCategory = null,
            filteredSeries = emptyList()
        ) }
    }

    fun moveItemUp(series: Series) {
        val state = _uiState.value
        val list = state.filteredSeries.toMutableList()
        val idx = list.indexOf(series)
        if (idx > 0) {
            list.removeAt(idx)
            list.add(idx - 1, series)
            _uiState.update { it.copy(filteredSeries = list) }
        }
    }

    fun moveItemDown(series: Series) {
        val state = _uiState.value
        val list = state.filteredSeries.toMutableList()
        val idx = list.indexOf(series)
        if (idx >= 0 && idx < list.size - 1) {
            list.removeAt(idx)
            list.add(idx + 1, series)
            _uiState.update { it.copy(filteredSeries = list) }
        }
    }

    fun saveReorder() {
        val state = _uiState.value
        val category = state.reorderCategory ?: return
        val currentList = state.filteredSeries

        exitCategoryReorderMode()

        viewModelScope.launch {
            try {
                val groupId = if (category.id == -999L) null else -category.id
                val favoritesFlow = if (groupId == null) {
                    favoriteRepository.getFavorites(ContentType.SERIES)
                } else {
                    favoriteRepository.getFavoritesByGroup(groupId)
                }

                val favorites = favoritesFlow.first()
                val favoriteMap = favorites.associateBy { it.contentId }

                val reorderedFavorites = currentList.mapNotNull { ser ->
                    favoriteMap[ser.id]
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

data class SeriesUiState(
    val seriesByCategory: Map<String, List<Series>> = emptyMap(),
    val categoryNames: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val continueWatching: List<PlaybackHistory> = emptyList(),
    val isLoading: Boolean = true,
    val parentalControlLevel: Int = 0,
    val showDialog: Boolean = false,
    val selectedSeriesForDialog: Series? = null,
    val categories: List<Category> = emptyList(),
    val dialogGroupMemberships: List<Long> = emptyList(),
    
    // Category Options & Deletion
    val selectedCategoryForOptions: Category? = null,
    val showDeleteGroupDialog: Boolean = false,
    val groupToDelete: Category? = null,
    
    // Reorder State
    val isReorderMode: Boolean = false,
    val reorderCategory: Category? = null,
    val filteredSeries: List<Series> = emptyList()
)
