package com.streamvault.data.remote.xtream

import com.streamvault.data.remote.dto.*
import com.streamvault.domain.model.*
import com.streamvault.domain.provider.IptvProvider
import com.streamvault.domain.util.ChannelNormalizer
import java.util.Base64

/**
 * Xtream Codes provider implementation.
 * Converts Xtream API responses to domain models.
 */
class XtreamProvider(
    override val providerId: Long,
    private val api: XtreamApiService,
    private val serverUrl: String,
    private val username: String,
    private val password: String
) : IptvProvider {

    private var serverInfo: XtreamServerInfo? = null

    override suspend fun authenticate(): Result<Provider> = try {
        val response = api.authenticate(username, password)
        serverInfo = response.serverInfo

        if (response.userInfo.auth != 1) {
            Result.error("Authentication failed: ${response.userInfo.message}")
        } else {
            // Parse expiration date
            val expDateStr = response.userInfo.expDate
            val expDate = when {
                expDateStr == null -> null
                expDateStr.equals("Unlimited", ignoreCase = true) -> Long.MAX_VALUE
                expDateStr.equals("null", ignoreCase = true) -> null
                // Try as timestamp (seconds)
                expDateStr.toLongOrNull() != null -> expDateStr.toLong() * 1000
                // Try as Date String (yyyy-MM-dd, etc.) - simple fallback
                else -> {
                    try {
                        // formats: yyyy-MM-dd HH:mm:ss, yyyy-MM-dd, etc.
                        // For now, let's try a few common patterns or just use a generic parser if available.
                        // Since we don't have a heavy date library, let's try basic java.text.SimpleDateFormat
                        val formats = listOf(
                            "yyyy-MM-dd HH:mm:ss",
                            "yyyy-MM-dd"
                        )
                        var parsed: Long? = null
                        for (fmt in formats) {
                            try {
                                parsed = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault()).parse(expDateStr)?.time
                                if (parsed != null) break
                            } catch (_: Exception) {}
                        }
                        parsed
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            Result.success(
                Provider(
                    id = providerId,
                    name = "$username@${serverUrl.substringAfter("://").substringBefore("/")}",
                    type = ProviderType.XTREAM_CODES,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    maxConnections = response.userInfo.maxConnections.toIntOrNull() ?: 1,
                    expirationDate = expDate,
                    status = when (response.userInfo.status) {
                        "Active" -> ProviderStatus.ACTIVE
                        "Expired" -> ProviderStatus.EXPIRED
                        "Disabled" -> ProviderStatus.DISABLED
                        else -> ProviderStatus.UNKNOWN
                    }
                )
            )
        }
    } catch (e: Exception) {
        Result.error("Authentication failed: ${e.message}", e)
    }

    // ── Live TV ────────────────────────────────────────────────────

    override suspend fun getLiveCategories(): Result<List<Category>> = try {
        val categories = api.getLiveCategories(username, password)
        Result.success(categories.map { it.toDomain(ContentType.LIVE) })
    } catch (e: Exception) {
        Result.error("Failed to load live categories: ${e.message}", e)
    }

    override suspend fun getLiveStreams(categoryId: Long?): Result<List<Channel>> = try {
        val streams = api.getLiveStreams(
            username, password,
            categoryId = categoryId?.toString()
        )
        Result.success(streams.map { it.toChannel() })
    } catch (e: Exception) {
        Result.error("Failed to load live streams: ${e.message}", e)
    }

    // ── VOD ────────────────────────────────────────────────────────

    override suspend fun getVodCategories(): Result<List<Category>> = try {
        val categories = api.getVodCategories(username, password)
        Result.success(categories.map { it.toDomain(ContentType.MOVIE) })
    } catch (e: Exception) {
        Result.error("Failed to load VOD categories: ${e.message}", e)
    }

    override suspend fun getVodStreams(categoryId: Long?): Result<List<Movie>> = try {
        val streams = api.getVodStreams(
            username, password,
            categoryId = categoryId?.toString()
        )
        Result.success(streams.map { it.toMovie() })
    } catch (e: Exception) {
        Result.error("Failed to load VOD: ${e.message}", e)
    }

    override suspend fun getVodInfo(vodId: Long): Result<Movie> = try {
        val response = api.getVodInfo(username, password, vodId = vodId)
        val movieData = response.movieData
        val info = response.info

        if (movieData == null) {
            Result.error("Movie not found")
        } else {
            Result.success(
                Movie(
                    id = movieData.streamId,
                    name = movieData.name,
                    posterUrl = info?.movieImage,
                    backdropUrl = info?.backdropPath?.firstOrNull(),
                    categoryId = movieData.categoryId?.toLongOrNull(),
                    containerExtension = movieData.containerExtension,
                    plot = info?.plot,
                    cast = info?.cast,
                    director = info?.director,
                    genre = info?.genre,
                    releaseDate = info?.releaseDate,
                    duration = info?.duration,
                    durationSeconds = info?.durationSecs ?: 0,
                    rating = info?.rating?.toFloatOrNull() ?: 0f,
                    tmdbId = info?.tmdbId,
                    youtubeTrailer = info?.youtubeTrailer,
                    providerId = providerId,
                    streamUrl = buildMovieStreamUrl(movieData.streamId, movieData.containerExtension)
                )
            )
        }
    } catch (e: Exception) {
        Result.error("Failed to load movie details: ${e.message}", e)
    }

    // ── Series ─────────────────────────────────────────────────────

    override suspend fun getSeriesCategories(): Result<List<Category>> = try {
        val categories = api.getSeriesCategories(username, password)
        Result.success(categories.map { it.toDomain(ContentType.SERIES) })
    } catch (e: Exception) {
        Result.error("Failed to load series categories: ${e.message}", e)
    }

    override suspend fun getSeriesList(categoryId: Long?): Result<List<Series>> = try {
        val items = api.getSeriesList(
            username, password,
            categoryId = categoryId?.toString()
        )
        Result.success(items.map { it.toDomain() })
    } catch (e: Exception) {
        Result.error("Failed to load series: ${e.message}", e)
    }

    override suspend fun getSeriesInfo(seriesId: Long): Result<Series> = try {
        val response = api.getSeriesInfo(username, password, seriesId = seriesId)
        val info = response.info

        if (info == null) {
            Result.error("Series not found")
        } else {
            val seasons = response.episodes.map { (seasonNum, episodes) ->
                Season(
                    seasonNumber = seasonNum.toIntOrNull() ?: 0,
                    name = "Season $seasonNum",
                    coverUrl = response.seasons.find {
                        it.seasonNumber == (seasonNum.toIntOrNull() ?: 0)
                    }?.cover,
                    episodes = episodes.map { ep ->
                        Episode(
                            id = ep.id.toLongOrNull() ?: 0,
                            title = ep.title.ifBlank { ep.info?.name ?: "Episode ${ep.episodeNum}" },
                            episodeNumber = ep.episodeNum,
                            seasonNumber = ep.season,
                            containerExtension = ep.containerExtension,
                            coverUrl = ep.info?.movieImage,
                            plot = ep.info?.plot,
                            duration = ep.info?.duration,
                            durationSeconds = ep.info?.durationSecs ?: 0,
                            rating = ep.info?.rating?.toFloatOrNull() ?: 0f,
                            releaseDate = ep.info?.releaseDate,
                            seriesId = seriesId,
                            providerId = providerId,
                            streamUrl = buildSeriesStreamUrl(
                                ep.id.toLongOrNull() ?: 0,
                                ep.containerExtension
                            ),
                            isAdult = false,
                            isUserProtected = false
                        )
                    },
                    episodeCount = episodes.size
                )
            }.sortedBy { it.seasonNumber }

            Result.success(
                info.toDomain().copy(
                    seasons = seasons,
                    providerId = providerId
                )
            )
        }
    } catch (e: Exception) {
        Result.error("Failed to load series details: ${e.message}", e)
    }

    // ── EPG ────────────────────────────────────────────────────────

    override suspend fun getEpg(channelId: String): Result<List<Program>> = try {
        val streamId = channelId.toLongOrNull() ?: 0
        val response = api.getFullEpg(username, password, streamId = streamId)
        Result.success(response.epgListings.map { it.toDomain() })
    } catch (e: Exception) {
        Result.error("Failed to load EPG: ${e.message}", e)
    }

    override suspend fun getShortEpg(channelId: String, limit: Int): Result<List<Program>> = try {
        val streamId = channelId.toLongOrNull() ?: 0
        val response = api.getShortEpg(username, password, streamId = streamId, limit = limit)
        Result.success(response.epgListings.map { it.toDomain() })
    } catch (e: Exception) {
        Result.error("Failed to load EPG: ${e.message}", e)
    }

    // ── Stream URLs ────────────────────────────────────────────────

    override suspend fun buildStreamUrl(streamId: Long, containerExtension: String?): String {
        val baseUrl = serverUrl.trimEnd('/')
        return "$baseUrl/live/$username/$password/$streamId.ts"
    }

    private fun buildMovieStreamUrl(streamId: Long, containerExtension: String?): String {
        val baseUrl = serverUrl.trimEnd('/')
        val ext = containerExtension ?: "mp4"
        return "$baseUrl/movie/$username/$password/$streamId.$ext"
    }

    private fun buildSeriesStreamUrl(streamId: Long, containerExtension: String?): String {
        val baseUrl = serverUrl.trimEnd('/')
        val ext = containerExtension ?: "mp4"
        return "$baseUrl/series/$username/$password/$streamId.$ext"
    }

    override suspend fun buildCatchUpUrl(streamId: Long, start: Long, end: Long): String? {
        val baseUrl = serverUrl.trimEnd('/')
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.getDefault())
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC") // Xtream servers typically use UTC for EPG timeshifts
        val formattedStart = dateFormat.format(java.util.Date(start * 1000L))
        val durationMinutes = (end - start) / 60
        return "$baseUrl/timeshift/$username/$password/$durationMinutes/$formattedStart/$streamId.ts"
    }

    // ── Mappers ────────────────────────────────────────────────────
    
    private fun isAdultContent(name: String?): Boolean {
        if (name == null) return false
        val keywords = listOf("XXX", "Adult", "Porn", "Erotic", "18+")
        return keywords.any { name.contains(it, ignoreCase = true) }
    }

    private fun XtreamCategory.toDomain(type: ContentType) = Category(
        id = categoryId.toLongOrNull() ?: 0,
        name = categoryName,
        parentId = if (parentId > 0) parentId.toLong() else null,
        type = type,
        isAdult = isAdultContent(categoryName)
    )

    private fun XtreamStream.toChannel(): Channel {
        val isCatAdult = isAdultContent(categoryName)
        // Check group title as fallback if available, though usually categoryName is the group
        val isGroupAdult = false 
        
        return Channel(
            id = streamId,
            name = name,
            logoUrl = streamIcon,
            categoryId = categoryId?.toLongOrNull(),
            categoryName = categoryName,
            epgChannelId = epgChannelId,
            number = num,
            catchUpSupported = tvArchive == 1,
            catchUpDays = tvArchiveDuration ?: 0,
            providerId = providerId,
            streamUrl = "$serverUrl/live/$username/$password/$streamId.ts",
            isAdult = isCatAdult,
            isUserProtected = false,
            logicalGroupId = ChannelNormalizer.getLogicalGroupId(name, providerId)
        )
    }

    private fun XtreamStream.toMovie() = Movie(
        id = streamId,
        name = name,
        posterUrl = streamIcon,
        categoryId = categoryId?.toLongOrNull(),
        categoryName = categoryName,
        containerExtension = containerExtension,
        rating = rating5based?.toFloatOrNull() ?: 0f,
        providerId = providerId,
        streamUrl = buildMovieStreamUrl(streamId, containerExtension),
        isAdult = isAdultContent(categoryName),
        isUserProtected = false
    )

    private fun XtreamSeriesItem.toDomain() = Series(
        id = seriesId,
        name = name,
        posterUrl = cover,
        backdropUrl = backdropPath?.firstOrNull(),
        categoryId = categoryId?.toLongOrNull(),
        plot = plot,
        cast = cast,
        director = director,
        genre = genre,
        releaseDate = releaseDate,
        rating = rating5based?.toFloatOrNull() ?: rating?.toFloatOrNull() ?: 0f,
        youtubeTrailer = youtubeTrailer,
        episodeRunTime = episodeRunTime,
        lastModified = lastModified?.toLongOrNull() ?: 0L,
        providerId = providerId,
        isAdult = false, // Category name not on series item in Xtream
        isUserProtected = false
    )

    private fun XtreamEpgListing.toDomain(): Program {
        // Xtream sometimes base64-encodes title and description
        val decodedTitle = tryBase64Decode(title)
        val decodedDescription = tryBase64Decode(description)

        return Program(
            id = id.toLongOrNull() ?: 0,
            channelId = channelId,
            title = decodedTitle,
            description = decodedDescription,
            startTime = startTimestamp * 1000L,
            endTime = stopTimestamp * 1000L,
            lang = lang,
            hasArchive = hasArchive == 1,
            isNowPlaying = nowPlaying == 1
        )
    }

    private fun tryBase64Decode(value: String): String = try {
        if (value.isBlank()) value
        else {
            val decoded = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
            if (decoded.any { it.isLetterOrDigit() }) decoded else value
        }
    } catch (_: Exception) {
        value
    }
}
