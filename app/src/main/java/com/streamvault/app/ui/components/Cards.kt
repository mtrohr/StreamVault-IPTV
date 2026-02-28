package com.streamvault.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Series
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

// ── Focusable Card Base ────────────────────────────────────────────

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = 160.dp,
    height: Dp = 240.dp,
    isReorderMode: Boolean = false,
    isDragging: Boolean = false,
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    // In reorder mode, only the dragging item scales. Otherwise grid is static.
    val scale by animateFloatAsState(
        targetValue = if (isFocused) {
            if (isReorderMode && !isDragging) 1f else 1.05f // PREMIUM: 1.05x instead of 1.08x
        } else {
            if (isDragging) 1.05f else 1f
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing), // PREMIUM: 200ms
        label = "scale"
    )

    val shadowElevation by animateFloatAsState(
        targetValue = if (isDragging) 16f else if (isFocused) 8f else 0f, // PREMIUM: Down from 24dp to 8dp
        animationSpec = tween(durationMillis = 200),
        label = "elevation"
    )

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.shadowElevation = shadowElevation
                shape = RoundedCornerShape(12.dp)
                clip = false
            }
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)), // PREMIUM: 12dp
        colors = ClickableSurfaceDefaults.colors(
            containerColor = CardBackground,
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(0.dp, Color.Transparent),
                shape = RoundedCornerShape(12.dp) // PREMIUM: 12dp
            ),
            focusedBorder = Border(
                border = BorderStroke(
                    width = if (isDragging) 4.dp else 3.dp, // PREMIUM: 3dp border
                    color = if (isDragging) AccentAmber else FocusBorder // PREMIUM: FocusBorder (White)
                ),
                shape = RoundedCornerShape(12.dp) // PREMIUM: 12dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(isFocused)
        }
    }
}

// ── Channel Card (16:9 landscape tile) ────────────────────────────

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isLocked: Boolean = false,
    isReorderMode: Boolean = false,
    isDragging: Boolean = false
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 280.dp,
        height = 157.dp,
        isReorderMode = isReorderMode,
        isDragging = isDragging
    ) { isFocused ->
        // Background: logo or solid surface
        if (!channel.logoUrl.isNullOrBlank() && !isLocked) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, GradientOverlayBottom)
                    )
                )
        )

        // Bottom content: channel name + EPG + progress
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isLocked) stringResource(R.string.card_locked_channel) else channel.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (isFocused) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isLocked) {
                channel.currentProgram?.let { program ->
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val now = System.currentTimeMillis()
                    val totalDuration = program.endTime - program.startTime
                    val elapsed = now - program.startTime
                    val progress = if (totalDuration > 0) elapsed.toFloat() / totalDuration else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color = AccentCyan,
                        trackColor = SurfaceHighlight
                    )
                }
            }
        }

        // Top-right badges
        if (!isLocked) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (channel.isFavorite) {
                    Box(
                        modifier = Modifier
                            .background(AccentAmber, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("★", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                    }
                }
                if (channel.catchUpSupported) {
                    Box(
                        modifier = Modifier
                            .background(Primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("📼", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .background(AccentRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(stringResource(R.string.card_live_badge), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        // Lock overlay
        if (isLocked) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🔒", style = MaterialTheme.typography.displayMedium)
            }
        }
    }
}

// ── Movie Card (16:9 landscape thumbnail) ─────────────────────────────────────────

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isLocked: Boolean = false,
    watchProgress: Float = 0f   // 0..1 from playback history
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 240.dp,
        height = 135.dp
    ) { isFocused ->
        // Poster image
        if (!movie.posterUrl.isNullOrBlank() && !isLocked) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.name,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLocked) "🔒" else "🎬",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, GradientOverlayBottom),
                    )
                )
        )

        // Title + metadata at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isLocked) stringResource(R.string.card_locked) else movie.name,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!isLocked && movie.year != null) {
                val year = movie.year
                val genre = movie.genre
                Text(
                    text = buildString {
                        append(year)
                        if (!genre.isNullOrBlank()) append(" · ${genre.take(20)}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Watch progress bar at very bottom edge
        if (watchProgress > 0f && !isLocked) {
            LinearProgressIndicator(
                progress = { watchProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = AccentCyan,
                trackColor = Color.Transparent
            )
        }

        // Rating badge top-right
        if (movie.rating > 0f && !isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(PrimaryGlow, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format("%.1f", movie.rating),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

// ── Series Card (16:9 landscape thumbnail) ────────────────────────────────────────

@Composable
fun SeriesCard(
    series: Series,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isLocked: Boolean = false,
    watchProgress: Float = 0f,
    subtitle: String? = null   // e.g. "12 episodes" — caller provides to avoid type collision
) {
    val posterUrl = series.posterUrl
    val seriesName = series.name

    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 240.dp,
        height = 135.dp
    ) { isFocused ->
        // Poster image
        if (!posterUrl.isNullOrBlank() && !isLocked) {
            AsyncImage(
                model = posterUrl,
                contentDescription = seriesName,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLocked) "🔒" else "📺",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, GradientOverlayBottom)
                    )
                )
        )

        // Title at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isLocked) stringResource(R.string.card_locked) else seriesName,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!isLocked && !subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }

        // Watch progress bar at very bottom edge
        if (watchProgress > 0f && !isLocked) {
            LinearProgressIndicator(
                progress = { watchProgress.coerceIn(0f, 1f) },
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

