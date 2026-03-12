package com.streamvault.app.ui.screens.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.streamvault.app.ui.components.dialogs.ProgramHistoryDialog
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.streamvault.app.R
import com.streamvault.app.ui.screens.multiview.MultiViewViewModel
import com.streamvault.app.ui.screens.multiview.MultiViewPlannerDialog
import com.streamvault.app.navigation.Routes



@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
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
    archiveStartMs: Long? = null,
    archiveEndMs: Long? = null,
    archiveTitle: String? = null,
    returnRoute: String? = null,
    onBack: () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
    multiViewViewModel: MultiViewViewModel = hiltViewModel()
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
    
    val showChannelListOverlay by viewModel.showChannelListOverlay.collectAsState()
    val showEpgOverlay by viewModel.showEpgOverlay.collectAsState()
    val currentChannelList by viewModel.currentChannelList.collectAsState()
    val recentChannels by viewModel.recentChannels.collectAsState()
    val lastVisitedCategory by viewModel.lastVisitedCategory.collectAsState()
    val displayChannelNumber by viewModel.displayChannelNumber.collectAsState()
    val upcomingPrograms by viewModel.upcomingPrograms.collectAsState()
    val showChannelInfoOverlay by viewModel.showChannelInfoOverlay.collectAsState()
    val numericChannelInput by viewModel.numericChannelInput.collectAsState()
    
    val availableAudioTracks by viewModel.availableAudioTracks.collectAsState()
    val availableSubtitleTracks by viewModel.availableSubtitleTracks.collectAsState()
    val availableVideoQualities by viewModel.availableVideoQualities.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val showDiagnostics by viewModel.showDiagnostics.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()
    val playerDiagnostics by viewModel.playerDiagnostics.collectAsState()
    val currentPosition by viewModel.playerEngine.currentPosition.collectAsState()
    val duration by viewModel.playerEngine.duration.collectAsState()
    val playerNotice by viewModel.playerNotice.collectAsState()
    val currentChannelRecording by viewModel.currentChannelRecording.collectAsState()

    var showTrackSelection by remember { mutableStateOf<TrackType?>(null) }
    var showProgramHistory by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val channelListFocusRequester = remember { FocusRequester() }
    val epgFocusRequester = remember { FocusRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    val channelInfoFocusRequester = remember { FocusRequester() } // NEW
    var lastFocusedChannelListItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastFocusedEpgProgramToken by rememberSaveable { mutableStateOf<Long?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onPlayerScreenDisposed()
        }
    }

    // Consolidated focus management for all overlays
    val anyOverlayVisible = showChannelListOverlay || showEpgOverlay || showChannelInfoOverlay || showTrackSelection != null || showProgramHistory || showSplitDialog || showZapOverlay || showDiagnostics
    
    LaunchedEffect(anyOverlayVisible) {
        if (anyOverlayVisible) {
            // Give overlays a moment to animate in before requesting focus
            delay(150)
            try {
                when {
                    showChannelListOverlay -> channelListFocusRequester.requestFocus()
                    showEpgOverlay -> epgFocusRequester.requestFocus()
                    showChannelInfoOverlay -> channelInfoFocusRequester.requestFocus()
                    // EPG and Dialogs usually handle their own initial focus or use their own re-composition logic
                }
            } catch (_: Exception) {}
        } else {
            // Restore focus to main player when all overlays are gone
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
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

    // Split Screen Manager dialog
    if (showSplitDialog && currentChannel != null) {
        MultiViewPlannerDialog(
            pendingChannel = currentChannel,
            onDismiss = { showSplitDialog = false },
            onLaunch = {
                showSplitDialog = false
                onNavigate?.invoke(Routes.MULTI_VIEW)
            },
            viewModel = multiViewViewModel
        )
    }

    LaunchedEffect(streamUrl, epgChannelId, title, internalChannelId, categoryId, providerId, isVirtual, contentType, archiveStartMs, archiveEndMs, archiveTitle) {
        viewModel.prepare(
            streamUrl = streamUrl,
            epgChannelId = epgChannelId,
            internalChannelId = internalChannelId,
            categoryId = categoryId ?: -1,
            providerId = providerId ?: -1,
            isVirtual = isVirtual,
            contentType = contentType,
            title = title,
            archiveStartMs = archiveStartMs,
            archiveEndMs = archiveEndMs,
            archiveTitle = archiveTitle
        )
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(100)
            try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
            viewModel.hideControlsAfterDelay()
        } else {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val handlePlayerNoticeAction: (PlayerNoticeAction) -> Unit = remember(returnRoute, onNavigate) {
        { action ->
            if (action == PlayerNoticeAction.OPEN_GUIDE && !returnRoute.isNullOrBlank() && onNavigate != null) {
                viewModel.dismissPlayerNotice()
                onNavigate(returnRoute)
            } else {
                viewModel.runPlayerNoticeAction(action)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusProperties {
                // Only allow focus on the main background when no overlays are active
                canFocus = !anyOverlayVisible
            }
            .focusable()
            .onKeyEvent { event ->
                // Only handle KeyDown to avoid double actions
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (contentType == "LIVE" && viewModel.hasPendingNumericChannelInput()) {
                                viewModel.commitNumericChannelInput()
                            } else if (contentType == "LIVE") {
                                if (showChannelInfoOverlay) viewModel.closeChannelInfoOverlay()
                                else viewModel.openChannelInfoOverlay()
                            } else {
                                viewModel.toggleControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (contentType == "LIVE" && !showChannelListOverlay && !showEpgOverlay && !showChannelInfoOverlay) {
                                if (isRtl) viewModel.openEpgOverlay() else viewModel.openChannelListOverlay()
                                true
                            } else if (!showChannelListOverlay && !showEpgOverlay && !showChannelInfoOverlay) {
                                if (isRtl) viewModel.seekForward() else viewModel.seekBackward()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (contentType == "LIVE" && !showChannelListOverlay && !showEpgOverlay && !showChannelInfoOverlay) {
                                if (isRtl) viewModel.openChannelListOverlay() else viewModel.openEpgOverlay()
                                true
                            } else if (!showChannelListOverlay && !showEpgOverlay && !showChannelInfoOverlay) {
                                if (isRtl) viewModel.seekBackward() else viewModel.seekForward()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (showChannelListOverlay || showEpgOverlay) return@onKeyEvent false

                            if (contentType == "LIVE") {
                                viewModel.playNext()
                            } else {
                                viewModel.toggleControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (showChannelListOverlay || showEpgOverlay) return@onKeyEvent false

                            if (contentType == "LIVE") {
                                viewModel.playPrevious()
                            } else {
                                viewModel.toggleControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (viewModel.hasPendingNumericChannelInput()) {
                                viewModel.clearNumericChannelInput()
                                true
                            } else if (playerNotice != null) {
                                viewModel.dismissPlayerNotice()
                                true
                            } else if (showProgramHistory) {
                                showProgramHistory = false
                                true
                            } else if (showSplitDialog) {
                                showSplitDialog = false
                                true
                            } else if (showTrackSelection != null) {
                                showTrackSelection = null
                                true
                            } else if (showDiagnostics) {
                                viewModel.toggleDiagnostics()
                                true
                            } else if (showChannelInfoOverlay) {
                                viewModel.closeChannelInfoOverlay()
                                true
                            } else if (showChannelListOverlay || showEpgOverlay) {
                                viewModel.closeOverlays()
                                true
                            } else if (showControls) {
                                viewModel.toggleControls()
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
                            if (contentType == "LIVE") {
                                viewModel.playNext()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> {
                            if (contentType == "LIVE") {
                                viewModel.playPrevious()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (contentType == "LIVE") {
                                viewModel.zapToLastChannel()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_GUIDE -> {
                            if (contentType == "LIVE") {
                                viewModel.openEpgOverlay()
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_INFO -> {
                            if (contentType == "LIVE") {
                                if (showChannelInfoOverlay) viewModel.closeChannelInfoOverlay()
                                else viewModel.openChannelInfoOverlay()
                            } else {
                                viewModel.toggleControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_MENU -> {
                            viewModel.toggleControls()
                            true
                        }
                        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                            if (contentType == "LIVE" && !showChannelListOverlay && !showEpgOverlay && !showChannelInfoOverlay) {
                                val digit = event.nativeKeyEvent.keyCode - KeyEvent.KEYCODE_0
                                viewModel.inputNumericChannelDigit(digit)
                                true
                            } else {
                                false
                            }
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
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.player_buffering),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = playerNotice != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 116.dp)
        ) {
            Surface(
                onClick = viewModel::dismissPlayerNotice,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xCC5B1F1F),
                    focusedContainerColor = Color(0xFFE45757)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Text(
                        text = playerNotice?.message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    if (!playerNotice?.actions.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            playerNotice?.actions.orEmpty().forEach { action ->
                                Surface(
                                    onClick = { handlePlayerNoticeAction(action) },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = Color.White.copy(alpha = 0.12f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.22f)
                                    )
                                ) {
                                    Text(
                                        text = when (action) {
                                            PlayerNoticeAction.RETRY -> stringResource(R.string.player_retry)
                                            PlayerNoticeAction.LAST_CHANNEL -> stringResource(R.string.player_last_channel_action)
                                            PlayerNoticeAction.ALTERNATE_STREAM -> stringResource(R.string.player_try_alternate_stream)
                                            PlayerNoticeAction.OPEN_GUIDE -> stringResource(R.string.player_open_guide_action)
                                        },
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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
                        text = stringResource(R.string.player_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = ErrorColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show specific error message based on error type
                    val errorMessage = when (playerError) {
                        is PlayerError.NetworkError -> 
                            stringResource(R.string.player_error_network)
                        is PlayerError.SourceError -> 
                            stringResource(R.string.player_error_source)
                        is PlayerError.DecoderError -> 
                            stringResource(R.string.player_error_decoder)
                        is PlayerError.UnknownError -> 
                            playerError?.message ?: stringResource(R.string.player_error_unknown)
                        null -> stringResource(R.string.player_error_unknown)
                    }
                    
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val recoveryActions = buildList {
                        add(PlayerNoticeAction.RETRY)
                        if (contentType == "LIVE" && viewModel.hasAlternateStream()) add(PlayerNoticeAction.ALTERNATE_STREAM)
                        if (contentType == "LIVE" && viewModel.hasLastChannel()) add(PlayerNoticeAction.LAST_CHANNEL)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        recoveryActions.forEach { action ->
                            Surface(
                                onClick = { handlePlayerNoticeAction(action) },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (action == PlayerNoticeAction.RETRY) Primary else Color.White.copy(alpha = 0.08f),
                                    focusedContainerColor = if (action == PlayerNoticeAction.RETRY) PrimaryVariant else Color.White.copy(alpha = 0.18f)
                                )
                            ) {
                                Text(
                                    text = when (action) {
                                        PlayerNoticeAction.RETRY -> stringResource(R.string.player_retry)
                                        PlayerNoticeAction.LAST_CHANNEL -> stringResource(R.string.player_last_channel_action)
                                        PlayerNoticeAction.ALTERNATE_STREAM -> stringResource(R.string.player_try_alternate_stream)
                                        PlayerNoticeAction.OPEN_GUIDE -> stringResource(R.string.player_open_guide_action)
                                    },
                                    color = if (action == PlayerNoticeAction.RETRY) OnBackground else Color.White,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (contentType == "LIVE" && viewModel.hasLastChannel()) {
                            stringResource(R.string.player_error_live_hint)
                        } else {
                            stringResource(R.string.player_error_back)
                        },
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
                                    text = if (contentType == "MOVIE") stringResource(R.string.player_type_movie) else stringResource(R.string.player_type_series),
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
                                    text = stringResource(R.string.player_close),
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
                                            text = "$displayChannelNumber. ${it.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                
                                // Next Program Preview Removed (Moved to EPG Overlay)
                                
                                // Track selection buttons shifted here for Live
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (currentChannel?.catchUpSupported == true) {
                                        QuickSettingsButton(stringResource(R.string.player_restart)) { viewModel.restartCurrentProgram() }
                                        QuickSettingsButton(stringResource(R.string.player_archive)) { showProgramHistory = true }
                                    }
                                    if (currentChannelRecording?.status == com.streamvault.domain.model.RecordingStatus.RECORDING) {
                                        QuickSettingsButton(stringResource(R.string.player_stop_recording)) { viewModel.stopCurrentRecording() }
                                    } else {
                                        QuickSettingsButton(stringResource(R.string.player_record)) { viewModel.startManualRecording() }
                                        QuickSettingsButton(stringResource(R.string.player_schedule_recording)) { viewModel.scheduleRecording() }
                                    }
                                    QuickSettingsButton(stringResource(R.string.player_aspect_ratio_label, aspectRatio.modeName)) { viewModel.toggleAspectRatio() }
                                    if (availableSubtitleTracks.isNotEmpty()) {
                                        QuickSettingsButton(stringResource(R.string.player_subs)) { showTrackSelection = TrackType.TEXT }
                                    }
                                    if (availableAudioTracks.size > 1) {
                                        QuickSettingsButton(stringResource(R.string.player_audio)) { showTrackSelection = TrackType.AUDIO }
                                    }
                                    if (availableVideoQualities.size > 1) {
                                        QuickSettingsButton(stringResource(R.string.player_video_quality)) { showTrackSelection = TrackType.VIDEO }
                                    }
                                    QuickSettingsButton(stringResource(R.string.multiview_nav)) { showSplitDialog = true }
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
                                    QuickSettingsButton(stringResource(R.string.player_aspect_ratio_label, aspectRatio.modeName)) { viewModel.toggleAspectRatio() }
                                    if (availableSubtitleTracks.isNotEmpty()) {
                                        QuickSettingsButton(stringResource(R.string.player_subs)) { showTrackSelection = TrackType.TEXT }
                                    }
                                    if (availableAudioTracks.size > 1) {
                                        QuickSettingsButton(stringResource(R.string.player_audio)) { showTrackSelection = TrackType.AUDIO }
                                    }
                                    if (availableVideoQualities.size > 1) {
                                        QuickSettingsButton(stringResource(R.string.player_video_quality)) { showTrackSelection = TrackType.VIDEO }
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
                        TransportButton("<<") { viewModel.seekBackward() }
                    }
                    
                    if (contentType != "LIVE") {
                        Surface(
                            onClick = { if (isPlaying) viewModel.pause() else viewModel.play() },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Primary.copy(alpha = 0.8f),
                                focusedContainerColor = Primary
                            ),
                            modifier = Modifier
                                .size(80.dp)
                                .focusRequester(playButtonFocusRequester)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = if (isPlaying) "||" else ">",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (contentType != "LIVE") {
                        TransportButton(">>") { viewModel.seekForward() }
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
                            text = displayChannelNumber.toString(),
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
                 }
            }
         }
         
         AnimatedVisibility(
             visible = numericChannelInput != null && contentType == "LIVE" && !showControls,
             enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
             exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
             modifier = Modifier
                 .align(Alignment.TopCenter)
                 .padding(top = 40.dp)
         ) {
             val inputState = numericChannelInput ?: return@AnimatedVisibility
             Box(
                 modifier = Modifier
                     .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(14.dp))
                     .padding(horizontal = 22.dp, vertical = 12.dp)
             ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Text(
                         text = inputState.input,
                         style = MaterialTheme.typography.headlineMedium,
                         color = if (inputState.invalid) ErrorColor else Primary,
                         fontWeight = FontWeight.Bold
                     )
                     Text(
                         text = when {
                             inputState.invalid -> "Channel not found"
                             !inputState.matchedChannelName.isNullOrBlank() -> inputState.matchedChannelName
                             else -> "Type channel number"
                         },
                         style = MaterialTheme.typography.bodySmall,
                         color = Color.White.copy(alpha = 0.85f),
                         maxLines = 1,
                         overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                     )
                 }
             }
         }

         // Aspect Ratio confirmation Toast
         var showAspectRatioToast by remember { mutableStateOf(false) }
         LaunchedEffect(aspectRatio) {
             showAspectRatioToast = true
             delay(2000)
             showAspectRatioToast = false
         }
         
         AnimatedVisibility(
             visible = showAspectRatioToast && !showControls,
             enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
             exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
             modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
         ) {
             Box(
                 modifier = Modifier
                     .background(Primary.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                     .padding(horizontal = 24.dp, vertical = 12.dp)
             ) {
                 Text(
                     text = "Aspect Ratio: ${aspectRatio.modeName}",
                     style = MaterialTheme.typography.titleMedium,
                     color = Color.White,
                     fontWeight = FontWeight.Bold
                 )
             }
         }
         
         // EPG Overlay removed (Unified into side overlay and bottom info overlay)

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
                        text = stringResource(R.string.player_resume_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.player_resume_desc, resumePrompt.title),
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
                                stringResource(R.string.player_resume_start_over),
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
                                stringResource(R.string.player_resume),
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
                val tracks = when (showTrackSelection) {
                    TrackType.AUDIO -> availableAudioTracks
                    TrackType.VIDEO -> availableVideoQualities
                    else -> availableSubtitleTracks
                }
                
                val titleRes = when (showTrackSelection) {
                    TrackType.AUDIO -> R.string.player_track_audio
                    TrackType.VIDEO -> R.string.player_video_quality
                    else -> R.string.player_track_subs
                }
                
                Column(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 400.dp)
                        .background(SurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (showTrackSelection == TrackType.VIDEO) "Video Quality" else stringResource(titleRes),
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
                                    name = stringResource(R.string.player_track_off),
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
                                    when (showTrackSelection) {
                                        TrackType.AUDIO -> viewModel.selectAudioTrack(track.id)
                                        TrackType.VIDEO -> viewModel.selectVideoQuality(track.id)
                                        else -> viewModel.selectSubtitleTrack(track.id)
                                    }
                                    showTrackSelection = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Overlays ---
        if (showDiagnostics) {
            DiagnosticsOverlay(
                stats = playerStats,
                diagnostics = playerDiagnostics,
                modifier = Modifier.align(Alignment.TopStart).padding(32.dp)
            )
        }

        AnimatedVisibility(
            visible = showChannelListOverlay,
            enter = slideInHorizontally(initialOffsetX = { if (isRtl) it else -it }),
            exit = slideOutHorizontally(targetOffsetX = { if (isRtl) it else -it }),
            modifier = Modifier
                .align(if (isRtl) Alignment.TopEnd else Alignment.TopStart)
                .fillMaxHeight()
                .width(350.dp)
                .focusGroup()
        ) {
            ChannelListOverlay(
                channels = currentChannelList,
                recentChannels = recentChannels,
                currentChannelId = currentChannel?.id ?: internalChannelId,
                overlayFocusRequester = channelListFocusRequester,
                preferredFocusedChannelId = lastFocusedChannelListItemId,
                onFocusedChannelChange = { channelId -> lastFocusedChannelListItemId = channelId },
                lastVisitedCategoryName = lastVisitedCategory?.name,
                onOpenLastGroup = { viewModel.openLastVisitedCategory() },
                onSelectChannel = { channelId -> viewModel.zapToChannel(channelId) },
                onDismiss = { viewModel.closeOverlays() }
            )
        }

        AnimatedVisibility(
            visible = showEpgOverlay,
            enter = slideInHorizontally(initialOffsetX = { if (isRtl) -it else it }),
            exit = slideOutHorizontally(targetOffsetX = { if (isRtl) -it else it }),
            modifier = Modifier
                .align(if (isRtl) Alignment.TopStart else Alignment.TopEnd)
                .fillMaxHeight()
                .width(400.dp)
                .focusGroup()
        ) {
            EpgOverlay(
                currentChannel = currentChannel,
                displayChannelNumber = displayChannelNumber,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                upcomingPrograms = upcomingPrograms,
                overlayFocusRequester = epgFocusRequester,
                preferredFocusedProgramToken = lastFocusedEpgProgramToken,
                onFocusedProgramChange = { token -> lastFocusedEpgProgramToken = token },
                onDismiss = { viewModel.closeOverlays() },
                onPlayCatchUp = { program -> 
                    viewModel.playCatchUp(program)
                    viewModel.closeOverlays()
                }
            )
        }

        AnimatedVisibility(
            visible = showChannelInfoOverlay,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .focusGroup()
        ) {
            ChannelInfoOverlay(
                currentChannel = currentChannel,
                displayChannelNumber = displayChannelNumber,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                focusRequester = channelInfoFocusRequester,
                lastVisitedCategoryName = lastVisitedCategory?.name,
                onDismiss = { viewModel.closeChannelInfoOverlay() },
                onOpenFullEpg = {
                    viewModel.closeChannelInfoOverlay()
                    viewModel.openEpgOverlay()
                },
                onOpenLastGroup = {
                    viewModel.closeChannelInfoOverlay()
                    viewModel.openLastVisitedCategory()
                },
                onOpenRecentChannels = {
                    viewModel.closeChannelInfoOverlay()
                    viewModel.openChannelListOverlay()
                },
                onOpenFullControls = {
                    viewModel.closeChannelInfoOverlay()
                    viewModel.toggleControls()
                },
                onRestartProgram = { viewModel.restartCurrentProgram() },
                onToggleAspectRatio = { viewModel.toggleAspectRatio() },
                onToggleDiagnostics = { viewModel.toggleDiagnostics() },
                onTogglePlayPause = { if (isPlaying) viewModel.pause() else viewModel.play() },
                isPlaying = isPlaying,
                currentAspectRatio = aspectRatio.modeName,
                isDiagnosticsEnabled = showDiagnostics,
                onOpenSplitScreen = { showSplitDialog = true }
            )
        }
    }
}

@Composable
fun ChannelInfoOverlay(
    currentChannel: com.streamvault.domain.model.Channel?,
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
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 32.dp)
            .background(
                Color.Black.copy(alpha = 0.55f),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Channel name & number
            if (currentChannel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$displayChannelNumber",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = currentChannel.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (currentChannel.catchUpSupported) {
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .background(Primary.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(stringResource(R.string.player_catchup_badge), style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }

            // Current program info
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
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(
                                    R.string.player_time_range_minutes,
                                    timeFormat.format(java.util.Date(currentProgram.startTime)),
                                    timeFormat.format(java.util.Date(currentProgram.endTime)),
                                    currentProgram.durationMinutes
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    // Progress bar
                    val now = System.currentTimeMillis()
                    val start = currentProgram.startTime
                    val end = currentProgram.endTime
                    if (start in 1..<end) {
                        val progress = (now - start).toFloat() / (end - start)
                        val remainingMin = ((end - now) / 60000).toInt().coerceAtLeast(0)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = Primary,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "$remainingMin min remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim
                        )
                    }
                }
            }

            // Next program & Quick actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
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
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = timeFormat.format(java.util.Date(nextProgram.startTime)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Quick action buttons aligned to the right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickActionButton(
                        icon = if (isPlaying) "||" else ">",
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
                            icon = "R",
                            label = stringResource(R.string.player_restart),
                            onClick = {
                                onRestartProgram()
                                onDismiss()
                            }
                        )
                    }
                    QuickActionButton(
                        icon = "AR",
                        label = currentAspectRatio,
                        onClick = onToggleAspectRatio,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    if (!lastVisitedCategoryName.isNullOrBlank()) {
                        QuickActionButton(
                            icon = "LG",
                            label = stringResource(R.string.player_last_group_label, lastVisitedCategoryName),
                            onClick = onOpenLastGroup
                        )
                    }
                    QuickActionButton(
                        icon = "RC",
                        label = stringResource(R.string.player_recent_channels),
                        onClick = onOpenRecentChannels
                    )
                    QuickActionButton(
                        icon = "EPG",
                        label = stringResource(R.string.player_full_epg),
                        onClick = onOpenFullEpg
                    )
                    QuickActionButton(
                        icon = "MV",
                        label = stringResource(R.string.multiview_nav),
                        onClick = {
                            onDismiss()
                            onOpenSplitScreen()
                        }
                    )
                    QuickActionButton(
                        icon = if (isDiagnosticsEnabled) "ON" else "OFF",
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
        containerColor = Color.White.copy(alpha = 0.1f),
        focusedContainerColor = Primary.copy(alpha = 0.85f)
    ),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = colors,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "iconToggle"
            ) { targetIcon ->
                Text(targetIcon, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
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
                Text(stringResource(R.string.player_selected), color = Primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ChannelListOverlay(
    channels: List<com.streamvault.domain.model.Channel>,
    recentChannels: List<com.streamvault.domain.model.Channel>,
    currentChannelId: Long,
    overlayFocusRequester: FocusRequester = remember { FocusRequester() },
    preferredFocusedChannelId: Long? = null,
    onFocusedChannelChange: (Long) -> Unit = {},
    lastVisitedCategoryName: String? = null,
    onOpenLastGroup: () -> Unit = {},
    onSelectChannel: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
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
    // Scroll to focused/current item when channels load
    LaunchedEffect(channels, preferredIndex) {
        if (channels.isNotEmpty()) {
            listState.scrollToItem(preferredIndex)
        }
    }

    BackHandler { onDismiss() }

    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.70f)),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
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
                            containerColor = Color.White.copy(alpha = 0.08f),
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
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.player_channel_list_hint),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
        if (recentChannels.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
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
                        items(recentChannels, key = { it.id }) { channel ->
                            Surface(
                                onClick = {
                                    onSelectChannel(channel.id)
                                    onDismiss()
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    focusedContainerColor = Primary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val recentNumber = if (channel.number > 0) channel.number.toString() else "?"
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
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            val channelNumber = if (channel.number > 0) channel.number else index + 1

            Surface(
                onClick = {
                    onSelectChannel(channel.id)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
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
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSelected) Primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
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
                        Text(stringResource(R.string.player_channel_selected), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text("  ", style = MaterialTheme.typography.bodySmall)
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (channel.catchUpSupported) {
                        Text(stringResource(R.string.player_archive_badge), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}


@Composable
fun EpgOverlay(
    currentChannel: com.streamvault.domain.model.Channel?,
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
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
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
            .background(Color.Black.copy(alpha = 0.70f))
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Channel info
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.epg_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (currentChannel != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$displayChannelNumber. ${currentChannel.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentChannel.catchUpSupported) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.epg_catchup_available, currentChannel.catchUpDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary.copy(alpha = 0.8f)
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

            // Now Playing section
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${timeFormat.format(java.util.Date(currentProgram.startTime))} - ${timeFormat.format(java.util.Date(currentProgram.endTime))}",
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
                    // Progress bar
                    Spacer(Modifier.height(8.dp))
                    val now = System.currentTimeMillis()
                    val start = currentProgram.startTime
                    val end = currentProgram.endTime
                    if (start in 1..<end) {
                        val progress = (now - start).toFloat() / (end - start)
                        val remainingMin = ((end - now) / 60000).toInt().coerceAtLeast(0)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.player_minutes_remaining, remainingMin),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim
                        )
                    }
                    // Description
                    if (!currentProgram.description.isNullOrEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            currentProgram.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 6,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(stringResource(R.string.epg_no_info), color = OnSurfaceDim)
                }
            }

            // Upcoming schedule
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
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
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
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
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
                            program.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isNext) Color.White else Color.White.copy(alpha = 0.8f),
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row {
                            Text(
                                text = "${timeFormat.format(java.util.Date(program.startTime))} - ${timeFormat.format(java.util.Date(program.endTime))}",
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
                                Text(
                                    text = stringResource(R.string.player_archive_badge),
                                    style = MaterialTheme.typography.labelSmall
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

private fun formatTime(ms: Long): String {
    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(ms))
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

@Composable
fun DiagnosticsOverlay(
    stats: com.streamvault.player.PlayerStats,
    diagnostics: PlayerDiagnosticsUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Player Diagnostics", color = Primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (diagnostics.providerName.isNotBlank()) {
                Text("Provider: ${diagnostics.providerName}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            if (diagnostics.providerSourceLabel.isNotBlank()) {
                Text("Source: ${diagnostics.providerSourceLabel}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            Text("Decoder: ${diagnostics.decoderMode.name}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("Stream Class: ${diagnostics.streamClassLabel}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("Playback State: ${diagnostics.playbackStateLabel}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            if (diagnostics.archiveSupportLabel.isNotBlank()) {
                Text("Archive: ${diagnostics.archiveSupportLabel}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            Text("Alternate Streams: ${diagnostics.alternativeStreamCount}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            if (diagnostics.channelErrorCount > 0) {
                Text("Channel Errors: ${diagnostics.channelErrorCount}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            Text("Resolution: ${stats.width}x${stats.height}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("Video Codec: ${stats.videoCodec}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("Audio Codec: ${stats.audioCodec}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("Video Bitrate: ${stats.videoBitrate / 1000} kbps", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("Dropped Frames: ${stats.droppedFrames}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            diagnostics.lastFailureReason?.let { reason ->
                Spacer(Modifier.height(4.dp))
                Text("Last Failure: $reason", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            if (diagnostics.recentRecoveryActions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Recovery Actions", color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                diagnostics.recentRecoveryActions.forEach { action ->
                    Text("• $action", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (diagnostics.troubleshootingHints.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Troubleshooting", color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                diagnostics.troubleshootingHints.forEach { hint ->
                    Text("- $hint", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
