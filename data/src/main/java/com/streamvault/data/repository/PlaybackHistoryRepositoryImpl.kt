package com.streamvault.data.repository

import com.streamvault.data.local.dao.PlaybackHistoryDao
import com.streamvault.data.mapper.toDomain
import com.streamvault.data.mapper.toEntity
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackHistoryRepositoryImpl @Inject constructor(
    private val dao: PlaybackHistoryDao
) : PlaybackHistoryRepository {

    override fun getRecentlyWatched(limit: Int): Flow<List<PlaybackHistory>> {
        return dao.getRecentlyWatched(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentlyWatchedByProvider(providerId: Long, limit: Int): Flow<List<PlaybackHistory>> {
        return dao.getRecentlyWatchedByProvider(providerId, limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPlaybackHistory(contentId: Long, contentType: ContentType, providerId: Long): PlaybackHistory? {
        return dao.get(contentId, contentType.name, providerId)?.toDomain()
    }

    override suspend fun updateResumePosition(history: PlaybackHistory) {
        val existing = dao.get(history.contentId, history.contentType.name, history.providerId)
        val watchCount = existing?.watchCount ?: 0
        
        // If the item exists and the watch is fresh, maybe bump watch count, but for now just use max 1 or explicit watch_count
        val updatedHistory = history.copy(
            watchCount = if (existing == null) 1 else existing.watchCount + 1,
            lastWatchedAt = System.currentTimeMillis()
        )
        dao.insertOrUpdate(updatedHistory.toEntity())
    }

    override suspend fun removeFromHistory(contentId: Long, contentType: ContentType, providerId: Long) {
        dao.delete(contentId, contentType.name, providerId)
    }

    override suspend fun clearAllHistory() {
        dao.deleteAll()
    }

    override suspend fun clearHistoryForProvider(providerId: Long) {
        dao.deleteByProvider(providerId)
    }
}
