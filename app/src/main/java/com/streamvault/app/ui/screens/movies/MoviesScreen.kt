package com.streamvault.app.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.streamvault.app.ui.components.SearchInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.streamvault.app.navigation.Routes
import com.streamvault.app.ui.components.CategoryRow
import com.streamvault.app.ui.components.ContinueWatchingRow
import com.streamvault.app.ui.components.MovieCard
import com.streamvault.app.ui.components.SelectionChip
import com.streamvault.app.ui.components.SelectionChipRow
import com.streamvault.app.ui.components.SavedCategoryContextCard
import com.streamvault.app.ui.components.SavedCategoryShortcut
import com.streamvault.app.ui.components.SavedCategoryShortcutsRow
import com.streamvault.app.ui.components.SkeletonRow
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Movie
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.border
import androidx.compose.runtime.saveable.rememberSaveable
import com.streamvault.app.ui.components.ReorderTopBar
import com.streamvault.app.ui.components.dialogs.DeleteGroupDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.tv.material3.Border
import com.streamvault.app.ui.components.dialogs.RenameGroupDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoviesScreen(
    onMovieClick: (Movie) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pendingMovie by remember { mutableStateOf<Movie?>(null) }
    var selectedLibraryLens by rememberSaveable { mutableStateOf(MovieLibraryLens.FAVORITES.name) }
    var selectedFacet by rememberSaveable { mutableStateOf(MovieLibraryFacet.ALL.name) }
    var selectedSort by rememberSaveable { mutableStateOf(MovieLibrarySort.LIBRARY.name) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.userMessageShown()
        }
    }

    if (showPinDialog) {
        com.streamvault.app.ui.components.dialogs.PinDialog(
            onDismissRequest = {
                showPinDialog = false
                pinError = null
                pendingMovie = null
            },
            onPinEntered = { pin ->
                scope.launch {
                    if (viewModel.verifyPin(pin)) {
                        showPinDialog = false
                        pinError = null
                        pendingMovie?.let { onMovieClick(it) }
                        pendingMovie = null
                    } else {
                        pinError = context.getString(R.string.movies_incorrect_pin)
                    }
                }
            },
            error = pinError
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isReorderMode && uiState.reorderCategory != null) {
            ReorderTopBar(
                categoryName = uiState.reorderCategory!!.name,
                onSave = { viewModel.saveReorder() },
                onCancel = { viewModel.exitCategoryReorderMode() }
            )
        } else {
            TopNavBar(currentRoute = currentRoute, onNavigate = onNavigate)
        }

        if (uiState.isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(3) {
                    SkeletonRow(
                        modifier = Modifier.fillMaxWidth(),
                        cardWidth = 160,
                        cardHeight = 240,
                        itemsCount = 5
                    )
                }
            }
        } else if (uiState.moviesByCategory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.movies_no_found), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                val focusManager = LocalFocusManager.current
                val categorySearchFocusRequester = remember { FocusRequester() }
                val categoryFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
                var lastFocusedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
                var shouldRestoreCategoryFocus by remember { mutableStateOf(false) }
                var preferCategoryRestore by rememberSaveable { mutableStateOf(false) }
                val heroMovie = remember(uiState.categoryNames, uiState.moviesByCategory) {
                    uiState.categoryNames
                        .asSequence()
                        .mapNotNull { categoryName -> uiState.moviesByCategory[categoryName]?.firstOrNull() }
                        .firstOrNull()
                }

                LaunchedEffect(
                    uiState.selectedCategoryForOptions,
                    uiState.showRenameGroupDialog,
                    uiState.showDeleteGroupDialog
                ) {
                    val modalClosed =
                        uiState.selectedCategoryForOptions == null &&
                            !uiState.showRenameGroupDialog &&
                            !uiState.showDeleteGroupDialog
                    if (modalClosed && preferCategoryRestore && lastFocusedCategoryName != null) {
                        shouldRestoreCategoryFocus = true
                        preferCategoryRestore = false
                    }
                }

                LaunchedEffect(shouldRestoreCategoryFocus, uiState.categoryNames) {
                    if (!shouldRestoreCategoryFocus) return@LaunchedEffect
                    kotlinx.coroutines.delay(80)
                    val categoryName = lastFocusedCategoryName
                    if (categoryName != null) {
                        runCatching {
                            categoryFocusRequesters[categoryName]?.requestFocus()
                        }
                    }
                    shouldRestoreCategoryFocus = false
                }

                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(SurfaceElevated.copy(alpha = 0.5f))
                        .padding(top = 8.dp)
                ) {
                    // Sticky Header
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = stringResource(R.string.movies_categories_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        SearchInput(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = stringResource(R.string.movies_search_placeholder),
                            focusRequester = categorySearchFocusRequester,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                    item {
                        val isAllSelected = uiState.selectedCategory == null || uiState.selectedCategory == uiState.fullLibraryCategoryName
                        Surface(
                            onClick = { viewModel.selectCategory(null) },
                            shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isAllSelected) Primary.copy(alpha = 0.15f) else Color.Transparent,
                                focusedContainerColor = Primary.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .onPreviewKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                                            categorySearchFocusRequester.requestFocus()
                                            true
                                        } else false
                                    } else false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.movies_all_categories),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isAllSelected) Primary else OnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${uiState.libraryCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceDim
                                )
                            }
                        }
                    }
                    items(uiState.categoryNames.size) { index ->
                        val categoryName = uiState.categoryNames[index]
                        val isSelected = uiState.selectedCategory == categoryName
                        val count = uiState.categoryCounts[categoryName] ?: 0
                        Surface(
                            onClick = { viewModel.selectCategory(categoryName) },
                            onLongClick = {
                                preferCategoryRestore = true
                                viewModel.showCategoryOptions(categoryName)
                            },
                            shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) Primary.copy(alpha = 0.15f) else Color.Transparent,
                                focusedContainerColor = Primary.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .focusRequester(categoryFocusRequesters.getOrPut(categoryName) { FocusRequester() })
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        lastFocusedCategoryName = categoryName
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                                            categorySearchFocusRequester.requestFocus()
                                            true
                                        } else false
                                    } else false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Primary else OnSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceDim
                                )
                            }
                        }
                    }
                }
                }

                val savedShortcuts = remember(
                    uiState.favoriteCategoryName,
                    uiState.categories,
                    uiState.moviesByCategory
                ) {
                    buildList {
                        val favoriteCount = uiState.categoryCounts[uiState.favoriteCategoryName] ?: 0
                        if (favoriteCount > 0) {
                            add(
                                SavedCategoryShortcut(
                                    name = uiState.favoriteCategoryName,
                                    count = favoriteCount
                                )
                            )
                        }
                        uiState.categories
                            .asSequence()
                            .filterNot { it.name == uiState.favoriteCategoryName }
                            .map { category ->
                                SavedCategoryShortcut(
                                    name = category.name,
                                    count = uiState.categoryCounts[category.name] ?: 0
                                )
                            }
                            .filter { it.count > 0 }
                            .sortedByDescending { it.count }
                            .forEach(::add)
                    }
                }
                val selectedSavedCategory = remember(
                    uiState.selectedCategory,
                    uiState.favoriteCategoryName,
                    uiState.categories
                ) {
                    when (uiState.selectedCategory) {
                        null -> null
                        uiState.favoriteCategoryName -> com.streamvault.domain.model.Category(
                            id = -999L,
                            name = uiState.favoriteCategoryName,
                            type = com.streamvault.domain.model.ContentType.MOVIE,
                            isVirtual = true
                        )
                        else -> uiState.categories.firstOrNull { it.name == uiState.selectedCategory }
                    }
                }

                // Main content
                if (uiState.selectedCategory == null) {
                    val activeLibraryLens = remember(selectedLibraryLens, uiState.libraryLensRows) {
                        MovieLibraryLens.entries.firstOrNull { it.name == selectedLibraryLens && uiState.libraryLensRows.containsKey(it) }
                            ?: uiState.libraryLensRows.keys.firstOrNull()
                    }
                    // Netflix-style rows (All categories view)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(
                            start = LocalSpacing.current.safeHoriz, 
                            end = LocalSpacing.current.safeHoriz, 
                            bottom = LocalSpacing.current.safeBottom
                        )
                    ) {
                        item {
                            Surface(
                                onClick = { onNavigate(Routes.SEARCH) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = LocalSpacing.current.md),
                                shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = SurfaceElevated,
                                    focusedContainerColor = Primary.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔍", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(16.dp))
                                    Text(stringResource(R.string.search_hint), color = OnSurfaceDim, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        if (heroMovie != null) {
                            item {
                                HeroBanner(
                                    movie = heroMovie,
                                    onClick = {
                                        val isLocked = (heroMovie.isAdult || heroMovie.isUserProtected) && uiState.parentalControlLevel == 1
                                        if (isLocked) {
                                            pendingMovie = heroMovie
                                            showPinDialog = true
                                        } else {
                                            onMovieClick(heroMovie)
                                        }
                                    }
                                )
                            }
                        }

                        item(key = "saved_shortcuts") {
                            SavedCategoryShortcutsRow(
                                title = stringResource(R.string.library_saved_title),
                                subtitle = stringResource(R.string.library_saved_subtitle),
                                emptyHint = stringResource(R.string.movies_saved_empty_hint),
                                shortcuts = savedShortcuts,
                                managementHint = stringResource(R.string.library_saved_manage_hint),
                                primaryShortcutLabel = stringResource(R.string.library_browse_all),
                                isPrimaryShortcutSelected = uiState.selectedCategory == null,
                                onPrimaryShortcutClick = { viewModel.selectCategory(null) },
                                selectedShortcutName = uiState.selectedCategory,
                                onShortcutLongClick = viewModel::showCategoryOptions,
                                onShortcutClick = viewModel::selectCategory
                            )
                        }

                        if (activeLibraryLens != null) {
                            item(key = "library_lens_chips") {
                                SelectionChipRow(
                                    title = stringResource(R.string.library_lens_title),
                                    subtitle = stringResource(R.string.movies_library_lens_subtitle),
                                    chips = uiState.libraryLensRows.keys.map { lens ->
                                        SelectionChip(
                                            key = lens.name,
                                            label = movieLibraryLensLabel(lens),
                                            supportingText = stringResource(
                                                R.string.library_saved_items_count,
                                                uiState.libraryLensRows[lens]?.size ?: 0
                                            )
                                        )
                                    },
                                    selectedKey = activeLibraryLens.name,
                                    onChipSelected = { selectedLibraryLens = it },
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                )
                            }

                            item(key = "library_lens_row") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        onClick = viewModel::selectFullLibraryBrowse,
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = Color.White.copy(alpha = 0.06f),
                                            focusedContainerColor = Primary
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                                            Text(
                                                text = stringResource(R.string.library_full_browse_title_movies),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.library_full_browse_subtitle, uiState.libraryCount),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    CategoryRow(
                                        title = movieLibraryLensLabel(activeLibraryLens),
                                        items = uiState.libraryLensRows[activeLibraryLens].orEmpty(),
                                        onSeeAll = if (activeLibraryLens == MovieLibraryLens.FAVORITES) {
                                            { viewModel.selectCategory(uiState.favoriteCategoryName) }
                                        } else {
                                            null
                                        }
                                    ) { movie ->
                                        val isLocked = (movie.isAdult || movie.isUserProtected) && uiState.parentalControlLevel == 1
                                        MovieCard(
                                            movie = movie,
                                            isLocked = isLocked,
                                            onClick = {
                                                if (isLocked) {
                                                    pendingMovie = movie
                                                    showPinDialog = true
                                                } else {
                                                    onMovieClick(movie)
                                                }
                                            },
                                            onLongClick = { viewModel.onShowDialog(movie) }
                                        )
                                    }
                                }
                            }
                        }

                        // Continue Watching row (shown first, only if non-empty)
                        item(key = "continue_watching") {
                            ContinueWatchingRow(
                                items = uiState.continueWatching,
                                onItemClick = { history -> onMovieClick(
                                    com.streamvault.domain.model.Movie(
                                        id = history.contentId,
                                        name = history.title,
                                        posterUrl = history.posterUrl,
                                        streamUrl = history.streamUrl,
                                        providerId = history.providerId
                                    )
                                )}
                            )
                        }
                        items(
                            items = uiState.moviesByCategory.entries.toList(),
                            key = { it.key }
                        ) { (categoryName, movies) ->
                            CategoryRow(
                                title = categoryName,
                                items = movies,
                                onSeeAll = { viewModel.selectCategory(categoryName) }
                            ) { movie ->
                                val isLocked = (movie.isAdult || movie.isUserProtected) && uiState.parentalControlLevel == 1
                                MovieCard(
                                    movie = movie,
                                    isLocked = isLocked,
                                    onClick = {
                                        if (isLocked) {
                                            pendingMovie = movie
                                            showPinDialog = true
                                        } else {
                                            onMovieClick(movie)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.onShowDialog(movie)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Filtered grid for selected category
                    val baseMovies = if (uiState.searchQuery.isBlank()) {
                        uiState.selectedCategoryItems
                    } else {
                        uiState.moviesByCategory[uiState.selectedCategory] ?: emptyList()
                    }
                    val activeMovies = if (uiState.isReorderMode) uiState.filteredMovies else baseMovies
                    val resumeMovieIds = remember(uiState.continueWatching) {
                        uiState.continueWatching.map { it.contentId }.toSet()
                    }
                    val activeFacet = remember(selectedFacet) {
                        MovieLibraryFacet.entries.firstOrNull { it.name == selectedFacet } ?: MovieLibraryFacet.ALL
                    }
                    val activeSort = remember(selectedSort) {
                        MovieLibrarySort.entries.firstOrNull { it.name == selectedSort } ?: MovieLibrarySort.LIBRARY
                    }
                    val filteredGridMovies = remember(activeMovies, activeFacet, activeSort, resumeMovieIds, uiState.isReorderMode) {
                        if (uiState.isReorderMode) {
                            activeMovies
                        } else {
                            applyMovieFacetAndSort(
                                items = activeMovies,
                                facet = activeFacet,
                                sort = activeSort,
                                resumeMovieIds = resumeMovieIds
                            )
                        }
                    }
                    val showSelectedCategoryControls = !uiState.isReorderMode &&
                        (selectedSavedCategory != null || activeMovies.size > 8)
                    
                    var draggingMovie by remember { mutableStateOf<Movie?>(null) }
                    
                    LaunchedEffect(uiState.isReorderMode) {
                        if (!uiState.isReorderMode) {
                            draggingMovie = null
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        if (selectedSavedCategory != null) {
                            SavedCategoryShortcutsRow(
                                title = stringResource(R.string.library_saved_title),
                                subtitle = stringResource(R.string.library_saved_subtitle),
                                emptyHint = stringResource(R.string.movies_saved_empty_hint),
                                shortcuts = savedShortcuts,
                                managementHint = stringResource(R.string.library_saved_manage_hint),
                                primaryShortcutLabel = stringResource(R.string.library_browse_all),
                                isPrimaryShortcutSelected = uiState.selectedCategory == null,
                                onPrimaryShortcutClick = { viewModel.selectCategory(null) },
                                selectedShortcutName = uiState.selectedCategory,
                                onShortcutLongClick = viewModel::showCategoryOptions,
                                onShortcutClick = viewModel::selectCategory
                            )

                            SavedCategoryContextCard(
                                categoryName = selectedSavedCategory.name,
                                itemCount = uiState.categoryCounts[selectedSavedCategory.name] ?: filteredGridMovies.size,
                                onManageClick = { viewModel.showCategoryOptions(selectedSavedCategory.name) },
                                onBrowseAllClick = { viewModel.selectCategory(null) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        if (showSelectedCategoryControls) {
                            SelectionChipRow(
                                title = stringResource(R.string.library_filter_title),
                                chips = buildMovieFacetChips(
                                    items = activeMovies,
                                    resumeMovieIds = resumeMovieIds
                                ),
                                selectedKey = activeFacet.name,
                                onChipSelected = { selectedFacet = it },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(horizontal = 0.dp)
                            )

                            SelectionChipRow(
                                title = stringResource(R.string.library_sort_title),
                                chips = MovieLibrarySort.entries.map { sort ->
                                    SelectionChip(
                                        key = sort.name,
                                        label = when (sort) {
                                            MovieLibrarySort.LIBRARY -> stringResource(R.string.library_sort_library)
                                            MovieLibrarySort.TITLE -> stringResource(R.string.library_sort_az)
                                            MovieLibrarySort.RELEASE -> stringResource(R.string.library_sort_release)
                                            MovieLibrarySort.RATING -> stringResource(R.string.library_sort_rating)
                                        }
                                    )
                                },
                                selectedKey = activeSort.name,
                                onChipSelected = { selectedSort = it },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(horizontal = 0.dp)
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .onPreviewKeyEvent { event ->
                                    if (uiState.isReorderMode && event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                                            if (draggingMovie != null) {
                                                draggingMovie = null
                                                true
                                            } else {
                                                viewModel.exitCategoryReorderMode()
                                                true
                                            }
                                        } else false
                                    } else false
                                },
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                    Text(
                                        text = when (uiState.selectedCategory) {
                                            uiState.fullLibraryCategoryName -> stringResource(R.string.library_full_browse_title_movies)
                                            else -> uiState.selectedCategory ?: ""
                                        },
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Primary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    if (!uiState.isReorderMode && uiState.selectedCategoryTotalCount > 0) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.library_loaded_results,
                                                uiState.selectedCategoryLoadedCount,
                                                uiState.selectedCategoryTotalCount
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceDim
                                        )
                                    }
                                }
                            }

                            if (filteredGridMovies.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = stringResource(R.string.library_filter_empty),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = OnSurfaceDim,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }
                            
                            gridItems(
                                items = filteredGridMovies,
                                key = { it.id }
                            ) { movie ->
                                val isLocked = (movie.isAdult || movie.isUserProtected) && uiState.parentalControlLevel == 1
                                val isDraggingThis = draggingMovie == movie

                                MovieCard(
                                    movie = movie,
                                    isLocked = isLocked,
                                    isReorderMode = uiState.isReorderMode,
                                    isDragging = isDraggingThis,
                                    onClick = {
                                        if (uiState.isReorderMode) {
                                            draggingMovie = if (isDraggingThis) null else movie
                                        } else if (isLocked) {
                                            pendingMovie = movie
                                            showPinDialog = true
                                        } else {
                                            onMovieClick(movie)
                                        }
                                    },
                                    onLongClick = {
                                        if (!uiState.isReorderMode) {
                                            viewModel.onShowDialog(movie)
                                        }
                                    },
                                    modifier = Modifier.onPreviewKeyEvent { event ->
                                        if (uiState.isReorderMode && isDraggingThis && event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            when (event.nativeKeyEvent.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                    viewModel.moveItemUp(movie)
                                                    true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    viewModel.moveItemDown(movie)
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                )
                            }

                            if (!uiState.isReorderMode && uiState.canLoadMoreSelectedCategory) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Surface(
                                        onClick = viewModel::loadMoreSelectedCategory,
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = Color.White.copy(alpha = 0.06f),
                                            focusedContainerColor = Primary
                                        ),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.library_load_more,
                                                uiState.selectedCategoryLoadedCount,
                                                uiState.selectedCategoryTotalCount
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (uiState.showDialog && uiState.selectedMovieForDialog != null) {
        val movie = uiState.selectedMovieForDialog!!
        com.streamvault.app.ui.components.dialogs.AddToGroupDialog(
            contentTitle = movie.name,
            groups = uiState.categories.filter { it.isVirtual && it.id != -999L },
            isFavorite = movie.isFavorite,
            memberOfGroups = uiState.dialogGroupMemberships,
            onDismiss = { viewModel.onDismissDialog() },
            onToggleFavorite = {
                if (movie.isFavorite) viewModel.removeFavorite(movie) else viewModel.addFavorite(movie)
            },
            onAddToGroup = { group -> viewModel.addToGroup(movie, group) },
            onRemoveFromGroup = { group -> viewModel.removeFromGroup(movie, group) },
            onCreateGroup = { name -> viewModel.createCustomGroup(name) }
        )
    }

    if (uiState.selectedCategoryForOptions != null) {
        val category = uiState.selectedCategoryForOptions!!
        com.streamvault.app.ui.components.dialogs.CategoryOptionsDialog(
            category = category,
            onDismissRequest = { viewModel.dismissCategoryOptions() },
            onRename = if (category.isVirtual && category.id != -999L) {
                { viewModel.requestRenameGroup(category) }
            } else null,
            onDelete = if (category.isVirtual && category.id != -999L) {
                {
                    viewModel.requestDeleteGroup(category)
                }
            } else null,
            onReorderChannels = if (category.isVirtual) {
                { viewModel.enterCategoryReorderMode(category) }
            } else null
        )
    }

    if (uiState.showRenameGroupDialog && uiState.groupToRename != null) {
        RenameGroupDialog(
            initialName = uiState.groupToRename!!.name,
            errorMessage = uiState.renameGroupError,
            onDismissRequest = { viewModel.cancelRenameGroup() },
            onConfirm = { name -> viewModel.confirmRenameGroup(name) }
        )
    }

    if (uiState.showDeleteGroupDialog && uiState.groupToDelete != null) {
        DeleteGroupDialog(
            groupName = uiState.groupToDelete!!.name,
            onDismissRequest = { viewModel.cancelDeleteGroup() },
            onConfirmDelete = { viewModel.confirmDeleteGroup() }
        )
    }
}

@Composable
fun HeroBanner(
    movie: Movie,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 32.dp, vertical = 16.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, Color.White),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.posterUrl ?: movie.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 200f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(32.dp)
            ) {
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                if (!movie.plot.isNullOrEmpty()) {
                    Text(
                        text = movie.plot!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                }
                
                // Play Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(if (isFocused) Primary else Color.White, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("▶", color = if (isFocused) Color.White else Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.player_resume).substringBefore(" "), color = if (isFocused) Color.White else Color.Black, style = MaterialTheme.typography.titleMedium) // "Play" fallback
                }
            }
        }
    }
}

private enum class MovieLibraryFacet {
    ALL,
    FAVORITES,
    RESUME,
    UNWATCHED,
    TOP_RATED
}

private enum class MovieLibrarySort {
    LIBRARY,
    TITLE,
    RELEASE,
    RATING
}

@Composable
private fun movieLibraryLensLabel(lens: MovieLibraryLens): String =
    when (lens) {
        MovieLibraryLens.FAVORITES -> stringResource(R.string.library_lens_favorites)
        MovieLibraryLens.CONTINUE -> stringResource(R.string.library_lens_continue)
        MovieLibraryLens.TOP_RATED -> stringResource(R.string.library_lens_top_rated)
        MovieLibraryLens.FRESH -> stringResource(R.string.library_lens_fresh_movies)
    }

private fun buildMovieFacetChips(
    items: List<Movie>,
    resumeMovieIds: Set<Long>
): List<SelectionChip> {
    val favoriteCount = items.count { it.isFavorite }
    val resumeCount = items.count { it.id in resumeMovieIds || it.watchProgress > 0L }
    val unwatchedCount = items.count { it.watchProgress <= 0L && it.id !in resumeMovieIds }
    val topRatedCount = items.count { it.rating > 0f }
    return listOf(
        SelectionChip(MovieLibraryFacet.ALL.name, "All", "${items.size} visible"),
        SelectionChip(MovieLibraryFacet.FAVORITES.name, "Favorites", "$favoriteCount saved"),
        SelectionChip(MovieLibraryFacet.RESUME.name, "Resume", "$resumeCount in progress"),
        SelectionChip(MovieLibraryFacet.UNWATCHED.name, "Unwatched", "$unwatchedCount not started"),
        SelectionChip(MovieLibraryFacet.TOP_RATED.name, "Top Rated", "$topRatedCount rated")
    )
}

private fun applyMovieFacetAndSort(
    items: List<Movie>,
    facet: MovieLibraryFacet,
    sort: MovieLibrarySort,
    resumeMovieIds: Set<Long>
): List<Movie> {
    val filtered = when (facet) {
        MovieLibraryFacet.ALL -> items
        MovieLibraryFacet.FAVORITES -> items.filter { it.isFavorite }
        MovieLibraryFacet.RESUME -> items.filter { it.id in resumeMovieIds || it.watchProgress > 0L }
        MovieLibraryFacet.UNWATCHED -> items.filter { it.watchProgress <= 0L && it.id !in resumeMovieIds }
        MovieLibraryFacet.TOP_RATED -> items.filter { it.rating > 0f }
    }

    return when (sort) {
        MovieLibrarySort.LIBRARY -> filtered
        MovieLibrarySort.TITLE -> filtered.sortedBy { it.name.lowercase() }
        MovieLibrarySort.RELEASE -> filtered.sortedByDescending(::movieReleaseScore)
        MovieLibrarySort.RATING -> filtered.sortedByDescending { it.rating }
    }
}

private fun movieReleaseScore(movie: Movie): Long {
    return movie.releaseDate
        ?.filter { it.isDigit() }
        ?.take(8)
        ?.toLongOrNull()
        ?: movie.year?.toLongOrNull()
        ?: 0L
}
