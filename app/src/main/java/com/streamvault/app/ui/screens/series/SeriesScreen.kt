package com.streamvault.app.ui.screens.series

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.onFocusChanged
import com.streamvault.app.navigation.Routes
import com.streamvault.app.ui.components.CategoryRow
import com.streamvault.app.ui.components.ContinueWatchingRow
import com.streamvault.app.ui.components.SeriesCard
import com.streamvault.app.ui.components.SkeletonRow
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@Composable
fun SeriesScreen(
    onSeriesClick: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pendingSeriesId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showPinDialog) {
        com.streamvault.app.ui.components.dialogs.PinDialog(
            onDismissRequest = {
                showPinDialog = false
                pinError = null
                pendingSeriesId = null
            },
            onPinEntered = { pin ->
                scope.launch {
                    if (viewModel.verifyPin(pin)) {
                        showPinDialog = false
                        pinError = null
                        pendingSeriesId?.let { onSeriesClick(it) }
                        pendingSeriesId = null
                    } else {
                        pinError = context.getString(R.string.series_incorrect_pin)
                    }
                }
            },
            error = pinError
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopNavBar(currentRoute = currentRoute, onNavigate = onNavigate)

        if (uiState.isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(3) {
                    SkeletonRow(
                        modifier = Modifier.fillMaxWidth(),
                        cardWidth = 140,
                        cardHeight = 210,
                        itemsCount = 10
                    )
                }
            }
        } else if (uiState.seriesByCategory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📺", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.series_no_found), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Surface(
                        onClick = { onNavigate(Routes.SEARCH) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
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

                val heroSeries = uiState.seriesByCategory.values.flatten().firstOrNull()
                if (heroSeries != null) {
                    item {
                        SeriesHeroBanner(
                            series = heroSeries,
                            onClick = {
                                val isLocked = (heroSeries.isAdult || heroSeries.isUserProtected) && uiState.parentalControlLevel == 1
                                if (isLocked) {
                                    pendingSeriesId = heroSeries.id
                                    showPinDialog = true
                                } else {
                                    onSeriesClick(heroSeries.id)
                                }
                            }
                        )
                    }
                }

                // Continue Watching row (shown first, only if non-empty)
                item(key = "continue_watching") {
                    ContinueWatchingRow(
                        items = uiState.continueWatching,
                        onItemClick = { history -> onSeriesClick(history.seriesId ?: history.contentId) }
                    )
                }
                items(
                    items = uiState.seriesByCategory.entries.toList(),
                    key = { it.key }
                ) { (categoryName, seriesList) ->
                    CategoryRow(title = categoryName, items = seriesList) { series ->
                        val isLocked = (series.isAdult || series.isUserProtected) && uiState.parentalControlLevel == 1
                        SeriesCard(
                            series = series,
                            isLocked = isLocked,
                            onClick = {
                                if (isLocked) {
                                    pendingSeriesId = series.id
                                    showPinDialog = true
                                } else {
                                    onSeriesClick(series.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesHeroBanner(
    series: com.streamvault.domain.model.Series,
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
            focusedBorder = androidx.tv.material3.Border(BorderStroke(2.dp, Primary))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = series.posterUrl ?: series.backdropUrl,
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
                    text = series.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                if (!series.plot.isNullOrEmpty()) {
                    Text(
                        text = series.plot!!,
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
