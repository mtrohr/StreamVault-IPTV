package com.streamvault.domain.model

data class PlaybackHistory(
    val id: Long = 0,
    val contentId: Long,
    val contentType: ContentType,
    val providerId: Long,
    val title: String,
    val posterUrl: String? = null,
    val streamUrl: String,
    val resumePositionMs: Long = 0,
    val totalDurationMs: Long = 0,
    val lastWatchedAt: Long = 0,
    val watchCount: Int = 1,
    val seriesId: Long? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)
