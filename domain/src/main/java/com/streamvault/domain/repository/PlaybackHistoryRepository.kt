package com.streamvault.domain.repository

import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.PlaybackHistory
import kotlinx.coroutines.flow.Flow

interface PlaybackHistoryRepository {
    fun getRecentlyWatched(limit: Int = 100): Flow<List<PlaybackHistory>>
    fun getRecentlyWatchedByProvider(providerId: Long, limit: Int = 100): Flow<List<PlaybackHistory>>
    suspend fun getPlaybackHistory(contentId: Long, contentType: ContentType, providerId: Long): PlaybackHistory?

    suspend fun recordPlayback(history: PlaybackHistory)
    suspend fun updateResumePosition(history: PlaybackHistory)
    suspend fun removeFromHistory(contentId: Long, contentType: ContentType, providerId: Long)
    suspend fun clearAllHistory()
    suspend fun clearHistoryForProvider(providerId: Long)
}
