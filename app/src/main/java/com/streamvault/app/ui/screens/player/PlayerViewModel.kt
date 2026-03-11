package com.streamvault.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.domain.model.Program
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.DecoderMode
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.model.VideoFormat
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.EpgRepository
import com.streamvault.domain.repository.PlaybackHistoryRepository
import com.streamvault.player.PlaybackState
import com.streamvault.player.PlayerEngine
import com.streamvault.player.PlayerError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResumePromptState(
    val show: Boolean = false,
    val positionMs: Long = 0L,
    val title: String = ""
)

data class NumericChannelInputState(
    val input: String = "",
    val matchedChannelName: String? = null,
    val invalid: Boolean = false
)

enum class AspectRatio(val modeName: String) {
    FIT("Fit"),
    FILL("Stretch"),
    ZOOM("Zoom")
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel @Inject constructor(
    val playerEngine: PlayerEngine,
    private val epgRepository: EpgRepository,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: com.streamvault.domain.repository.FavoriteRepository,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val providerRepository: com.streamvault.domain.repository.ProviderRepository,
    private val preferencesRepository: com.streamvault.data.preferences.PreferencesRepository
) : ViewModel() {

    private val _showControls = MutableStateFlow(false)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    private val _showZapOverlay = MutableStateFlow(false)
    val showZapOverlay: StateFlow<Boolean> = _showZapOverlay.asStateFlow()
    
    private val _currentProgram = MutableStateFlow<Program?>(null)
    val currentProgram: StateFlow<Program?> = _currentProgram.asStateFlow()

    private val _nextProgram = MutableStateFlow<Program?>(null)
    val nextProgram: StateFlow<Program?> = _nextProgram.asStateFlow()

    private val _programHistory = MutableStateFlow<List<Program>>(emptyList())
    val programHistory: StateFlow<List<Program>> = _programHistory.asStateFlow()

    private val _upcomingPrograms = MutableStateFlow<List<Program>>(emptyList())
    val upcomingPrograms: StateFlow<List<Program>> = _upcomingPrograms.asStateFlow()

    private val _currentChannel = MutableStateFlow<com.streamvault.domain.model.Channel?>(null)
    val currentChannel: StateFlow<com.streamvault.domain.model.Channel?> = _currentChannel.asStateFlow()
    
    private val _resumePrompt = MutableStateFlow(ResumePromptState())
    val resumePrompt: StateFlow<ResumePromptState> = _resumePrompt.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatio.FIT)
    val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

    private val _showChannelListOverlay = MutableStateFlow(false)
    val showChannelListOverlay: StateFlow<Boolean> = _showChannelListOverlay.asStateFlow()

    private val _showEpgOverlay = MutableStateFlow(false)
    val showEpgOverlay: StateFlow<Boolean> = _showEpgOverlay.asStateFlow()

    private val _currentChannelList = MutableStateFlow<List<com.streamvault.domain.model.Channel>>(emptyList())
    val currentChannelList: StateFlow<List<com.streamvault.domain.model.Channel>> = _currentChannelList.asStateFlow()

    private val _displayChannelNumber = MutableStateFlow(0)
    val displayChannelNumber: StateFlow<Int> = _displayChannelNumber.asStateFlow()

    private val _showChannelInfoOverlay = MutableStateFlow(false)
    val showChannelInfoOverlay: StateFlow<Boolean> = _showChannelInfoOverlay.asStateFlow()

    private val _numericChannelInput = MutableStateFlow<NumericChannelInputState?>(null)
    val numericChannelInput: StateFlow<NumericChannelInputState?> = _numericChannelInput.asStateFlow()

    private val _showDiagnostics = MutableStateFlow(false)
    val showDiagnostics: StateFlow<Boolean> = _showDiagnostics.asStateFlow()

    private var channelInfoHideJob: Job? = null
    private var numericInputCommitJob: Job? = null
    private var numericInputFeedbackJob: Job? = null
    private var numericInputBuffer: String = ""
    private val triedAlternativeStreams = mutableSetOf<String>()
    private var hasRetriedWithSoftwareDecoder = false

    init {
        viewModelScope.launch {
            playerEngine.error.collect { error ->
                if (error != null) {
                    handlePlaybackError(error)
                }
            }
        }
        viewModelScope.launch {
            playerEngine.playbackState.collect { state ->
                if (state == PlaybackState.READY && currentContentType == ContentType.LIVE) {
                    zapBufferWatchdogJob?.cancel()
                    _currentChannel.value?.let { channel ->
                        if (channel.errorCount > 0) {
                            channelRepository.resetChannelErrorCount(channel.id)
                        }
                    }
                }
            }
        }
    }

    private fun handlePlaybackError(error: PlayerError) {
        if (error is PlayerError.DecoderError && !hasRetriedWithSoftwareDecoder) {
            hasRetriedWithSoftwareDecoder = true
            android.util.Log.w("PlayerVM", "Decoder error detected. Retrying with software decoder mode.")
            playerEngine.setDecoderMode(DecoderMode.SOFTWARE)
            playerEngine.play()
            return
        }

        // Only auto-switch for live TV
        if (currentContentType != ContentType.LIVE) return
        
        // Only for network or source errors (potential broken links)
        if (error is PlayerError.NetworkError || error is PlayerError.SourceError) {
            val channel = _currentChannel.value ?: return
            
            viewModelScope.launch {
                // 1. Mark as broken in DB
                channelRepository.incrementChannelErrorCount(channel.id)
                
                // 2. Try alternative streams
                val nextStream = channel.alternativeStreams.firstOrNull { 
                    it !in triedAlternativeStreams && it != currentStreamUrl 
                }
                
                if (nextStream != null) {
                    triedAlternativeStreams.add(nextStream)
                    android.util.Log.d("PlayerVM", "Switching to alternative stream for current channel")
                    
                    val streamInfo = com.streamvault.domain.model.StreamInfo(
                        url = nextStream,
                        streamType = com.streamvault.domain.model.StreamType.UNKNOWN
                    )
                    playerEngine.prepare(streamInfo)
                    playerEngine.play()
                } else {
                    android.util.Log.e("PlayerVM", "No more alternative streams for current channel")
                    fallbackToPreviousChannel("No alternative stream")
                }
            }
        }
    }

    fun openChannelListOverlay() {
        clearNumericChannelInput()
        _showChannelListOverlay.value = true
        _showEpgOverlay.value = false
        _showChannelInfoOverlay.value = false
        _showControls.value = false
    }

    fun openEpgOverlay() {
        clearNumericChannelInput()
        _showEpgOverlay.value = true
        _showChannelListOverlay.value = false
        _showChannelInfoOverlay.value = false
        _showControls.value = false
    }

    fun openChannelInfoOverlay() {
        clearNumericChannelInput()
        _showChannelInfoOverlay.value = true
        _showChannelListOverlay.value = false
        _showEpgOverlay.value = false
        _showControls.value = false
        channelInfoHideJob?.cancel()
        channelInfoHideJob = viewModelScope.launch {
            delay(3000)
            _showChannelInfoOverlay.value = false
        }
    }

    fun closeChannelInfoOverlay() {
        channelInfoHideJob?.cancel()
        _showChannelInfoOverlay.value = false
    }

    fun closeOverlays() {
        clearNumericChannelInput()
        _showChannelListOverlay.value = false
        _showEpgOverlay.value = false
        _showChannelInfoOverlay.value = false
        channelInfoHideJob?.cancel()
    }

    fun toggleDiagnostics() {
        _showDiagnostics.value = !_showDiagnostics.value
    }

    // Zapping state
    private var channelList: List<com.streamvault.domain.model.Channel> = emptyList()
    private var currentChannelIndex = -1
    private var previousChannelIndex = -1
    private var currentCategoryId: Long = -1
    private var currentProviderId: Long = -1L
    private var currentContentId: Long = -1L
    private var currentContentType: ContentType = ContentType.LIVE
    private var currentTitle: String = ""
    private var isVirtualCategory: Boolean = false
    
    private var epgJob: Job? = null
    private var playlistJob: Job? = null
    private var controlsHideJob: Job? = null
    private var progressTrackingJob: Job? = null
    private var zapOverlayJob: Job? = null
    private var aspectRatioJob: Job? = null
    private var zapBufferWatchdogJob: Job? = null
    private var isAppInForeground: Boolean = true
    private var shouldResumeAfterForeground: Boolean = false
    
    val playerError: StateFlow<PlayerError?> = playerEngine.error
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    val videoFormat: StateFlow<VideoFormat> = playerEngine.videoFormat
    
    val playerStats = playerEngine.playerStats
    val availableAudioTracks = playerEngine.availableAudioTracks
    val availableSubtitleTracks = playerEngine.availableSubtitleTracks

    private val _availableVideoQualities = MutableStateFlow<List<com.streamvault.player.PlayerTrack>>(emptyList())
    val availableVideoQualities: StateFlow<List<com.streamvault.player.PlayerTrack>> = _availableVideoQualities.asStateFlow()

    fun selectAudioTrack(trackId: String) {
        playerEngine.selectAudioTrack(trackId)
    }

    fun selectSubtitleTrack(trackId: String?) {
        playerEngine.selectSubtitleTrack(trackId)
    }

    fun selectVideoQuality(url: String) {
        prepare(
            streamUrl = url,
            epgChannelId = _currentChannel.value?.epgChannelId,
            internalChannelId = currentContentId,
            categoryId = currentCategoryId,
            providerId = currentProviderId,
            isVirtual = isVirtualCategory,
            contentType = currentContentType.name,
            title = currentTitle
        )
    }

    fun prepare(
        streamUrl: String, 
        epgChannelId: String?, 
        internalChannelId: Long, 
        categoryId: Long = -1, 
        providerId: Long = -1, 
        isVirtual: Boolean = false,
        contentType: String = "CHANNEL",
        title: String = ""
    ) {
        currentStreamUrl = streamUrl
        currentProviderId = providerId
        currentContentId = internalChannelId
        currentTitle = title
        currentContentType = try { ContentType.valueOf(contentType) } catch (e: Exception) { ContentType.LIVE }
        hasRetriedWithSoftwareDecoder = false
        playerEngine.setDecoderMode(DecoderMode.AUTO)
        
        // Reset tried streams for manual switch
        triedAlternativeStreams.clear()
        triedAlternativeStreams.add(streamUrl)
        
        val streamInfo = com.streamvault.domain.model.StreamInfo(
            url = streamUrl,
            streamType = com.streamvault.domain.model.StreamType.UNKNOWN
        )
        playerEngine.prepare(streamInfo)
        
        // Show context info on entry for both Live and VOD
        openChannelInfoOverlay()
        
        // 1. Check for Resume Position for VODs
        if (currentContentType != ContentType.LIVE && currentContentId != -1L && currentProviderId != -1L) {
            viewModelScope.launch {
                val history = playbackHistoryRepository.getPlaybackHistory(currentContentId, currentContentType, currentProviderId)
                if (history != null && history.resumePositionMs > 5000L && history.totalDurationMs > 0 && history.resumePositionMs < history.totalDurationMs * 0.95) {
                    playerEngine.pause()
                    _resumePrompt.value = ResumePromptState(
                        show = true,
                        positionMs = history.resumePositionMs,
                        title = currentTitle
                    )
                }
            }
        }
        
        // Load playlist if context changed
        if (categoryId != -1L && (categoryId != currentCategoryId || providerId != currentProviderId)) {
            currentCategoryId = categoryId
            currentProviderId = providerId
            isVirtualCategory = isVirtual
            loadPlaylist(categoryId, providerId, isVirtual, internalChannelId)
        } else {
             // If playlist already loaded, just update index
             if (channelList.isNotEmpty() && internalChannelId != -1L) {
                 currentChannelIndex = channelList.indexOfFirst { it.id == internalChannelId }
                 // Fallback to URL if ID fail
                 if (currentChannelIndex == -1) {
                     currentChannelIndex = channelList.indexOfFirst { it.streamUrl == streamUrl }
                 }
             }
        }
        currentStreamUrl = streamUrl
        
        // Fetch EPG if ID provided
        fetchEpg(currentProviderId, epgChannelId)

        // Load Aspect Ratio safely (fallback to FIT if none saved)
        aspectRatioJob?.cancel()
        _aspectRatio.value = AspectRatio.FIT
        if (internalChannelId != -1L) {
            aspectRatioJob = viewModelScope.launch {
                preferencesRepository.getAspectRatioForChannel(internalChannelId).collect { savedRatio ->
                    _aspectRatio.value = try {
                        savedRatio?.let { AspectRatio.valueOf(it) } ?: AspectRatio.FIT
                    } catch (e: Exception) {
                        AspectRatio.FIT
                    }
                }
            }
            
            // Fetch Channel for tracking alternative streams (video qualities)
            viewModelScope.launch {
                val channel = channelRepository.getChannel(internalChannelId)
                _currentChannel.value = channel
                if (channel != null && channel.alternativeStreams.isNotEmpty()) {
                    val tracks = mutableListOf<com.streamvault.player.PlayerTrack>()
                    tracks.add(
                        com.streamvault.player.PlayerTrack(
                            id = channel.streamUrl,
                            name = "Primary / Default",
                            language = null,
                            type = com.streamvault.player.TrackType.VIDEO,
                            isSelected = streamUrl == channel.streamUrl
                        )
                    )
                    channel.alternativeStreams.forEachIndexed { index, altUrl ->
                        tracks.add(
                            com.streamvault.player.PlayerTrack(
                                id = altUrl,
                                name = "Alternative ${index + 1}",
                                language = null,
                                type = com.streamvault.player.TrackType.VIDEO,
                                isSelected = streamUrl == altUrl
                            )
                        )
                    }
                    _availableVideoQualities.value = tracks as List<com.streamvault.player.PlayerTrack> // Ensure cast
                } else {
                    _availableVideoQualities.value = emptyList()
                }
            }
        } else {
             _availableVideoQualities.value = emptyList()
        }

        // 2. Start Progress Tracking for VODs
        startProgressTracking()
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        if (currentContentType == ContentType.LIVE) return

        progressTrackingJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Track every 5 seconds
                if (!isAppInForeground || !playerEngine.isPlaying.value) continue
                persistPlaybackProgress()
            }
        }
    }

    private suspend fun persistPlaybackProgress() {
        val pos = playerEngine.currentPosition.value
        val dur = playerEngine.duration.value

        if (pos > 0 && dur > 0 && currentContentId != -1L && currentProviderId != -1L) {
            val history = PlaybackHistory(
                contentId = currentContentId,
                contentType = currentContentType,
                providerId = currentProviderId,
                title = currentTitle,
                streamUrl = currentStreamUrl,
                resumePositionMs = pos,
                totalDurationMs = dur,
                lastWatchedAt = System.currentTimeMillis()
            )
            playbackHistoryRepository.updateResumePosition(history)
        }
    }

    fun onAppBackgrounded() {
        if (!isAppInForeground) return
        isAppInForeground = false
        shouldResumeAfterForeground = playerEngine.isPlaying.value
        if (shouldResumeAfterForeground) {
            playerEngine.pause()
        }
        if (currentContentType != ContentType.LIVE) {
            viewModelScope.launch { persistPlaybackProgress() }
        }
    }

    fun onAppForegrounded() {
        if (isAppInForeground) return
        isAppInForeground = true
        if (shouldResumeAfterForeground && !_resumePrompt.value.show) {
            playerEngine.play()
        }
        shouldResumeAfterForeground = false
    }

    fun onPlayerScreenDisposed() {
        if (currentContentType != ContentType.LIVE) {
            viewModelScope.launch { persistPlaybackProgress() }
        }
    }

    private fun fetchEpg(providerId: Long, epgChannelId: String?) {
        epgJob?.cancel()
        if (providerId > 0 && epgChannelId != null) {
            epgJob = viewModelScope.launch {
                launch {
                    epgRepository.getNowAndNext(providerId, epgChannelId).collect { (now, next) ->
                        _currentProgram.value = now
                        _nextProgram.value = next
                    }
                }
                launch {
                    // Fetch last 24 hours.
                    val now = System.currentTimeMillis()
                    val start = now - (24 * 60 * 60 * 1000L)
                    epgRepository.getProgramsForChannel(providerId, epgChannelId, start, now).collect { programs ->
                        _programHistory.value = programs
                            .filter { it.hasArchive }
                            .sortedByDescending { it.startTime }
                    }
                }
                launch {
                    // Fetch next 6 hours.
                    val now = System.currentTimeMillis()
                    val end = now + (6 * 60 * 60 * 1000L)
                    epgRepository.getProgramsForChannel(providerId, epgChannelId, now, end).collect { programs ->
                        _upcomingPrograms.value = programs.sortedBy { it.startTime }
                    }
                }
            }
        } else {
            _currentProgram.value = null
            _nextProgram.value = null
            _programHistory.value = emptyList()
            _upcomingPrograms.value = emptyList()
        }
    }

    private fun loadPlaylist(categoryId: Long, providerId: Long, isVirtual: Boolean, initialChannelId: Long) {
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
                _currentChannelList.value = channels
                // Recalculate index based on initial ID or URL
                if (initialChannelId != -1L) {
                    currentChannelIndex = channelList.indexOfFirst { it.id == initialChannelId }
                }
                if (currentChannelIndex == -1) {
                    currentChannelIndex = channelList.indexOfFirst { it.streamUrl == currentStreamUrl }
                }
                
                if (currentChannelIndex != -1) {
                    _currentChannel.value = channelList[currentChannelIndex]
                    val ch = channelList[currentChannelIndex]
                    _displayChannelNumber.value = if (ch.number > 0) ch.number else currentChannelIndex + 1
                }
            }
        }
    }
    
    // Store current URL to find index later
    private var currentStreamUrl: String = ""

    fun playNext() {
        clearNumericChannelInput()
        if (channelList.isEmpty()) return
        
        if (currentChannelIndex == -1) {
             currentChannelIndex = channelList.indexOfFirst { it.streamUrl == currentStreamUrl }
             if (currentChannelIndex == -1) return
        }
        
        val nextIndex = (currentChannelIndex + 1) % channelList.size
        changeChannel(nextIndex)
    }

    fun playPrevious() {
        clearNumericChannelInput()
        if (channelList.isEmpty()) return
        
        if (currentChannelIndex == -1) {
             currentChannelIndex = channelList.indexOfFirst { it.streamUrl == currentStreamUrl }
             if (currentChannelIndex == -1) return
        }
        
        val prevIndex = if (currentChannelIndex - 1 < 0) channelList.size - 1 else currentChannelIndex - 1
        changeChannel(prevIndex)
    }

    fun zapToChannel(channelId: Long) {
        clearNumericChannelInput()
        if (channelList.isEmpty()) return
        val index = channelList.indexOfFirst { it.id == channelId }
        if (index != -1) {
            changeChannel(index)
            closeOverlays()
        }
    }

    fun zapToLastChannel() {
        clearNumericChannelInput()
        if (channelList.isEmpty() || previousChannelIndex == -1) return
        changeChannel(previousChannelIndex)
    }

    fun hasPendingNumericChannelInput(): Boolean = numericInputBuffer.isNotBlank()

    fun inputNumericChannelDigit(digit: Int) {
        if (currentContentType != ContentType.LIVE || channelList.isEmpty() || digit !in 0..9) return

        if (numericInputBuffer.isEmpty() && digit == 0) {
            zapToLastChannel()
            return
        }

        numericInputBuffer = if (numericInputBuffer.length >= 4) {
            digit.toString()
        } else {
            numericInputBuffer + digit.toString()
        }

        val exactMatch = resolveChannelByNumber(numericInputBuffer.toIntOrNull())
        val previewMatch = exactMatch ?: resolveChannelByPrefix(numericInputBuffer)

        _numericChannelInput.value = NumericChannelInputState(
            input = numericInputBuffer,
            matchedChannelName = previewMatch?.name,
            invalid = false
        )

        scheduleNumericChannelCommit()
    }

    fun commitNumericChannelInput() {
        numericInputCommitJob?.cancel()
        if (numericInputBuffer.isBlank()) return

        val targetChannel = resolveChannelByNumber(numericInputBuffer.toIntOrNull())
        if (targetChannel != null) {
            val targetIndex = channelList.indexOfFirst { it.id == targetChannel.id }
            if (targetIndex != -1) {
                changeChannel(targetIndex)
            }
            clearNumericChannelInput()
            return
        }

        _numericChannelInput.value = NumericChannelInputState(
            input = numericInputBuffer,
            matchedChannelName = null,
            invalid = true
        )

        numericInputFeedbackJob?.cancel()
        numericInputFeedbackJob = viewModelScope.launch {
            delay(900)
            clearNumericChannelInput()
        }
    }

    fun clearNumericChannelInput() {
        numericInputCommitJob?.cancel()
        numericInputFeedbackJob?.cancel()
        numericInputBuffer = ""
        _numericChannelInput.value = null
    }

    private fun changeChannel(index: Int) {
        clearNumericChannelInput()
        if (currentChannelIndex != -1 && currentChannelIndex != index) {
            previousChannelIndex = currentChannelIndex
        }
        val channel = channelList[index]
        currentChannelIndex = index
        _currentChannel.value = channel
        _displayChannelNumber.value = if (channel.number > 0) channel.number else index + 1
        
        // Prepare player
        val streamInfo = com.streamvault.domain.model.StreamInfo(
            url = channel.streamUrl,
            streamType = com.streamvault.domain.model.StreamType.UNKNOWN
        )
        playerEngine.prepare(streamInfo)
        playerEngine.play()
        
        fetchEpg(currentProviderId, channel.epgChannelId)
        
        // Show Zap Overlay & Brief Info
        _showZapOverlay.value = true
        _showControls.value = false 
        openChannelInfoOverlay()
        
        // Reset tried streams for manual switch
        triedAlternativeStreams.clear()
        triedAlternativeStreams.add(channel.streamUrl)
        
        hideZapOverlayAfterDelay()
        if (currentContentType == ContentType.LIVE) {
            scheduleZapBufferWatchdog(index)
        }
    }

    private fun scheduleZapBufferWatchdog(targetIndex: Int) {
        zapBufferWatchdogJob?.cancel()
        zapBufferWatchdogJob = viewModelScope.launch {
            delay(9000)
            val stillOnTarget = currentChannelIndex == targetIndex
            val state = playerEngine.playbackState.value
            val stalled = state == PlaybackState.BUFFERING || state == PlaybackState.ERROR
            if (stillOnTarget && stalled) {
                fallbackToPreviousChannel("Channel timed out in buffering state")
            }
        }
    }

    private fun fallbackToPreviousChannel(reason: String) {
        val fallbackIndex = previousChannelIndex
        if (fallbackIndex in channelList.indices && fallbackIndex != currentChannelIndex) {
            android.util.Log.w("PlayerVM", "Falling back to previous channel: $reason")
            changeChannel(fallbackIndex)
        }
    }

    private fun scheduleNumericChannelCommit() {
        numericInputCommitJob?.cancel()
        numericInputCommitJob = viewModelScope.launch {
            delay(1300)
            commitNumericChannelInput()
        }
    }

    private fun resolveChannelByNumber(number: Int?): com.streamvault.domain.model.Channel? {
        if (number == null) return null
        return channelList
            .withIndex()
            .firstOrNull { (index, channel) -> resolveChannelNumber(channel, index) == number }
            ?.value
    }

    private fun resolveChannelByPrefix(prefix: String): com.streamvault.domain.model.Channel? {
        return channelList
            .withIndex()
            .firstOrNull { (index, channel) ->
                resolveChannelNumber(channel, index).toString().startsWith(prefix)
            }
            ?.value
    }

    private fun resolveChannelNumber(
        channel: com.streamvault.domain.model.Channel,
        index: Int
    ): Int = if (channel.number > 0) channel.number else index + 1

    fun play() = playerEngine.play()
    fun pause() = playerEngine.pause()
    fun seekForward() = playerEngine.seekForward()
    fun seekBackward() = playerEngine.seekBackward()

    fun toggleControls() {
        closeChannelInfoOverlay()
        _showControls.value = !_showControls.value
    }

    fun toggleAspectRatio() {
        val nextRatio = when (_aspectRatio.value) {
            AspectRatio.FIT -> AspectRatio.FILL
            AspectRatio.FILL -> AspectRatio.ZOOM
            AspectRatio.ZOOM -> AspectRatio.FIT
        }
        _aspectRatio.value = nextRatio

        // Save instantly if we have a valid channel ID
        if (currentContentId != -1L) {
            viewModelScope.launch {
                preferencesRepository.setAspectRatioForChannel(currentContentId, nextRatio.name)
            }
        }
    }

    fun playCatchUp(program: Program) {
        viewModelScope.launch {
            val start = program.startTime / 1000L
            val end = program.endTime / 1000L
            val streamId = _currentChannel.value?.id ?: 0L
            val providerId = currentProviderId
            
            if (providerId == -1L || streamId == 0L) return@launch

            val catchUpUrl = providerRepository.buildCatchUpUrl(providerId, streamId, start, end)
            if (catchUpUrl != null) {
                // Update metadata for player
                currentTitle = "${_currentChannel.value?.name ?: ""}: ${program.title}"
                
                val streamInfo = com.streamvault.domain.model.StreamInfo(
                    url = catchUpUrl,
                    streamType = com.streamvault.domain.model.StreamType.UNKNOWN
                )
                playerEngine.prepare(streamInfo)
                playerEngine.play()
                _showControls.value = true
            }
        }
    }

    fun restartCurrentProgram() {
        val program = _currentProgram.value ?: return
        if (program.hasArchive || (_currentChannel.value?.catchUpSupported == true)) {
            playCatchUp(program)
        }
    }

    fun hideControlsAfterDelay() {
        // Cancel previous job to prevent race condition
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(5000)
            _showControls.value = false
        }
    }

    private fun hideZapOverlayAfterDelay() {
        zapOverlayJob?.cancel()
        zapOverlayJob = viewModelScope.launch {
            delay(4000)
            _showZapOverlay.value = false
        }
    }
    
    fun retryStream(streamUrl: String, epgChannelId: String?) {
        val currentId = if (currentChannelIndex != -1 && channelList.isNotEmpty()) channelList[currentChannelIndex].id else -1L
        prepare(streamUrl, epgChannelId, currentId, currentCategoryId, currentProviderId, isVirtualCategory, currentContentType.name, currentTitle)
    }

    fun dismissResumePrompt(resume: Boolean) {
        val prompt = _resumePrompt.value
        _resumePrompt.value = ResumePromptState() // hide
        if (resume && prompt.positionMs > 0) {
            playerEngine.seekTo(prompt.positionMs)
        }
        playerEngine.play()
    }

    override fun onCleared() {
        super.onCleared()
        onPlayerScreenDisposed()
        channelInfoHideJob?.cancel()
        numericInputCommitJob?.cancel()
        numericInputFeedbackJob?.cancel()
        epgJob?.cancel()
        playlistJob?.cancel()
        controlsHideJob?.cancel()
        zapOverlayJob?.cancel()
        zapBufferWatchdogJob?.cancel()
        progressTrackingJob?.cancel()
        aspectRatioJob?.cancel()
        playerEngine.release()
    }
}
