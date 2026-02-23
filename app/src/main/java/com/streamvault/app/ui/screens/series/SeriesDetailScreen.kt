package com.streamvault.app.ui.screens.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Episode
import com.streamvault.domain.model.Season
import com.streamvault.domain.model.Series
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@Composable
fun SeriesDetailScreen(
    onEpisodeClick: (Episode) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val series = uiState.series
    
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.series_loading_details), color = OnSurface)
        }
        return
    }

    if (series == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.series_not_found), color = ErrorColor)
        }
        return
    }

    SeriesDetailContent(
        series = series,
        selectedSeason = uiState.selectedSeason,
        onSeasonSelected = viewModel::selectSeason,
        onEpisodeClick = onEpisodeClick
    )
}

@Composable
private fun SeriesDetailContent(
    series: Series,
    selectedSeason: Season?,
    onSeasonSelected: (Season) -> Unit,
    onEpisodeClick: (Episode) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        // Backdrop
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(series.backdropUrl ?: series.posterUrl)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Background
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header Info
            item {
                Column(modifier = Modifier.padding(top = 200.dp, bottom = 24.dp)) {
                    Text(
                        text = series.name,
                        style = MaterialTheme.typography.displayMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (series.rating > 0) {
                            Text("⭐ ${series.rating}", color = Secondary, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        series.releaseDate?.let { date ->
                            Text(date, color = OnSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        series.genre?.let { genre ->
                             Text(genre, color = OnSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = series.plot ?: "No plot available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface.copy(alpha = 0.8f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Seasons
            if (series.seasons.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.series_seasons), style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        items(series.seasons) { season ->
                            SeasonChip(
                                season = season,
                                isSelected = season == selectedSeason,
                                onClick = { onSeasonSelected(season) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Episodes
            selectedSeason?.let { season ->
                item {
                    Text(stringResource(R.string.series_episodes, season.episodes.size), style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(season.episodes) { episode ->
                   EpisodeItem(episode = episode, onClick = { onEpisodeClick(episode) })
                   Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SeasonChip(
    season: Season,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary else Surface,
            contentColor = if (isSelected) OnPrimary else OnSurface,
            focusedContainerColor = Primary,
            focusedContentColor = OnPrimary
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.5f))
            )
        )
    ) {
        Text(
            text = season.name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Surface,
            focusedContainerColor = SurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Number
            Text(
                text = "${episode.episodeNumber}",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            
            // Thumbnail (Optional)
            episode.coverUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(16f/9f)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                episode.plot?.let { plot ->
                    if (plot.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plot,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Text(
                text = "▶", 
                color = Primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
