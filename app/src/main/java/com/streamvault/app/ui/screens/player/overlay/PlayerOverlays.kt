package com.streamvault.app.ui.screens.player.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.streamvault.app.R
import com.streamvault.app.ui.components.shell.StatusPill
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.screens.player.PlayerDiagnosticsUiState
import com.streamvault.app.ui.theme.OnSurfaceDim
import com.streamvault.app.ui.theme.Primary
import com.streamvault.app.ui.theme.SurfaceVariant
import com.streamvault.app.ui.theme.TextSecondary
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import com.streamvault.player.PlayerStats
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun PlayerOverlayPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = AppColors.SurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.BrandMuted.copy(alpha = 0.15f),
                            AppColors.SurfaceElevated,
                            AppColors.Surface
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun PlayerMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChannelInfoOverlay(
    currentChannel: Channel?,
    displayChannelNumber: Int,
    currentProgram: Program?,
    nextProgram: Program?,
    focusRequester: FocusRequester,
    lastVisitedCategoryName: String?,
    onDismiss: () -> Unit,
    onOpenFullEpg: () -> Unit,
    onOpenLastGroup: () -> Unit,
    onOpenRecentChannels: () -> Unit,
    onOpenFullControls: () -> Unit,
    onRestartProgram: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onToggleDiagnostics: () -> Unit,
    onTogglePlayPause: () -> Unit,
    isPlaying: Boolean,
    currentAspectRatio: String,
    isDiagnosticsEnabled: Boolean,
    onOpenSplitScreen: () -> Unit = {}
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    BackHandler { onDismiss() }

    PlayerOverlayPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentChannel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        label = stringResource(R.string.player_live_channel, displayChannelNumber),
                        containerColor = AppColors.BrandMuted
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = currentChannel.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentChannel.catchUpSupported) {
                        Spacer(Modifier.width(12.dp))
                        StatusPill(
                            label = stringResource(R.string.player_catchup_badge),
                            containerColor = AppColors.Live
                        )
                    }
                }
            }

            if (currentProgram != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProgram.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(
                                    R.string.player_time_range_minutes,
                                    timeFormat.format(Date(currentProgram.startTime)),
                                    timeFormat.format(Date(currentProgram.endTime)),
                                    currentProgram.durationMinutes
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                    val now = System.currentTimeMillis()
                    val start = currentProgram.startTime
                    val end = currentProgram.endTime
                    if (start in 1..<end) {
                        val progress = (now - start).toFloat() / (end - start)
                        val remainingMin = ((end - now) / 60000).toInt().coerceAtLeast(0)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = Primary,
                            trackColor = AppColors.SurfaceEmphasis
                        )
                        Text(
                            text = stringResource(R.string.player_minutes_remaining, remainingMin),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    if (nextProgram != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.player_next_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary
                            )
                            Text(
                                text = nextProgram.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = timeFormat.format(Date(nextProgram.startTime)),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextTertiary
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickActionButton(
                        icon = stringResource(R.string.player_action_playback),
                        label = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                        onClick = onTogglePlayPause,
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Primary.copy(alpha = 0.25f),
                            focusedContainerColor = Primary,
                            pressedContainerColor = Primary.copy(alpha = 0.8f)
                        )
                    )
                    if (currentChannel?.catchUpSupported == true && currentProgram?.hasArchive == true) {
                        QuickActionButton(
                            icon = stringResource(R.string.player_action_restart_short),
                            label = stringResource(R.string.player_restart),
                            onClick = {
                                onRestartProgram()
                                onDismiss()
                            }
                        )
                    }
                    QuickActionButton(
                        icon = stringResource(R.string.player_action_view),
                        label = currentAspectRatio,
                        onClick = onToggleAspectRatio,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    if (!lastVisitedCategoryName.isNullOrBlank()) {
                        QuickActionButton(
                            icon = stringResource(R.string.player_action_group),
                            label = stringResource(R.string.player_last_group_label, lastVisitedCategoryName),
                            onClick = onOpenLastGroup
                        )
                    }
                    QuickActionButton(
                        icon = stringResource(R.string.player_action_recent_short),
                        label = stringResource(R.string.player_recent_channels),
                        onClick = onOpenRecentChannels
                    )
                    QuickActionButton(
                        icon = stringResource(R.string.player_action_guide),
                        label = stringResource(R.string.player_full_epg),
                        onClick = onOpenFullEpg
                    )
                    QuickActionButton(
                        icon = stringResource(R.string.player_action_split),
                        label = stringResource(R.string.multiview_nav),
                        onClick = {
                            onDismiss()
                            onOpenSplitScreen()
                        }
                    )
                    QuickActionButton(
                        icon = stringResource(R.string.player_action_diagnostics),
                        label = stringResource(R.string.player_stats),
                        onClick = onToggleDiagnostics
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    colors: androidx.tv.material3.ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = AppColors.SurfaceEmphasis,
        focusedContainerColor = Primary.copy(alpha = 0.85f)
    ),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = colors,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "iconToggle"
            ) { targetIcon ->
                Text(
                    text = targetIcon.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.BrandStrong,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChannelListOverlay(
    channels: List<Channel>,
    recentChannels: List<Channel>,
    currentChannelId: Long,
    overlayFocusRequester: FocusRequester = remember { FocusRequester() },
    preferredFocusedChannelId: Long? = null,
    onFocusedChannelChange: (Long) -> Unit = {},
    lastVisitedCategoryName: String? = null,
    onOpenLastGroup: () -> Unit = {},
    onSelectChannel: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentIndex = remember(channels, currentChannelId) {
        channels.indexOfFirst { it.id == currentChannelId }.coerceAtLeast(0)
    }
    val preferredIndex = remember(channels, currentChannelId, preferredFocusedChannelId) {
        val focused = preferredFocusedChannelId?.let { channelId ->
            channels.indexOfFirst { it.id == channelId }.takeIf { it >= 0 }
        }
        focused ?: currentIndex
    }

    LaunchedEffect(channels, preferredIndex) {
        if (channels.isNotEmpty()) {
            listState.scrollToItem(preferredIndex)
        }
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
    ) {
        PlayerOverlayPanel(
            modifier = Modifier
                .width(520.dp)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.player_channel_list_title, channels.size),
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary
                        )
                        if (!lastVisitedCategoryName.isNullOrBlank()) {
                            Surface(
                                onClick = onOpenLastGroup,
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = AppColors.SurfaceEmphasis,
                                    focusedContainerColor = Primary
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.player_last_group_label, lastVisitedCategoryName),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
                if (!lastVisitedCategoryName.isNullOrBlank()) {
                    item {
                        Text(
                            text = stringResource(R.string.player_last_group_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.player_channel_list_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (recentChannels.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.player_recent_channels),
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSurfaceDim,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                itemsIndexed(recentChannels, key = { _, channel -> channel.id }) { index, channel ->
                                    Surface(
                                        onClick = {
                                            onSelectChannel(channel.id)
                                            onDismiss()
                                        },
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = AppColors.SurfaceEmphasis,
                                            focusedContainerColor = Primary
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val recentNumber = (index + 1).toString().padStart(2, '0')
                                            Text(
                                                text = recentNumber,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                            Text(
                                                text = channel.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                items(channels.size) { index ->
                    val channel = channels[index]
                    val isSelected = channel.id == currentChannelId
                    val shouldRequestFocus = preferredFocusedChannelId?.let { it == channel.id } ?: isSelected
                    val channelNumber = index + 1

                    Surface(
                        onClick = {
                            onSelectChannel(channel.id)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    onFocusedChannelChange(channel.id)
                                    scope.launch {
                                        val targetIndex = (index - 1).coerceAtLeast(0)
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            }
                            .then(
                                if (shouldRequestFocus) Modifier.focusRequester(overlayFocusRequester)
                                else Modifier
                            ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) Primary.copy(alpha = 0.25f) else AppColors.Surface.copy(alpha = 0.82f),
                            focusedContainerColor = Primary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isSelected) {
                                StatusPill(
                                    label = stringResource(R.string.player_channel_selected),
                                    containerColor = AppColors.BrandMuted
                                )
                            } else {
                                Spacer(Modifier.width(42.dp))
                            }
                            Text(
                                text = channelNumber.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (channel.catchUpSupported) {
                                StatusPill(
                                    label = stringResource(R.string.player_archive_badge),
                                    containerColor = AppColors.Warning,
                                    contentColor = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpgOverlay(
    currentChannel: Channel?,
    displayChannelNumber: Int,
    currentProgram: Program?,
    nextProgram: Program?,
    upcomingPrograms: List<Program>,
    overlayFocusRequester: FocusRequester = remember { FocusRequester() },
    preferredFocusedProgramToken: Long? = null,
    onFocusedProgramChange: (Long) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayCatchUp: (Program) -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val listState = rememberLazyListState()
    val filteredUpcoming = remember(upcomingPrograms, currentProgram, nextProgram) {
        upcomingPrograms.filter { it.id != currentProgram?.id && it.id != nextProgram?.id }
    }
    val displayPrograms = remember(filteredUpcoming, nextProgram) {
        if (nextProgram != null) listOf(nextProgram) + filteredUpcoming else filteredUpcoming
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
    ) {
        PlayerOverlayPanel(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(560.dp)
                .padding(24.dp)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.epg_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentChannel != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$displayChannelNumber. ${currentChannel.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentChannel.catchUpSupported) {
                            Spacer(Modifier.height(8.dp))
                            StatusPill(
                                label = stringResource(R.string.epg_catchup_available, currentChannel.catchUpDays),
                                containerColor = AppColors.BrandMuted
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.player_epg_overlay_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )
                }

                item {
                    androidx.compose.material3.HorizontalDivider(color = SurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.epg_now_playing),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    if (currentProgram != null) {
                        Text(
                            currentProgram.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${timeFormat.format(Date(currentProgram.startTime))} - ${timeFormat.format(Date(currentProgram.endTime))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${currentProgram.durationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceDim
                            )
                            if (currentProgram.lang.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = currentProgram.lang.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val now = System.currentTimeMillis()
                        val start = currentProgram.startTime
                        val end = currentProgram.endTime
                        if (start in 1..<end) {
                            val progress = (now - start).toFloat() / (end - start)
                            val remainingMin = ((end - now) / 60000).toInt().coerceAtLeast(0)
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Primary,
                                trackColor = AppColors.SurfaceEmphasis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.player_minutes_remaining, remainingMin),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceDim
                            )
                        }
                        if (!currentProgram.description.isNullOrEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                currentProgram.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(stringResource(R.string.epg_no_info), color = OnSurfaceDim)
                    }
                }

                if (upcomingPrograms.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.HorizontalDivider(color = SurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.epg_upcoming_schedule),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(displayPrograms.size) { index ->
                        val program = displayPrograms[index]
                        val isNext = index == 0 && nextProgram != null
                        val focusToken = if (program.id > 0) program.id else program.startTime
                        val shouldRequestFocus = preferredFocusedProgramToken?.let { it == focusToken } ?: (index == 0)

                        Surface(
                            onClick = {
                                if (program.hasArchive || currentChannel?.catchUpSupported == true) {
                                    onPlayCatchUp(program)
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isNext) Primary.copy(alpha = 0.08f) else Color.Transparent,
                                focusedContainerColor = Primary.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (shouldRequestFocus) Modifier.focusRequester(overlayFocusRequester)
                                    else Modifier
                                )
                                .onFocusChanged {
                                    if (it.isFocused) onFocusedProgramChange(focusToken)
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (isNext) {
                                    Text(
                                        text = stringResource(R.string.epg_up_next),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                Text(
                                    text = program.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isNext) Color.White else Color.White.copy(alpha = 0.8f),
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Row {
                                    Text(
                                        text = "${timeFormat.format(Date(program.startTime))} - ${timeFormat.format(Date(program.endTime))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${program.durationMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceDim
                                    )
                                    if (program.hasArchive) {
                                        Spacer(Modifier.width(8.dp))
                                        StatusPill(
                                            label = stringResource(R.string.player_archive_badge),
                                            containerColor = AppColors.Warning,
                                            contentColor = Color.Black
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
}

@Composable
fun DiagnosticsOverlay(
    stats: PlayerStats,
    diagnostics: PlayerDiagnosticsUiState,
    modifier: Modifier = Modifier
) {
    PlayerOverlayPanel(modifier = modifier.width(420.dp)) {
        Text(
            text = stringResource(R.string.player_diagnostics_title),
            color = Primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (diagnostics.providerName.isNotBlank()) {
            PlayerMetaRow(stringResource(R.string.player_diagnostics_provider), diagnostics.providerName)
        }
        if (diagnostics.providerSourceLabel.isNotBlank()) {
            PlayerMetaRow(stringResource(R.string.player_diagnostics_source), diagnostics.providerSourceLabel)
        }
        PlayerMetaRow(stringResource(R.string.player_diagnostics_decoder), diagnostics.decoderMode.name)
        PlayerMetaRow(stringResource(R.string.player_diagnostics_stream_class), diagnostics.streamClassLabel)
        PlayerMetaRow(stringResource(R.string.player_diagnostics_playback_state), diagnostics.playbackStateLabel)
        if (diagnostics.archiveSupportLabel.isNotBlank()) {
            PlayerMetaRow(stringResource(R.string.player_diagnostics_archive), diagnostics.archiveSupportLabel)
        }
        PlayerMetaRow(stringResource(R.string.player_diagnostics_alternates), diagnostics.alternativeStreamCount.toString())
        if (diagnostics.channelErrorCount > 0) {
            PlayerMetaRow(stringResource(R.string.player_diagnostics_channel_errors), diagnostics.channelErrorCount.toString())
        }
        PlayerMetaRow(stringResource(R.string.player_diagnostics_resolution), "${stats.width}x${stats.height}")
        PlayerMetaRow(stringResource(R.string.player_diagnostics_video_codec), stats.videoCodec)
        PlayerMetaRow(stringResource(R.string.player_diagnostics_audio_codec), stats.audioCodec)
        PlayerMetaRow(stringResource(R.string.player_diagnostics_video_bitrate), "${stats.videoBitrate / 1000} kbps")
        PlayerMetaRow(stringResource(R.string.player_diagnostics_dropped_frames), stats.droppedFrames.toString())
        diagnostics.lastFailureReason?.let { reason ->
            PlayerMetaRow(stringResource(R.string.player_diagnostics_last_failure), reason)
        }
        if (diagnostics.recentRecoveryActions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.player_diagnostics_recovery_actions),
                color = Primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            diagnostics.recentRecoveryActions.forEach { action ->
                Text(action, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (diagnostics.troubleshootingHints.isNotEmpty()) {
            Text(
                text = stringResource(R.string.player_diagnostics_troubleshooting),
                color = Primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            diagnostics.troubleshootingHints.forEach { hint ->
                Text(hint, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
