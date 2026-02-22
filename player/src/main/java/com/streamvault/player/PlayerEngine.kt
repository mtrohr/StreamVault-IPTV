package com.streamvault.player

import com.streamvault.domain.model.DecoderMode
import com.streamvault.domain.model.StreamInfo
import com.streamvault.domain.model.VideoFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the underlying media player.
 * UI layer talks to this interface, never to ExoPlayer directly.
 */
interface PlayerEngine {
    val playbackState: StateFlow<PlaybackState>
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val videoFormat: StateFlow<VideoFormat>
    val error: Flow<PlayerError?>

    // Tracks
    val availableAudioTracks: StateFlow<List<PlayerTrack>>
    val availableSubtitleTracks: StateFlow<List<PlayerTrack>>

    fun prepare(streamInfo: StreamInfo)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekForward(ms: Long = 10_000)
    fun seekBackward(ms: Long = 10_000)
    fun setDecoderMode(mode: DecoderMode)
    fun setVolume(volume: Float)
    fun selectAudioTrack(trackId: String)
    fun selectSubtitleTrack(trackId: String?) // null to disable subtitles
    fun release()

    /** Returns the underlying player view for Compose embedding */
    fun getPlayerView(): Any?
}

enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

sealed class PlayerError(val message: String) {
    class NetworkError(message: String) : PlayerError(message)
    class SourceError(message: String) : PlayerError(message)
    class DecoderError(message: String) : PlayerError(message)
    class UnknownError(message: String) : PlayerError(message)

    companion object {
        fun fromException(e: Throwable): PlayerError {
            val msg = e.message ?: "Unknown playback error"
            return when {
                msg.contains("Unable to connect", ignoreCase = true) -> NetworkError(msg)
                msg.contains("timeout", ignoreCase = true) -> NetworkError(msg)
                msg.contains("Response code: 4", ignoreCase = true) -> SourceError(msg)
                msg.contains("Response code: 5", ignoreCase = true) -> NetworkError(msg)
                msg.contains("decoder", ignoreCase = true) -> DecoderError(msg)
                msg.contains("codec", ignoreCase = true) -> DecoderError(msg)
                else -> UnknownError(msg)
            }
        }
    }
}
