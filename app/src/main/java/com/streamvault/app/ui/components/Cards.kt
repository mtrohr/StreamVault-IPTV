package com.streamvault.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Series

// ── Focusable Card Base ────────────────────────────────────────────

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = 160.dp,
    height: Dp = 240.dp,
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "cardScale"
    )

    val borderColor = if (isFocused) FocusBorder else Color.Transparent

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(width)
            .height(height)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = CardBackground,
            focusedContainerColor = CardBackground
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(2.dp, borderColor),
                shape = RoundedCornerShape(8.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(isFocused)
        }
    }
}

// ── Channel Card ───────────────────────────────────────────────────

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isLocked: Boolean = false
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 200.dp,
        height = 140.dp
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalSpacing.current.sm)
        ) {
            // Top section: Logo and Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!channel.logoUrl.isNullOrBlank() && !isLocked) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Text(
                    text = if (isLocked) "Locked Channel" else channel.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) OnBackground else OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom section: Current Program (Hide if locked)
            if (!isLocked) {
                channel.currentProgram?.let { program ->
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) OnBackground.copy(alpha = 0.8f) else OnSurfaceDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Progress Bar
                    val now = System.currentTimeMillis()
                    val totalDuration = program.endTime - program.startTime
                    val elapsed = now - program.startTime
                    val progress = if (totalDuration > 0) elapsed.toFloat() / totalDuration else 0f
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = if (isFocused) Primary else OnSurfaceDim,
                        trackColor = SurfaceHighlight
                    )
                }
            }
        }
            
        // Lock Overlay
        if (isLocked) {
             Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        // Live indicator (Hide if locked)
        if (!isLocked) {
            Box(
                modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (channel.isFavorite) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                                .background(Color.Yellow, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 14.dp)
                            .background(LiveIndicator, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    }
                }
            }
        }
    }


// ── Movie Card ─────────────────────────────────────────────────────

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isLocked: Boolean = false
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 160.dp,
        height = 240.dp
    ) { isFocused ->
        if (!movie.posterUrl.isNullOrBlank() && !isLocked) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Title overlay at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(LocalSpacing.current.sm)
        ) {
            Text(
                text = if (isLocked) "Locked Movie" else movie.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) OnBackground else OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Rating badge
        if (movie.rating > 0f && !isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Surface(
                    onClick = {},
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Primary.copy(alpha = 0.85f),
                        focusedContainerColor = Primary.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = String.format("%.1f", movie.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Lock Overlay
        if (isLocked) {
             Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }
    }
}

// ── Series Card ────────────────────────────────────────────────────

@Composable
fun SeriesCard(
    series: Series,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isLocked: Boolean = false
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = 160.dp,
        height = 240.dp
    ) { isFocused ->
        if (!series.posterUrl.isNullOrBlank() && !isLocked) {
            AsyncImage(
                model = series.posterUrl,
                contentDescription = series.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(LocalSpacing.current.sm)
        ) {
            Text(
                text = if (isLocked) "Locked Series" else series.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) OnBackground else OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Lock Overlay
        if (isLocked) {
             Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }
    }
}
