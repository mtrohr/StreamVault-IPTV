package com.streamvault.data.remote.dto

import com.google.gson.annotations.SerializedName

data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: XtreamUserInfo,
    @SerializedName("server_info") val serverInfo: XtreamServerInfo
)

data class XtreamUserInfo(
    @SerializedName("username") val username: String = "",
    @SerializedName("password") val password: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("auth") val auth: Int = 0,
    @SerializedName("status") val status: String = "",
    @SerializedName("exp_date") val expDate: String? = null,
    @SerializedName("is_trial") val isTrial: String = "0",
    @SerializedName("active_cons") val activeConnections: String = "0",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("max_connections") val maxConnections: String = "1",
    @SerializedName("allowed_output_formats") val allowedOutputFormats: List<String> = emptyList()
)

data class XtreamServerInfo(
    @SerializedName("url") val url: String = "",
    @SerializedName("port") val port: String = "",
    @SerializedName("https_port") val httpsPort: String = "",
    @SerializedName("server_protocol") val serverProtocol: String = "http",
    @SerializedName("rtmp_port") val rtmpPort: String = "",
    @SerializedName("timezone") val timezone: String = "",
    @SerializedName("timestamp_now") val timestampNow: Long = 0,
    @SerializedName("time_now") val timeNow: String = ""
)

data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String = "0",
    @SerializedName("category_name") val categoryName: String = "",
    @SerializedName("parent_id") val parentId: Int = 0
)

data class XtreamStream(
    @SerializedName("num") val num: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("stream_type") val streamType: String = "",
    @SerializedName("stream_id") val streamId: Long = 0,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("epg_channel_id") val epgChannelId: String? = null,
    @SerializedName("added") val added: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("custom_sid") val customSid: String? = null,
    @SerializedName("tv_archive") val tvArchive: Int = 0,
    @SerializedName("direct_source") val directSource: String? = null,
    @SerializedName("tv_archive_duration") val tvArchiveDuration: Int? = null,
    @SerializedName("container_extension") val containerExtension: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("rating_5based") val rating5based: String? = null
)

data class XtreamSeriesItem(
    @SerializedName("series_id") val seriesId: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("cast") val cast: String? = null,
    @SerializedName("director") val director: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("rating_5based") val rating5based: String? = null,
    @SerializedName("backdrop_path") val backdropPath: List<String>? = null,
    @SerializedName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerializedName("episode_run_time") val episodeRunTime: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("last_modified") val lastModified: String? = null
)

data class XtreamSeriesInfoResponse(
    @SerializedName("info") val info: XtreamSeriesItem? = null,
    @SerializedName("episodes") val episodes: Map<String, List<XtreamEpisode>> = emptyMap(),
    @SerializedName("seasons") val seasons: List<XtreamSeason> = emptyList()
)

data class XtreamSeason(
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("air_date") val airDate: String? = null,
    @SerializedName("episode_count") val episodeCount: Int = 0
)

data class XtreamEpisode(
    @SerializedName("id") val id: String = "",
    @SerializedName("episode_num") val episodeNum: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("container_extension") val containerExtension: String? = null,
    @SerializedName("custom_sid") val customSid: String? = null,
    @SerializedName("added") val added: String? = null,
    @SerializedName("season") val season: Int = 0,
    @SerializedName("direct_source") val directSource: String? = null,
    @SerializedName("info") val info: XtreamEpisodeInfo? = null
)

data class XtreamEpisodeInfo(
    @SerializedName("movie_image") val movieImage: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("releasedate") val releaseDate: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("duration_secs") val durationSecs: Int = 0,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("bitrate") val bitrate: Int = 0
)

data class XtreamVodInfoResponse(
    @SerializedName("info") val info: XtreamVodInfo? = null,
    @SerializedName("movie_data") val movieData: XtreamVodMovieData? = null
)

data class XtreamVodInfo(
    @SerializedName("movie_image") val movieImage: String? = null,
    @SerializedName("tmdb_id") val tmdbId: Long? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("cast") val cast: String? = null,
    @SerializedName("director") val director: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("releasedate") val releaseDate: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerializedName("duration_secs") val durationSecs: Int = 0,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("backdrop_path") val backdropPath: List<String>? = null,
    @SerializedName("bitrate") val bitrate: Int = 0
)

data class XtreamVodMovieData(
    @SerializedName("stream_id") val streamId: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("added") val added: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("container_extension") val containerExtension: String? = null,
    @SerializedName("custom_sid") val customSid: String? = null,
    @SerializedName("direct_source") val directSource: String? = null
)

data class XtreamEpgResponse(
    @SerializedName("epg_listings") val epgListings: List<XtreamEpgListing> = emptyList()
)

data class XtreamEpgListing(
    @SerializedName("id") val id: String = "",
    @SerializedName("epg_id") val epgId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("lang") val lang: String = "",
    @SerializedName("start") val start: String = "",
    @SerializedName("end") val end: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("channel_id") val channelId: String = "",
    @SerializedName("start_timestamp") val startTimestamp: Long = 0,
    @SerializedName("stop_timestamp") val stopTimestamp: Long = 0,
    @SerializedName("now_playing") val nowPlaying: Int = 0,
    @SerializedName("has_archive") val hasArchive: Int = 0
)
