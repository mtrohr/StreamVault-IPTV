package com.streamvault.app.ui.screens.player

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.DecoderMode
import com.streamvault.domain.model.StreamInfo
import com.streamvault.domain.model.VideoFormat
import com.streamvault.domain.model.Program
import com.streamvault.domain.repository.EpgRepository
import com.streamvault.player.PlaybackState
import com.streamvault.player.PlayerEngine
import com.streamvault.player.PlayerError
import com.streamvault.player.PlayerTrack
import com.streamvault.player.TrackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.streamvault.app.ui.components.dialogs.ProgramHistoryDialog



@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    epgChannelId: String? = null,
    internalChannelId: Long = -1L,
    categoryId: Long? = null,
    providerId: Long? = null,
    isVirtual: Boolean = false,
    contentType: String = "LIVE",
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playerEngine.playbackState.collectAsState()
    val isPlaying by viewModel.playerEngine.isPlaying.collectAsState()
    val showControls by viewModel.showControls.collectAsState()
    val videoFormat by viewModel.videoFormat.collectAsState()
    val playerError by viewModel.playerError.collectAsState()
    val currentProgram by viewModel.currentProgram.collectAsState()
    val nextProgram by viewModel.nextProgram.collectAsState()
    val programHistory by viewModel.programHistory.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val showZapOverlay by viewModel.showZapOverlay.collectAsState()
    val resumePrompt by viewModel.resumePrompt.collectAsState()
    
    val availableAudioTracks by viewModel.availableAudioTracks.collectAsState()
    val availableSubtitleTracks by viewModel.availableSubtitleTracks.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val currentPosition by viewModel.playerEngine.currentPosition.collectAsState()
    val duration by viewModel.playerEngine.duration.collectAsState()

    var showTrackSelection by remember { mutableStateOf<TrackType?>(null) }
    var showProgramHistory by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Show resolution overlay temporarily when it changes
    var showResolution by remember { mutableStateOf(false) }
    
    LaunchedEffect(videoFormat) {
        if (!videoFormat.isEmpty) {
            showResolution = true
            delay(3000)
            showResolution = false
        }
    }

    if (showProgramHistory) {
        ProgramHistoryDialog(
            programs = programHistory,
            onDismiss = { showProgramHistory = false },
            onProgramSelect = { program ->
                viewModel.playCatchUp(program)
                showProgramHistory = false
            }
        )
    }

    LaunchedEffect(streamUrl, epgChannelId) {
        viewModel.prepare(streamUrl, epgChannelId, internalChannelId, categoryId ?: -1, providerId ?: -1, isVirtual, contentType)
    }

    LaunchedEffect(showControls) {
        if (showControls) viewModel.hideControlsAfterDelay()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                // Only handle KeyDown to avoid double actions
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            viewModel.toggleControls()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            viewModel.seekBackward()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            viewModel.seekForward()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            viewModel.playNext()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            viewModel.playPrevious()
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (showTrackSelection != null) {
                                showTrackSelection = null
                                true
                            } else {
                                onBack()
                                true
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (isPlaying) viewModel.pause() else viewModel.play()
                            true
                        }
                        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP_RIGHT -> {
                             viewModel.playNext()
                             true
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> {
                             viewModel.playPrevious()
                             true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // ExoPlayer Video Surface
        val player = viewModel.playerEngine.getPlayerView()
        if (player is androidx.media3.common.Player) {
            AndroidView<androidx.media3.ui.PlayerView>(
                factory = { context ->
                    androidx.media3.ui.PlayerView(context).apply {
                        this.player = player
                        useController = false
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = when (aspectRatio) {
                        AspectRatio.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatio.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatio.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering indicator
        if (playbackState == PlaybackState.BUFFERING) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Buffering...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }

        // Error overlay
        if (playbackState == PlaybackState.ERROR) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️ Playback Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = ErrorColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show specific error message based on error type
                    val errorMessage = when (playerError) {
                        is PlayerError.NetworkError -> 
                            "Stream unavailable — check your internet connection"
                        is PlayerError.SourceError -> 
                            "Stream not found or access denied"
                        is PlayerError.DecoderError -> 
                            "Unable to play this format — try changing decoder mode in Settings"
                        is PlayerError.UnknownError -> 
                            playerError?.message ?: "Unknown playback error"
                        null -> "Unknown playback error"
                    }
                    
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Retry button
                    Surface(
                        onClick = { viewModel.retryStream(streamUrl, epgChannelId) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Primary,
                            focusedContainerColor = PrimaryVariant
                        )
                    ) {
                        Text(
                            text = "Retry",
                            color = OnBackground,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Or press Back to return",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )
                }
            }
        }

        // Cinematic Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient & Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (contentType != "LIVE") {
                                Text(
                                    text = if (contentType == "MOVIE") "Movie" else "Series",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // System Clock
                            val currentTime = remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
                            LaunchedEffect(Unit) {
                                while(true) {
                                    delay(10000)
                                    currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                }
                            }
                            Text(
                                text = currentTime.value,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(end = 24.dp)
                            )

                            // Exit Button
                            Surface(
                                onClick = onBack,
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Primary.copy(alpha = 0.9f)
                                )
                            ) {
                                Text(
                                    text = "✕ Close",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Gradient & Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp, vertical = 32.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (contentType == "LIVE" && currentProgram != null) {
                            // Live TV Program Info
                            Row(verticalAlignment = Alignment.Bottom) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentProgram?.title ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    currentChannel?.let {
                                        Text(
                                            text = "${it.number}. ${it.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                
                                // Next Program Preview
                                if (nextProgram != null) {
                                    Column(
                                        modifier = Modifier
                                            .padding(end = 24.dp)
                                            .widthIn(max = 200.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "NEXT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = nextProgram?.title ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextProgram?.startTime ?: 0)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                
                                // Track selection buttons shifted here for Live
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (currentChannel?.catchUpSupported == true) {
                                        QuickSettingsButton("🔄 Restart") { viewModel.restartCurrentProgram() }
                                        QuickSettingsButton("📼 Archive") { showProgramHistory = true }
                                    }
                                    QuickSettingsButton("📺 ${aspectRatio.modeName}") { viewModel.toggleAspectRatio() }
                                    if (availableSubtitleTracks.isNotEmpty()) {
                                        QuickSettingsButton("💬 Subs") { showTrackSelection = TrackType.TEXT }
                                    }
                                    if (availableAudioTracks.size > 1) {
                                        QuickSettingsButton("🔊 Audio") { showTrackSelection = TrackType.AUDIO }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Progress bar for Live TV
                            val now = System.currentTimeMillis()
                            val start = currentProgram?.startTime ?: 0
                            val end = currentProgram?.endTime ?: 0
                            if (start > 0 && end > 0) {
                                val progress = (now - start).toFloat() / (end - start)
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = Primary,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(start)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(end)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else if (contentType != "LIVE") {
                            // VOD Seek Bar
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDuration(currentPosition),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Slider(
                                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                    onValueChange = { /* Handled via DPAD usually */ },
                                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                                Text(
                                    text = formatDuration(duration),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Content info for VOD
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )

                                // Track selection for VOD
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QuickSettingsButton("📺 ${aspectRatio.modeName}") { viewModel.toggleAspectRatio() }
                                    if (availableSubtitleTracks.isNotEmpty()) {
                                        QuickSettingsButton("💬 Subs") { showTrackSelection = TrackType.TEXT }
                                    }
                                    if (availableAudioTracks.size > 1) {
                                        QuickSettingsButton("🔊 Audio") { showTrackSelection = TrackType.AUDIO }
                                    }
                                }
                            }
                        }
                    }
                }

                // Center Playback Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (contentType != "LIVE") {
                        TransportButton("⏪") { viewModel.seekBackward() }
                    }
                    
                    Surface(
                        onClick = { if (isPlaying) viewModel.pause() else viewModel.play() },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Primary.copy(alpha = 0.8f),
                            focusedContainerColor = Primary
                        ),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = if (isPlaying) "⏸" else "▶",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }

                    if (contentType != "LIVE") {
                        TransportButton("⏩") { viewModel.seekForward() }
                    }
                }
            }
        }
        
        // Cinematic Zap Overlay
        AnimatedVisibility(
            visible = showZapOverlay && !showControls && currentChannel != null,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
                    .widthIn(min = 300.dp, max = 450.dp)
            ) {
                 Column {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                            text = currentChannel?.number?.toString() ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = currentChannel?.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            if (currentProgram != null) {
                                Text(
                                    text = currentProgram?.title ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                     }
                     
                     if (currentProgram != null) {
                         val now = System.currentTimeMillis()
                         val start = currentProgram?.startTime ?: 0
                         val end = currentProgram?.endTime ?: 0
                         if (start > 0 && end > 0) {
                             val progress = (now - start).toFloat() / (end - start)
                             Spacer(modifier = Modifier.height(8.dp))
                             androidx.compose.material3.LinearProgressIndicator(
                                 progress = { progress.coerceIn(0f, 1f) },
                                 modifier = Modifier.fillMaxWidth().height(2.dp),
                                 color = Primary,
                                 trackColor = Color.White.copy(alpha = 0.2f)
                             )
                         }
                     }
                 }
            }
        }

        // Resolution Overlay (Top Right)
        if (showResolution && !showControls && !videoFormat.isEmpty) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = videoFormat.resolutionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
        
        // Resume Prompt Dialog
        if (resumePrompt.show) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .background(SurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Resume Playback",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Do you want to resume ${resumePrompt.title} from where you left off?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.dismissResumePrompt(resume = false) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = SurfaceVariant,
                                focusedContainerColor = SurfaceHighlight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Start Over",
                                modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = Color.White
                            )
                        }
                        
                        Surface(
                            onClick = { viewModel.dismissResumePrompt(resume = true) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Primary,
                                focusedContainerColor = PrimaryVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Resume",
                                modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // Track Selection Dialog
        if (showTrackSelection != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(onClick = { showTrackSelection = null }),
                contentAlignment = Alignment.Center
            ) {
                val tracks = if (showTrackSelection == TrackType.AUDIO) availableAudioTracks else availableSubtitleTracks
                
                Column(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 400.dp)
                        .background(SurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (showTrackSelection == TrackType.AUDIO) "Select Audio Track" else "Select Subtitles",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showTrackSelection == TrackType.TEXT) {
                            item {
                                TrackItem(
                                    name = "Off",
                                    isSelected = tracks.none { it.isSelected },
                                    onClick = {
                                        viewModel.selectSubtitleTrack(null)
                                        showTrackSelection = null
                                    }
                                )
                            }
                        }
                        
                        items(tracks.size) { index ->
                            val track = tracks[index]
                            TrackItem(
                                name = track.name,
                                isSelected = track.isSelected,
                                onClick = {
                                    if (showTrackSelection == TrackType.AUDIO) {
                                        viewModel.selectAudioTrack(track.id)
                                    } else {
                                        viewModel.selectSubtitleTrack(track.id)
                                    }
                                    showTrackSelection = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.2f) else Color.Transparent,
            focusedContainerColor = SurfaceHighlight
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) Primary else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Text("✓", color = Primary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun QuickSettingsButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Primary.copy(alpha = 0.9f)
        )
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TransportButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, remainingMinutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", remainingMinutes, seconds)
    }
}
