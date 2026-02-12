package com.streamvault.app.ui.screens.home

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.ui.components.CategoryRow
import com.streamvault.app.ui.components.ChannelCard
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Provider
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.EpgRepository
import com.streamvault.domain.repository.ProviderRepository
import kotlinx.coroutines.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.streamvault.domain.usecase.GetCustomCategories

// ── ViewModel ──────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: com.streamvault.domain.repository.FavoriteRepository,
    private val preferencesRepository: com.streamvault.data.preferences.PreferencesRepository,
    private val epgRepository: EpgRepository,
    private val getCustomCategories: com.streamvault.domain.usecase.GetCustomCategories
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Cache channels locally for filtering
    private var localChannels: List<Channel> = emptyList()
    private var epgJob: Job? = null

    init {
        loadAllProviders()
        viewModelScope.launch {
            providerRepository.getActiveProvider()
                .filterNotNull()
                .collectLatest { provider ->
                    _uiState.update { it.copy(provider = provider) }
                    loadCategoriesAndChannels(provider.id)
                    // Save as last active
                    preferencesRepository.setLastActiveProviderId(provider.id)
                }
        }
    }

    private fun loadAllProviders() {
        viewModelScope.launch {
            providerRepository.getProviders().collect { providers ->
                _uiState.update { it.copy(allProviders = providers) }
            }
        }
    }

    fun switchProvider(providerId: Long) {
        viewModelScope.launch {
            providerRepository.setActiveProvider(providerId)
        }
    }

    private fun loadCategoriesAndChannels(providerId: Long) {
        viewModelScope.launch {
            // Combine provider categories with custom categories
            combine(
                channelRepository.getCategories(providerId),
                getCustomCategories()
            ) { providerCats, customCats ->
                customCats + providerCats
            }.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
                
                // Select "Favorites" by default if nothing selected
                if (_uiState.value.selectedCategory == null && categories.isNotEmpty()) {
                    // Try to find Favorites (ID -999)
                    val favoritesCat = categories.find { it.id == -999L }
                    if (favoritesCat != null) {
                        selectCategory(favoritesCat)
                    } else {
                        selectCategory(categories.first())
                    }
                } else if (_uiState.value.selectedCategory != null) {
                    // Re-select current category to refresh channels (e.g. if favorites changed)
                     selectCategory(_uiState.value.selectedCategory!!)
                }
            }
        }
    }

    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category, isLoading = true) }
        loadChannelsForCategory(category)
    }

    private fun loadChannelsForCategory(category: Category) {
        viewModelScope.launch {
            val providerId = _uiState.value.provider?.id ?: return@launch
            
            val channelsFlow = if (category.isVirtual) {
                if (category.id == -999L) {
                    // Global Favorites
                    favoriteRepository.getFavorites(com.streamvault.domain.model.ContentType.LIVE)
                        .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
                        .flatMapLatest { ids -> 
                            if (ids.isEmpty()) flowOf(emptyList()) 
                            else channelRepository.getChannelsByIds(ids).map { unsorted ->
                                val map = unsorted.associateBy { it.id }
                                ids.mapNotNull { map[it] }
                            }
                        }
                } else {
                    // Custom Group
                    val groupId = -category.id
                    favoriteRepository.getFavoritesByGroup(groupId)
                        .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
                        .flatMapLatest { ids -> 
                            if (ids.isEmpty()) flowOf(emptyList()) 
                            else channelRepository.getChannelsByIds(ids).map { unsorted ->
                                val map = unsorted.associateBy { it.id }
                                ids.mapNotNull { map[it] }
                            }
                        }
                }
            } else {
                // Regular Category
                 channelRepository.getChannelsByCategory(providerId, category.id)
            }

            channelsFlow.collect { channels ->
                localChannels = channels
                _uiState.update { it.copy(hasChannels = channels.isNotEmpty()) }
                updateFilteredChannels()
            }
        }
    }
    
    fun updateCategorySearchQuery(query: String) {
        _uiState.update { it.copy(categorySearchQuery = query) }
        // We calculate filtered categories in UI or here? 
        // Better to expose filtered list or filter in UI. 
        // For performance, let's filter in UI for now or add a derived state.
    }

    fun updateChannelSearchQuery(query: String) {
        _uiState.update { it.copy(channelSearchQuery = query) }
        updateFilteredChannels()
    }

    private fun updateFilteredChannels() {
        epgJob?.cancel()
        
        val query = _uiState.value.channelSearchQuery.lowercase()
        val filtered = if (query.isBlank()) {
            localChannels
        } else {
            localChannels.filter { it.name.lowercase().contains(query) }
        }
        
        // Update UI with initial list
        _uiState.update { it.copy(filteredChannels = filtered, isLoading = false) }

        // Fetch EPG for these channels
        val epgIds = filtered.mapNotNull { it.epgChannelId }.distinct()
        
        viewModelScope.launch {
            // Get Favorites to mark channels
            val favorites = favoriteRepository.getFavorites(com.streamvault.domain.model.ContentType.LIVE)
                .firstOrNull() ?: emptyList()
            val favoriteIds = favorites.map { it.contentId }.toSet()

            // Fetch EPG
            val programs = if (epgIds.isNotEmpty()) {
                epgRepository.getNowPlayingForChannels(epgIds).firstOrNull() ?: emptyList()
            } else {
                emptyList()
            }
            val programMap = programs.associateBy { it.channelId }
            
            val enrichedChannels = filtered.map { channel ->
                var enriched = channel
                if (favoriteIds.contains(channel.id)) {
                    enriched = enriched.copy(isFavorite = true)
                }
                
                val program = channel.epgChannelId?.let { programMap[it] }
                if (program != null) {
                    enriched = enriched.copy(currentProgram = program)
                }
                enriched
            }
            
            _uiState.update { it.copy(filteredChannels = enrichedChannels) }
        }
    }

    fun addFavorite(channel: Channel) {
        viewModelScope.launch {
            favoriteRepository.addFavorite(
                contentId = channel.id,
                contentType = com.streamvault.domain.model.ContentType.LIVE
            )
            // If currently in Favorites, this might trigger a refresh via flows
        }
    }
    
    fun removeFavorite(channel: Channel) {
        viewModelScope.launch {
             favoriteRepository.removeFavorite(
                contentId = channel.id,
                contentType = com.streamvault.domain.model.ContentType.LIVE
             )
        }
    }

    fun createCustomGroup(name: String) {
        viewModelScope.launch {
            favoriteRepository.createGroup(name)
        }
    }

    fun deleteCustomGroup(category: Category) {
        if (!category.isVirtual || category.id == -999L) return
        viewModelScope.launch {
            favoriteRepository.deleteGroup(-category.id)
        }
    }

    fun addToGroup(channel: Channel, category: Category) {
        if (!category.isVirtual || category.id == -999L) return
        viewModelScope.launch {
            // This will replace existing favorite entry, effectively moving it to the group
            favoriteRepository.addFavorite(
                contentId = channel.id,
                contentType = com.streamvault.domain.model.ContentType.LIVE,
                groupId = -category.id
            )
        }
    }

    fun moveChannel(channel: Channel, direction: Int) {
        // direction: -1 for UP/Left, 1 for DOWN/Right
        val currentCategory = _uiState.value.selectedCategory ?: return
        if (!currentCategory.isVirtual) return // Only for Favorites/Custom Groups

        val currentList = localChannels.toMutableList()
        val index = currentList.indexOfFirst { it.id == channel.id }
        if (index == -1) return
        
        val newIndex = index + direction
        if (newIndex < 0 || newIndex >= currentList.size) return
        
        // Optimistic UI update
        java.util.Collections.swap(currentList, index, newIndex)
        localChannels = currentList
        updateFilteredChannels()
        
        // Persist change
        viewModelScope.launch {
            val groupId = if (currentCategory.id == -999L) null else -currentCategory.id
            
            // We need to fetch current favorites to get their IDs and other metadata
            val favoritesCallback = if (groupId == null) {
                favoriteRepository.getFavorites(com.streamvault.domain.model.ContentType.LIVE)
            } else {
                favoriteRepository.getFavoritesByGroup(groupId)
            }
            
            val favorites = favoritesCallback.first()
            val favoriteMap = favorites.associateBy { it.contentId }
            
            // Reconstruct the ordered list of favorites based on the NEW channel order
            val reorderedFavorites = currentList.mapNotNull { ch ->
                favoriteMap[ch.id]
            }.mapIndexed { i, fav ->
                fav.copy(position = i)
            }
            
            favoriteRepository.reorderFavorites(reorderedFavorites)
        }
    }

    fun refreshData() {
        val provider = _uiState.value.provider ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            providerRepository.refreshProviderData(provider.id)
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    fun onShowDialog(channel: Channel) {
        _uiState.update { it.copy(showDialog = true, selectedChannelForDialog = channel) }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showDialog = false, selectedChannelForDialog = null) }
    }
}

data class HomeUiState(
    val provider: Provider? = null,
    val allProviders: List<Provider> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val filteredChannels: List<Channel> = emptyList(),
    val hasChannels: Boolean = false,
    val isLoading: Boolean = true,
    val categorySearchQuery: String = "",
    val channelSearchQuery: String = "",
    val showDialog: Boolean = false,
    val selectedChannelForDialog: Channel? = null
)

// ── Screen ─────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onChannelClick: (Channel, Category?, Provider?) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1f)
            )
            
            // Playlist Switcher
            if (uiState.allProviders.size > 1) {
                com.streamvault.app.ui.components.PlaylistSwitcher(
                    currentProvider = uiState.provider,
                    allProviders = uiState.allProviders,
                    onProviderSelected = { provider ->
                        viewModel.switchProvider(provider.id)
                    },
                    modifier = Modifier.padding(end = 32.dp)
                )
            }
        }

        if (uiState.isLoading && uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading channels...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface
                )
            }
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar - Categories
                LazyColumn(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(SurfaceElevated)
                        .padding(vertical = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                            )
                            SearchInput(
                                value = uiState.categorySearchQuery,
                                onValueChange = { viewModel.updateCategorySearchQuery(it) },
                                placeholder = "Search categories...",
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                    
                    items(
                        items = uiState.categories.filter { 
                            uiState.categorySearchQuery.isEmpty() || 
                            it.name.contains(uiState.categorySearchQuery, ignoreCase = true) 
                        },
                        key = { it.id }
                    ) { category ->
                        CategoryItem(
                            category = category,
                            isSelected = category.id == uiState.selectedCategory?.id,
                            onClick = { viewModel.selectCategory(category) }
                        )
                    }
                }
                
                // Content - Channel Grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Category Title Header and Search
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 24.dp, bottom = 16.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.selectedCategory?.name ?: "All Channels",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnBackground
                        )
                        
                        SearchInput(
                            value = uiState.channelSearchQuery,
                            onValueChange = { viewModel.updateChannelSearchQuery(it) },
                            placeholder = "Search channels...",
                            modifier = Modifier.width(300.dp)
                        )
                    }
                    
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnSurface
                            )
                        }
                    } else if (!uiState.hasChannels) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "📺",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No channels found in this category",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnSurface
                                )
                                val selectedCategory = uiState.selectedCategory
                                if (selectedCategory?.isVirtual == true && selectedCategory.id == -999L) {
                                    Text(
                                        text = "Add channels to favorites to see them here",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceDim,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = uiState.filteredChannels,
                                key = { it.id }
                            ) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    onClick = { onChannelClick(channel, uiState.selectedCategory, uiState.provider) },
                                    onLongClick = {
                                        viewModel.onShowDialog(channel)
                                    },
                                    modifier = Modifier.aspectRatio(16f/9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDialog && uiState.selectedChannelForDialog != null) {
        val channel = uiState.selectedChannelForDialog!!
        val context = androidx.compose.ui.platform.LocalContext.current
        com.streamvault.app.ui.components.dialogs.AddToGroupDialog(
            channel = channel,
            groups = uiState.categories.filter { it.isVirtual && it.id != -999L },
            isFavorite = channel.isFavorite,
            onDismiss = { viewModel.onDismissDialog() },
            onToggleFavorite = { 
                if (channel.isFavorite) viewModel.removeFavorite(channel) else viewModel.addFavorite(channel)
                viewModel.onDismissDialog()
            },
            onAddToGroup = { group ->
                viewModel.addToGroup(channel, group)
                viewModel.onDismissDialog()
                android.widget.Toast.makeText(context, "Added to ${group.name}", android.widget.Toast.LENGTH_SHORT).show()
            },
            onRemoveFromGroup = { /* Not implemented yet directly, toggle favorite removes it */ },
            onCreateGroup = { name -> viewModel.createCustomGroup(name) },
            onMoveUp = if (uiState.selectedCategory?.isVirtual == true) { { viewModel.moveChannel(channel, -1) } } else null,
            onMoveDown = if (uiState.selectedCategory?.isVirtual == true) { { viewModel.moveChannel(channel, 1) } } else null
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.15f) else Color.Transparent,
            focusedContainerColor = SurfaceHighlight,
            contentColor = if (isSelected) Primary else OnSurface
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Text(
            text = category.name,
            style = if (isSelected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 1,
            color = if (isFocused) OnBackground else if (isSelected) Primary else OnSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { 
            Text(
                text = placeholder, 
                style = MaterialTheme.typography.bodyMedium, 
                color = OnSurfaceDim
            ) 
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isFocused) FocusBorder else OnSurfaceDim,
            unfocusedBorderColor = OnSurfaceDim.copy(alpha = 0.5f),
            cursorColor = Primary,
            focusedContainerColor = if (isFocused) SurfaceHighlight else SurfaceElevated,
            unfocusedContainerColor = if (isFocused) SurfaceHighlight else SurfaceElevated,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface
        ),
        singleLine = true,
        leadingIcon = {
            Text("🔍", modifier = Modifier.padding(start = 8.dp))
        },
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
