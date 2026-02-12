package com.streamvault.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.streamvault.domain.model.DecoderMode
import com.streamvault.domain.model.StreamInfo
import com.streamvault.domain.model.StreamType
import com.streamvault.domain.model.VideoFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class Media3PlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : PlayerEngine {

    private var exoPlayer: ExoPlayer? = null
    private var currentDecoderMode: DecoderMode = DecoderMode.AUTO

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _videoFormat = MutableStateFlow(VideoFormat(0, 0))
    override val videoFormat: StateFlow<VideoFormat> = _videoFormat.asStateFlow()

    private val _error = MutableSharedFlow<PlayerError?>(replay = 1)
    override val error: Flow<PlayerError?> = _error.asSharedFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                when (currentDecoderMode) {
                    DecoderMode.AUTO -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    DecoderMode.HARDWARE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    DecoderMode.SOFTWARE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                }
            )
        }

        return ExoPlayer.Builder(context, renderersFactory)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _playbackState.value = when (state) {
                            Player.STATE_IDLE -> PlaybackState.IDLE
                            Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                            Player.STATE_READY -> PlaybackState.READY
                            Player.STATE_ENDED -> PlaybackState.ENDED
                            else -> PlaybackState.IDLE
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        _videoFormat.value = VideoFormat(
                            width = videoSize.width,
                            height = videoSize.height,
                            frameRate = videoSize.pixelWidthHeightRatio // Using this as proxy or just storing what we have
                        )
                        // Note: ExoPlayer's VideoSize doesn't directly expose bitrate/codec here easily without TrackSelection
                        // tailored logic, but width/height is the main request.
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _error.tryEmit(PlayerError.fromException(error))
                        _playbackState.value = PlaybackState.ERROR
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        _currentPosition.value = newPosition.positionMs
                    }
                })
            }
    }

    override fun prepare(streamInfo: StreamInfo) {
        val player = getOrCreatePlayer()
        _error.tryEmit(null)

        val dataSourceFactory = createDataSourceFactory(streamInfo)
        val streamType = streamInfo.streamType.takeIf { it != StreamType.UNKNOWN }
            ?: StreamTypeDetector.detect(streamInfo.url)

        val mediaSource = createMediaSource(streamInfo.url, streamType, dataSourceFactory)

        player.setMediaSource(mediaSource)
        player.prepare()
        _videoFormat.value = VideoFormat(0, 0) // Reset format
        player.playWhenReady = true
    }

    private fun createDataSourceFactory(streamInfo: StreamInfo): DataSource.Factory {
        val headers = buildMap {
            putAll(streamInfo.headers)
            streamInfo.userAgent?.let { put("User-Agent", it) }
        }

        return if (headers.isNotEmpty()) {
            OkHttpDataSource.Factory(okHttpClient).apply {
                setDefaultRequestProperties(headers)
            }
        } else {
            OkHttpDataSource.Factory(okHttpClient)
        }
    }

    private fun createMediaSource(
        url: String,
        streamType: StreamType,
        dataSourceFactory: DataSource.Factory
    ): MediaSource {
        val uri = Uri.parse(url)
        val mediaItem = MediaItem.fromUri(uri)

        return when (streamType) {
            StreamType.HLS -> HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)

            StreamType.DASH -> DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            StreamType.MPEG_TS, StreamType.PROGRESSIVE, StreamType.UNKNOWN ->
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
        }
    }

    override fun play() {
        exoPlayer?.playWhenReady = true
    }

    override fun pause() {
        exoPlayer?.playWhenReady = false
    }

    override fun stop() {
        exoPlayer?.stop()
        _playbackState.value = PlaybackState.IDLE
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun seekForward(ms: Long) {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition + ms).coerceAtMost(player.duration)
            player.seekTo(newPos)
        }
    }

    override fun seekBackward(ms: Long) {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition - ms).coerceAtLeast(0)
            player.seekTo(newPos)
        }
    }

    override fun setDecoderMode(mode: DecoderMode) {
        if (currentDecoderMode != mode) {
            currentDecoderMode = mode
            // Recreate player with new decoder settings
            val wasPlaying = exoPlayer?.isPlaying ?: false
            val position = exoPlayer?.currentPosition ?: 0
            val mediaSource = exoPlayer?.currentMediaItem

            exoPlayer?.release()
            exoPlayer = null

            if (mediaSource != null) {
                val player = getOrCreatePlayer()
                player.setMediaItem(mediaSource)
                player.prepare()
                player.seekTo(position)
                player.playWhenReady = wasPlaying
            }
        }
    }

    override fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = PlaybackState.IDLE
        _isPlaying.value = false
    }

    override fun getPlayerView(): Any? = exoPlayer
}
