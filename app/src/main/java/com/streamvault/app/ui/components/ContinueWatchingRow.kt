package com.streamvault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.PlaybackHistory
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

/**
 * Netflix-style "Continue Watching" row backed by PlaybackHistoryRepository.
 *
 * Shows landscape tiles (280×157dp) with:
 *  - Poster/backdrop image
 *  - Bottom gradient overlay with title + episode info
 *  - AccentCyan resume progress bar at bottom edge
 *
 * Hidden automatically when [items] is empty.
 */
@Composable
fun ContinueWatchingRow(
    items: List<PlaybackHistory>,
    onItemClick: (PlaybackHistory) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.continue_watching_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = items, key = { it.id }) { history ->
                ContinueWatchingTile(history = history, onClick = { onItemClick(history) })
            }
        }
    }
}

@Composable
private fun ContinueWatchingTile(
    history: PlaybackHistory,
    onClick: () -> Unit
) {
    val progress = if (history.totalDurationMs > 0) {
        (history.resumePositionMs.toFloat() / history.totalDurationMs).coerceIn(0f, 1f)
    } else 0f

    FocusableCard(
        onClick = onClick,
        width = 280.dp,
        height = 157.dp
    ) { isFocused ->
        // Artwork
        if (!history.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = history.posterUrl,
                contentDescription = history.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (history.contentType) {
                        ContentType.LIVE -> "📡"
                        ContentType.MOVIE -> "🎬"
                        ContentType.SERIES, ContentType.SERIES_EPISODE -> "📺"
                    },
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Bottom gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, GradientOverlayBottom)))
        )

        // Title + subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (progress > 0f) 10.dp else 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = history.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (history.contentType == ContentType.SERIES_EPISODE &&
                history.seasonNumber != null && history.episodeNumber != null
            ) {
                Text(
                    text = stringResource(R.string.continue_watching_season_episode, history.seasonNumber!!, history.episodeNumber!!),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }

        // Resume progress bar at very bottom
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = AccentCyan,
                trackColor = Color.Transparent
            )
        }
    }
}
