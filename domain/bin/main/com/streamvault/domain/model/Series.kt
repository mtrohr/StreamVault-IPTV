package com.streamvault.domain.model

data class Series(
    val id: Long,
    val name: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: Float = 0f,
    val tmdbId: Long? = null,
    val youtubeTrailer: String? = null,
    val isFavorite: Boolean = false,
    val providerId: Long = 0,
    val seasons: List<Season> = emptyList(),
    val episodeRunTime: String? = null,
    val lastModified: Long = 0L,
    val isAdult: Boolean = false,
    val isUserProtected: Boolean = false
)

data class Season(
    val seasonNumber: Int,
    val name: String = "Season $seasonNumber",
    val coverUrl: String? = null,
    val episodes: List<Episode> = emptyList(),
    val airDate: String? = null,
    val episodeCount: Int = 0
)

data class Episode(
    val id: Long,
    val title: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val streamUrl: String = "",
    val containerExtension: String? = null,
    val coverUrl: String? = null,
    val plot: String? = null,
    val duration: String? = null,
    val durationSeconds: Int = 0,
    val rating: Float = 0f,
    val releaseDate: String? = null,
    val seriesId: Long = 0,
    val providerId: Long = 0,
    val watchProgress: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val isAdult: Boolean = false,
    val isUserProtected: Boolean = false
)
