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
import androidx.hilt.navigation.compose.hiltViewModel
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerEngine: PlayerEngine,
    private val epgRepository: EpgRepository,
    private val channelRepository: com.streamvault.domain.repository.ChannelRepository,
    private val favoriteRepository: com.streamvault.domain.repository.FavoriteRepository
) : ViewModel() {

    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()
    
    private val _currentProgram = MutableStateFlow<Program?>(null)
    val currentProgram: StateFlow<Program?> = _currentProgram.asStateFlow()
    
    // Zapping state
    private var channelList: List<com.streamvault.domain.model.Channel> = emptyList()
    private var currentChannelIndex = -1
    private var currentCategoryId: Long = -1
    private var currentProviderId: Long = -1
    private var isVirtualCategory: Boolean = false
    
    private var epgJob: kotlinx.coroutines.Job? = null
    private var playlistJob: kotlinx.coroutines.Job? = null
    private var hideControlsJob: kotlinx.coroutines.Job? = null
    
    val playerError: StateFlow<PlayerError?> = playerEngine.error
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    val videoFormat: StateFlow<VideoFormat> = playerEngine.videoFormat

    fun prepare(streamUrl: String, epgChannelId: String?, categoryId: Long = -1, providerId: Long = -1, isVirtual: Boolean = false) {
        val streamInfo = StreamInfo(
            url = streamUrl,
            streamType = com.streamvault.domain.model.StreamType.UNKNOWN
        )
        playerEngine.prepare(streamInfo)
        
        // Load playlist if context changed
        if (categoryId != -1L && (categoryId != currentCategoryId || providerId != currentProviderId)) {
            currentCategoryId = categoryId
            currentProviderId = providerId
            isVirtualCategory = isVirtual
            loadPlaylist(categoryId, providerId, isVirtual)
        } else {
             // If playlist already loaded, just update index
             if (channelList.isNotEmpty()) {
                 currentChannelIndex = channelList.indexOfFirst { it.streamUrl == streamUrl }
             }
        }
        currentStreamUrl = streamUrl
        
        // Fetch EPG if ID provided
        fetchEpg(epgChannelId)
    }

    private fun fetchEpg(epgChannelId: String?) {
        epgJob?.cancel()
        if (epgChannelId != null) {
            epgJob = viewModelScope.launch {
                epgRepository.getNowPlaying(epgChannelId).collect { program ->
                    _currentProgram.value = program
                }
            }
        } else {
            _currentProgram.value = null
        }
    }

    private fun loadPlaylist(categoryId: Long, providerId: Long, isVirtual: Boolean) {
        playlistJob?.cancel()
        playlistJob = viewModelScope.launch {
            val flows = if (isVirtual) {
                if (categoryId == -999L) {
                    // Global Favorites
                    favoriteRepository.getFavorites(com.streamvault.domain.model.ContentType.LIVE)
                        .map { favorites -> favorites.map { it.contentId } }
                        .flatMapLatest { ids -> 
                            if (ids.isEmpty()) flowOf(emptyList()) 
                            else channelRepository.getChannelsByIds(ids)
                        }
                } else {
                    // Custom Group
                    // categoryId is positive here from args? 
                    // In HomeViewModel, category.id is negative for custom groups.
                    // We passed category.id directly. So it should be negative if it came from HomeViewModel.
                    // But wait, HomeViewModel passes `category.id`.
                    // If it is a custom group, ID is negative.
                    // So we should handle it.
                    // BUT `FavoriteRepository.getFavoritesByGroup` expects positive Group ID.
                    // In HomeViewModel: `groupId = -category.id`.
                    // So here strict check:
                    val groupId = if (categoryId < 0) -categoryId else categoryId
                    favoriteRepository.getFavoritesByGroup(groupId)
                        .map { favorites -> favorites.map { it.contentId } }
                        .flatMapLatest { ids -> 
                            if (ids.isEmpty()) flowOf(emptyList()) 
                            else channelRepository.getChannelsByIds(ids)
                        }
                }
            } else {
                channelRepository.getChannelsByCategory(providerId, categoryId)
            }
            
            flows.collect { channels ->
                channelList = channels
                // Recalculate index based on currently playing URL (from prepare call)
                // We'll rely on prepare checking streamUrl.
                // But prepare is called ONCE.
                // We should update index here if we can match the URL.
                // Since we don't have URL here, we will just use playNext/playPrevious carefully.
                // Ideally we should track currentUrl efficiently.
                // For now, next PlayNext call will find current index if -1.
                // But PlayNext cannot find it if we don't store it.
            }
        }
    }
    
    // Store current URL to find index later
    private var currentStreamUrl: String = ""

    fun playNext() {
        if (channelList.isEmpty()) return
        
        if (currentChannelIndex == -1) {
             currentChannelIndex = channelList.indexOfFirst { it.streamUrl == currentStreamUrl }
             if (currentChannelIndex == -1) return
        }
        
        val nextIndex = (currentChannelIndex + 1) % channelList.size
        changeChannel(nextIndex)
    }

    fun playPrevious() {
        if (channelList.isEmpty()) return
        
        if (currentChannelIndex == -1) {
             currentChannelIndex = channelList.indexOfFirst { it.streamUrl == currentStreamUrl }
             if (currentChannelIndex == -1) return
        }
        
        val prevIndex = if (currentChannelIndex - 1 < 0) channelList.size - 1 else currentChannelIndex - 1
        changeChannel(prevIndex)
    }

    private fun changeChannel(index: Int) {
        val channel = channelList[index]
        currentChannelIndex = index
        
        // Prepare player
        val streamInfo = StreamInfo(
            url = channel.streamUrl,
            streamType = com.streamvault.domain.model.StreamType.UNKNOWN
        )
        playerEngine.prepare(streamInfo)
        playerEngine.play()
        
        fetchEpg(channel.epgChannelId)
    }

    fun play() = playerEngine.play()
    fun pause() = playerEngine.pause()
    fun seekForward() = playerEngine.seekForward()
    fun seekBackward() = playerEngine.seekBackward()

    fun toggleControls() {
        _showControls.value = !_showControls.value
    }

    fun hideControlsAfterDelay() {
        // Cancel previous job to prevent race condition
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(5000)
            _showControls.value = false
        }
    }
    
    fun retryStream(streamUrl: String, epgChannelId: String?) {
        prepare(streamUrl, epgChannelId, currentCategoryId, currentProviderId, isVirtualCategory)
    }

    override fun onCleared() {
        super.onCleared()
        hideControlsJob?.cancel()
        playerEngine.release()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    epgChannelId: String? = null,
    categoryId: Long? = null,
    providerId: Long? = null,
    isVirtual: Boolean = false,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playerEngine.playbackState.collectAsState()
    val isPlaying by viewModel.playerEngine.isPlaying.collectAsState()
    val showControls by viewModel.showControls.collectAsState()
    val videoFormat by viewModel.videoFormat.collectAsState()
    val playerError by viewModel.playerError.collectAsState()
    val currentProgram by viewModel.currentProgram.collectAsState()
    
    // Show resolution overlay temporarily when it changes
    var showResolution by remember { mutableStateOf(false) }
    
    LaunchedEffect(videoFormat) {
        if (!videoFormat.isEmpty) {
            showResolution = true
            delay(3000)
            showResolution = false
        }
    }

    LaunchedEffect(streamUrl, epgChannelId) {
        viewModel.prepare(streamUrl, epgChannelId, categoryId ?: -1, providerId ?: -1, isVirtual)
    }

    LaunchedEffect(showControls) {
        if (showControls) viewModel.hideControlsAfterDelay()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                            onBack()
                            true
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
            AndroidView<PlayerView>(
                factory = { context ->
                    PlayerView(context).apply {
                        this.player = player
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
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

        // Controls overlay
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Title at top
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(32.dp)
                )

                // Current Program Info
                if (currentProgram != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 32.dp, bottom = 32.dp, end = 32.dp)
                            .widthIn(max = 600.dp)
                    ) {
                        Text(
                            text = currentProgram?.title ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        
                        val now = System.currentTimeMillis()
                        val start = currentProgram?.startTime ?: 0
                        val end = currentProgram?.endTime ?: 0
                        
                        if (start > 0 && end > 0) {
                            val totalDuration = end - start
                            val elapsed = now - start
                            val progress = if (totalDuration > 0) elapsed.toFloat() / totalDuration else 0f
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Primary,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text(
                                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(start)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(end)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Playback controls at center
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton("⏪") { viewModel.seekBackward() }
                    ControlButton(if (isPlaying) "⏸" else "▶") {
                        if (isPlaying) viewModel.pause() else viewModel.play()
                    }
                    ControlButton("⏩") { viewModel.seekForward() }
                }

                // Back button
                Surface(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(32.dp),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        focusedContainerColor = Primary.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "✕ Back",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
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
    }
}

@Composable
private fun ControlButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.2f),
            focusedContainerColor = Primary.copy(alpha = 0.8f)
        ),
        modifier = Modifier.size(64.dp)
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
