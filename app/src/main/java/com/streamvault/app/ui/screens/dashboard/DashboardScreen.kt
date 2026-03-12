package com.streamvault.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.streamvault.app.R
import com.streamvault.app.navigation.Routes
import com.streamvault.app.ui.components.CategoryRow
import com.streamvault.app.ui.components.ChannelCard
import com.streamvault.app.ui.components.ContinueWatchingRow
import com.streamvault.app.ui.components.MovieCard
import com.streamvault.app.ui.components.SeriesCard
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.FocusBorder
import com.streamvault.app.ui.theme.OnBackground
import com.streamvault.app.ui.theme.OnSurfaceDim
import com.streamvault.app.ui.theme.Primary
import com.streamvault.app.ui.theme.SurfaceElevated
import com.streamvault.app.ui.theme.SurfaceHighlight
import com.streamvault.app.ui.theme.TextPrimary
import com.streamvault.app.ui.theme.TextTertiary
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.model.Series
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onAddProvider: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
    onPlaybackHistoryClick: (PlaybackHistory) -> Unit,
    currentRoute: String,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopNavBar(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxWidth()
        )

        val provider = uiState.provider
        if (provider == null) {
            EmptyDashboard(
                onAddProvider = onAddProvider,
                onOpenSettings = { onNavigate(Routes.SETTINGS) }
            )
            return@Column
        }
        val orderedSections = rememberDashboardSections(uiState)

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                DashboardHero(
                    providerName = provider.name,
                    feature = uiState.feature,
                    stats = uiState.stats,
                    onOpenLiveTv = { onNavigate(Routes.LIVE_TV) },
                    onOpenGuide = { onNavigate(Routes.EPG) },
                    onOpenSearch = { onNavigate(Routes.SEARCH) },
                    onOpenSavedLibrary = { onNavigate(Routes.FAVORITES) },
                    onFeatureAction = {
                        when (uiState.feature.actionType) {
                            DashboardFeatureAction.LIVE -> onNavigate(Routes.LIVE_TV)
                            DashboardFeatureAction.CONTINUE_WATCHING -> uiState.continueWatching.firstOrNull()?.let(onPlaybackHistoryClick)
                            DashboardFeatureAction.MOVIES -> onNavigate(Routes.MOVIES)
                            DashboardFeatureAction.SERIES -> onNavigate(Routes.SERIES)
                        }
                    }
                )
            }

            item {
                DashboardProviderHealthCard(
                    providerName = provider.name,
                    health = uiState.providerHealth,
                    onOpenDiagnostics = { onNavigate(Routes.SETTINGS) }
                )
            }

            if (uiState.providerWarnings.isNotEmpty()) {
                item {
                    DashboardProviderWarningCard(
                        warnings = uiState.providerWarnings,
                        onOpenSettings = { onNavigate(Routes.SETTINGS) }
                    )
                }
            }

            items(orderedSections, key = { it.name }) { section ->
                when (section) {
                    DashboardHomeSection.LIVE_SHORTCUTS -> DashboardShortcutRow(
                        title = stringResource(R.string.dashboard_live_shortcuts),
                        subtitle = stringResource(R.string.dashboard_live_shortcuts_subtitle),
                        shortcuts = uiState.liveShortcuts,
                        onShortcutClick = { shortcut ->
                            shortcut.categoryId?.let { categoryId ->
                                onNavigate(Routes.liveTv(categoryId))
                            } ?: onNavigate(Routes.LIVE_TV)
                        }
                    )

                    DashboardHomeSection.FAVORITE_CHANNELS -> CategoryRow(
                        title = stringResource(R.string.dashboard_favorite_channels),
                        items = uiState.favoriteChannels,
                        onSeeAll = { onNavigate(Routes.liveTv(com.streamvault.domain.model.VirtualCategoryIds.FAVORITES)) }
                    ) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) }
                        )
                    }

                    DashboardHomeSection.RECENT_CHANNELS -> CategoryRow(
                        title = stringResource(R.string.dashboard_recent_channels),
                        items = uiState.recentChannels,
                        onSeeAll = { onNavigate(Routes.liveTv(com.streamvault.domain.model.VirtualCategoryIds.RECENT)) }
                    ) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) }
                        )
                    }

                    DashboardHomeSection.CONTINUE_WATCHING -> ContinueWatchingRow(
                        items = uiState.continueWatching,
                        onItemClick = onPlaybackHistoryClick
                    )

                    DashboardHomeSection.RECENT_MOVIES -> CategoryRow(
                        title = stringResource(R.string.dashboard_recent_movies),
                        items = uiState.recentMovies,
                        onSeeAll = { onNavigate(Routes.MOVIES) }
                    ) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) }
                        )
                    }

                    DashboardHomeSection.RECENT_SERIES -> CategoryRow(
                        title = stringResource(R.string.dashboard_recent_series),
                        items = uiState.recentSeries,
                        onSeeAll = { onNavigate(Routes.SERIES) }
                    ) { series ->
                        SeriesCard(
                            series = series,
                            subtitle = series.releaseDate ?: stringResource(R.string.dashboard_updated_series_badge),
                            onClick = { onSeriesClick(series) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHero(
    providerName: String,
    feature: DashboardFeature,
    stats: DashboardStats,
    onOpenLiveTv: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSavedLibrary: () -> Unit,
    onFeatureAction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = SurfaceDefaults.colors(
            containerColor = SurfaceElevated
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF07111F),
                            Color(0xFF0B2240),
                            Color(0xFF111827)
                        )
                    )
                )
        ) {
            if (!feature.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = feature.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.84f),
                                    Color.Black.copy(alpha = 0.68f),
                                    Color.Black.copy(alpha = 0.28f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle, providerName),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceDim
                        )
                    }

                    DashboardStatRow(stats = stats)
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = feature.summary,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardActionButton(
                            label = stringResource(R.string.nav_live_tv),
                            onClick = onOpenLiveTv
                        )
                        DashboardActionButton(
                            label = stringResource(R.string.nav_epg),
                            onClick = onOpenGuide
                        )
                        DashboardActionButton(
                            label = stringResource(R.string.dashboard_search_library),
                            onClick = onOpenSearch
                        )
                        DashboardActionButton(
                            label = stringResource(R.string.favorites_title),
                            onClick = onOpenSavedLibrary
                        )
                        if (feature.actionLabel.isNotBlank()) {
                            DashboardActionButton(
                                label = feature.actionLabel,
                                onClick = onFeatureAction
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatRow(
    stats: DashboardStats
) {
    val statItems = listOf(
        stringResource(R.string.dashboard_stat_live, stats.liveChannelCount),
        stringResource(R.string.dashboard_stat_favorites, stats.favoriteChannelCount),
        stringResource(R.string.dashboard_stat_recent, stats.recentChannelCount),
        stringResource(R.string.dashboard_stat_resume, stats.continueWatchingCount),
        stringResource(R.string.dashboard_stat_movies, stats.movieLibraryCount),
        stringResource(R.string.dashboard_stat_series, stats.seriesLibraryCount)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(statItems) { statLabel ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
            ) {
                Text(
                    text = statLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardShortcutRow(
    title: String,
    subtitle: String,
    shortcuts: List<DashboardLiveShortcut>,
    onShortcutClick: (DashboardLiveShortcut) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(shortcuts, key = { "${it.type}:${it.categoryId}:${it.label}" }) { shortcut ->
                DashboardShortcutCard(
                    shortcut = shortcut,
                    onClick = { onShortcutClick(shortcut) }
                )
            }
        }
    }
}

@Composable
private fun DashboardShortcutCard(
    shortcut: DashboardLiveShortcut,
    onClick: () -> Unit
) {
    val accentColor = when (shortcut.type) {
        DashboardShortcutType.FAVORITES -> Color(0xFFFFC857)
        DashboardShortcutType.RECENT -> Color(0xFF4FD1C5)
        DashboardShortcutType.LAST_GROUP -> Color(0xFF60A5FA)
        DashboardShortcutType.CUSTOM_GROUP -> Primary
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(108.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(18.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(18.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                )
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = shortcut.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardActionButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Primary.copy(alpha = 0.18f),
            focusedContainerColor = Primary.copy(alpha = 0.32f),
            contentColor = TextPrimary
        )
    ) {
        Text(text = label)
    }
}

@Composable
private fun DashboardProviderHealthCard(
    providerName: String,
    health: DashboardProviderHealth,
    onOpenDiagnostics: () -> Unit
) {
    val syncLabel = remember(health.lastSyncedAt) {
        if (health.lastSyncedAt <= 0L) {
            "No recent sync"
        } else {
            val format = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            "Synced ${format.format(Date(health.lastSyncedAt))}"
        }
    }
    val expiryLabel = remember(health.expirationDate) {
        health.expirationDate?.let {
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "Expires ${format.format(Date(it))}"
        } ?: "No expiry reported"
    }
    val statusLabel = when (health.status) {
        com.streamvault.domain.model.ProviderStatus.ACTIVE -> stringResource(R.string.settings_status_active)
        com.streamvault.domain.model.ProviderStatus.PARTIAL -> stringResource(R.string.settings_status_partial)
        com.streamvault.domain.model.ProviderStatus.ERROR -> stringResource(R.string.settings_status_error)
        com.streamvault.domain.model.ProviderStatus.EXPIRED -> stringResource(R.string.settings_status_expired)
        com.streamvault.domain.model.ProviderStatus.DISABLED -> stringResource(R.string.settings_status_disabled)
        com.streamvault.domain.model.ProviderStatus.UNKNOWN -> stringResource(R.string.settings_status_unknown)
    }
    val sourceLabel = when (health.type) {
        com.streamvault.domain.model.ProviderType.XTREAM_CODES -> stringResource(R.string.dashboard_provider_xtream)
        com.streamvault.domain.model.ProviderType.M3U -> stringResource(R.string.dashboard_provider_m3u)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_provider_health_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceDim
                )
                Text(
                    text = "$syncLabel • $expiryLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    DashboardHealthPill(
                        label = statusLabel,
                        value = stringResource(R.string.dashboard_provider_status)
                    )
                }
                item {
                    DashboardHealthPill(
                        label = sourceLabel,
                        value = stringResource(R.string.dashboard_provider_source)
                    )
                }
                item {
                    DashboardHealthPill(
                        label = health.maxConnections.toString(),
                        value = stringResource(R.string.dashboard_provider_connections)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.End
        ) {
            DashboardActionButton(
                label = stringResource(R.string.dashboard_warning_review),
                onClick = onOpenDiagnostics
            )
        }
    }
}

@Composable
private fun DashboardHealthPill(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceDim
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun DashboardProviderWarningCard(
    warnings: List<String>,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
            Text(
                text = warnings.take(3).joinToString(" | "),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardActionButton(
                    label = stringResource(R.string.dashboard_warning_review),
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboard(
    onAddProvider: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(
                containerColor = SurfaceHighlight
            )
        ) {
            Column(
                modifier = Modifier
                    .width(720.dp)
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.dashboard_empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnBackground
                )
                Text(
                    text = stringResource(R.string.dashboard_empty_body),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceDim
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onAddProvider) {
                        Text(stringResource(R.string.settings_add_provider))
                    }
                    Button(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceElevated,
                            focusedContainerColor = Primary.copy(alpha = 0.24f),
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(stringResource(R.string.nav_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDashboardSections(
    uiState: DashboardUiState
): List<DashboardHomeSection> {
    return remember(
        uiState.feature.actionType,
        uiState.liveShortcuts,
        uiState.favoriteChannels,
        uiState.recentChannels,
        uiState.continueWatching,
        uiState.recentMovies,
        uiState.recentSeries
    ) {
        val preferred = when (uiState.feature.actionType) {
            DashboardFeatureAction.CONTINUE_WATCHING -> listOf(
                DashboardHomeSection.CONTINUE_WATCHING,
                DashboardHomeSection.RECENT_CHANNELS,
                DashboardHomeSection.FAVORITE_CHANNELS,
                DashboardHomeSection.RECENT_MOVIES,
                DashboardHomeSection.RECENT_SERIES,
                DashboardHomeSection.LIVE_SHORTCUTS
            )
            DashboardFeatureAction.MOVIES -> listOf(
                DashboardHomeSection.RECENT_MOVIES,
                DashboardHomeSection.CONTINUE_WATCHING,
                DashboardHomeSection.RECENT_SERIES,
                DashboardHomeSection.RECENT_CHANNELS,
                DashboardHomeSection.FAVORITE_CHANNELS,
                DashboardHomeSection.LIVE_SHORTCUTS
            )
            DashboardFeatureAction.SERIES -> listOf(
                DashboardHomeSection.RECENT_SERIES,
                DashboardHomeSection.CONTINUE_WATCHING,
                DashboardHomeSection.RECENT_MOVIES,
                DashboardHomeSection.RECENT_CHANNELS,
                DashboardHomeSection.FAVORITE_CHANNELS,
                DashboardHomeSection.LIVE_SHORTCUTS
            )
            DashboardFeatureAction.LIVE -> listOf(
                DashboardHomeSection.RECENT_CHANNELS,
                DashboardHomeSection.FAVORITE_CHANNELS,
                DashboardHomeSection.LIVE_SHORTCUTS,
                DashboardHomeSection.CONTINUE_WATCHING,
                DashboardHomeSection.RECENT_MOVIES,
                DashboardHomeSection.RECENT_SERIES
            )
        }

        preferred.filter { section ->
            when (section) {
                DashboardHomeSection.LIVE_SHORTCUTS -> uiState.liveShortcuts.isNotEmpty()
                DashboardHomeSection.FAVORITE_CHANNELS -> uiState.favoriteChannels.isNotEmpty()
                DashboardHomeSection.RECENT_CHANNELS -> uiState.recentChannels.isNotEmpty()
                DashboardHomeSection.CONTINUE_WATCHING -> uiState.continueWatching.isNotEmpty()
                DashboardHomeSection.RECENT_MOVIES -> uiState.recentMovies.isNotEmpty()
                DashboardHomeSection.RECENT_SERIES -> uiState.recentSeries.isNotEmpty()
            }
        }
    }
}

private enum class DashboardHomeSection {
    LIVE_SHORTCUTS,
    FAVORITE_CHANNELS,
    RECENT_CHANNELS,
    CONTINUE_WATCHING,
    RECENT_MOVIES,
    RECENT_SERIES
}
