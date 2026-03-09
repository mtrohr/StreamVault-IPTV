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
            if (isReorderMode && !isDragging) 1f else 1.1f // PREMIUM: 1.1x for better visibility
        } else {
            if (isDragging) 1.1f else 1f
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
                clip = false // PREMIUM: Allow focus border and shadow to show outside
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
                if (channel.errorCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(AccentRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("⚠️", style = MaterialTheme.typography.labelSmall, color = Color.White)
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
    watchProgress: Float = 0f,
    isReorderMode: Boolean = false,
    isDragging: Boolean = false
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 160.dp, // PREMIUM: Vertical aspect ratio
        height = 240.dp,
        isReorderMode = isReorderMode,
        isDragging = isDragging
    ) { isFocused ->
        // Poster image
        if (!movie.posterUrl.isNullOrBlank() && !isLocked) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)) // PREMIUM: Clip poster to card shape
                    .background(SurfaceElevated), // Background for non-standard posters
                contentScale = ContentScale.Fit // PREMIUM: Fits entire poster inside card
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLocked) "🔒" else "🎬",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        // Bottom gradient overlay - Higher for vertical text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    )
                )
        )

        // Title + metadata at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp), // Increased padding
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isLocked) stringResource(R.string.card_locked) else movie.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!isLocked && movie.year != null) {
                Text(
                    text = movie.year!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Watch progress bar
        if (watchProgress > 0f && !isLocked) {
            LinearProgressIndicator(
                progress = { watchProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = Primary,
                trackColor = Color.Transparent
            )
        }

        // Badges
        if (!isLocked) {
            // Rating badge (Bottom Right above title area or overlap)
            if (movie.rating > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⭐ ${String.format("%.1f", movie.rating)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentAmber
                    )
                }
            }

            // Favorite badge
            if (movie.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Text("❤", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
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
    subtitle: String? = null,
    isReorderMode: Boolean = false,
    isDragging: Boolean = false
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 160.dp, // PREMIUM: Vertical aspect ratio
        height = 240.dp,
        isReorderMode = isReorderMode,
        isDragging = isDragging
    ) { isFocused ->
        // Poster image
        if (!series.posterUrl.isNullOrBlank() && !isLocked) {
            AsyncImage(
                model = series.posterUrl,
                contentDescription = series.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)) // PREMIUM: Clip poster to card shape
                    .background(SurfaceElevated),
                contentScale = ContentScale.Fit // PREMIUM: Fits entire poster inside card
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLocked) "🔒" else "📺",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    )
                )
        )

        // Title at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isLocked) stringResource(R.string.card_locked) else series.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!isLocked && !subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }

        // Watch progress bar
        if (watchProgress > 0f && !isLocked) {
            LinearProgressIndicator(
                progress = { watchProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = Primary,
                trackColor = Color.Transparent
            )
        }

        // Badges
        if (!isLocked) {
            // Rating badge
            if (series.rating > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⭐ ${String.format("%.1f", series.rating)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentAmber
                    )
                }
            }

            // Favorite badge
            if (series.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Text("❤", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

