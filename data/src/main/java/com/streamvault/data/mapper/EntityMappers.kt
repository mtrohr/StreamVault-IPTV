package com.streamvault.data.mapper

import com.streamvault.data.local.entity.*
import com.streamvault.domain.model.*

// ── Provider ───────────────────────────────────────────────────────

fun ProviderEntity.toDomain() = Provider(
    id = id,
    name = name,
    type = ProviderType.valueOf(type),
    serverUrl = serverUrl,
    username = username,
    password = password,
    m3uUrl = m3uUrl,
    epgUrl = epgUrl,
    isActive = isActive,
    maxConnections = maxConnections,
    expirationDate = expirationDate,
    status = try { ProviderStatus.valueOf(status) } catch (_: Exception) { ProviderStatus.UNKNOWN },
    lastSyncedAt = lastSyncedAt,
    createdAt = createdAt
)

fun Provider.toEntity() = ProviderEntity(
    id = id,
    name = name,
    type = type.name,
    serverUrl = serverUrl,
    username = username,
    password = password,
    m3uUrl = m3uUrl,
    epgUrl = epgUrl,
    isActive = isActive,
    maxConnections = maxConnections,
    expirationDate = expirationDate,
    status = status.name,
    lastSyncedAt = lastSyncedAt,
    createdAt = createdAt
)

// ── Channel ────────────────────────────────────────────────────────

fun ChannelEntity.toDomain() = Channel(
    id = id,
    name = name,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    categoryId = categoryId,
    categoryName = categoryName,
    streamUrl = streamUrl,
    epgChannelId = epgChannelId,
    number = number,
    catchUpSupported = catchUpSupported,
    catchUpDays = catchUpDays,
    providerId = providerId
)

fun Channel.toEntity() = ChannelEntity(
    id = id,
    streamId = id,
    name = name,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    categoryId = categoryId,
    categoryName = categoryName,
    streamUrl = streamUrl,
    epgChannelId = epgChannelId,
    number = number,
    catchUpSupported = catchUpSupported,
    catchUpDays = catchUpDays,
    providerId = providerId
)

// ── Movie ──────────────────────────────────────────────────────────

fun MovieEntity.toDomain() = Movie(
    id = id,
    name = name,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    categoryId = categoryId,
    categoryName = categoryName,
    streamUrl = streamUrl,
    containerExtension = containerExtension,
    plot = plot,
    cast = cast,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    duration = duration,
    durationSeconds = durationSeconds,
    rating = rating,
    year = year,
    tmdbId = tmdbId,
    youtubeTrailer = youtubeTrailer,
    providerId = providerId,
    watchProgress = watchProgress,
    lastWatchedAt = lastWatchedAt
)

fun Movie.toEntity() = MovieEntity(
    id = id,
    streamId = id,
    name = name,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    categoryId = categoryId,
    categoryName = categoryName,
    streamUrl = streamUrl,
    containerExtension = containerExtension,
    plot = plot,
    cast = cast,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    duration = duration,
    durationSeconds = durationSeconds,
    rating = rating,
    year = year,
    tmdbId = tmdbId,
    youtubeTrailer = youtubeTrailer,
    providerId = providerId,
    watchProgress = watchProgress,
    lastWatchedAt = lastWatchedAt
)

// ── Series ─────────────────────────────────────────────────────────

fun SeriesEntity.toDomain() = Series(
    id = id,
    name = name,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    categoryId = categoryId,
    categoryName = categoryName,
    plot = plot,
    cast = cast,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    rating = rating,
    tmdbId = tmdbId,
    youtubeTrailer = youtubeTrailer,
    episodeRunTime = episodeRunTime,
    lastModified = lastModified,
    providerId = providerId
)

fun Series.toEntity() = SeriesEntity(
    id = id,
    seriesId = id,
    name = name,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    categoryId = categoryId,
    categoryName = categoryName,
    plot = plot,
    cast = cast,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    rating = rating,
    tmdbId = tmdbId,
    youtubeTrailer = youtubeTrailer,
    episodeRunTime = episodeRunTime,
    lastModified = lastModified,
    providerId = providerId
)

// ── Episode ────────────────────────────────────────────────────────

fun EpisodeEntity.toDomain() = Episode(
    id = id,
    title = title,
    episodeNumber = episodeNumber,
    seasonNumber = seasonNumber,
    streamUrl = streamUrl,
    containerExtension = containerExtension,
    coverUrl = coverUrl,
    plot = plot,
    duration = duration,
    durationSeconds = durationSeconds,
    rating = rating,
    releaseDate = releaseDate,
    seriesId = seriesId,
    providerId = providerId,
    watchProgress = watchProgress,
    lastWatchedAt = lastWatchedAt
)

fun Episode.toEntity() = EpisodeEntity(
    id = id,
    episodeId = id,
    title = title,
    episodeNumber = episodeNumber,
    seasonNumber = seasonNumber,
    streamUrl = streamUrl,
    containerExtension = containerExtension,
    coverUrl = coverUrl,
    plot = plot,
    duration = duration,
    durationSeconds = durationSeconds,
    rating = rating,
    releaseDate = releaseDate,
    seriesId = seriesId,
    providerId = providerId,
    watchProgress = watchProgress,
    lastWatchedAt = lastWatchedAt
)

// ── Category ───────────────────────────────────────────────────────

fun CategoryEntity.toDomain() = com.streamvault.domain.model.Category(
    id = categoryId,
    name = name,
    parentId = parentId,
    type = try { ContentType.valueOf(type) } catch (_: Exception) { ContentType.LIVE }
)

fun com.streamvault.domain.model.Category.toEntity(providerId: Long) = CategoryEntity(
    categoryId = id,
    name = name,
    parentId = parentId,
    type = type.name,
    providerId = providerId
)

// ── Program ────────────────────────────────────────────────────────

fun ProgramEntity.toDomain() = Program(
    id = id,
    channelId = channelId,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    lang = lang,
    hasArchive = hasArchive
)

fun Program.toEntity() = ProgramEntity(
    id = id,
    channelId = channelId,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    lang = lang,
    hasArchive = hasArchive
)

// ── Favorite ───────────────────────────────────────────────────────

fun FavoriteEntity.toDomain() = Favorite(
    id = id,
    contentId = contentId,
    contentType = try { ContentType.valueOf(contentType) } catch (_: Exception) { ContentType.LIVE },
    position = position,
    groupId = groupId,
    addedAt = addedAt
)

fun Favorite.toEntity() = FavoriteEntity(
    id = id,
    contentId = contentId,
    contentType = contentType.name,
    position = position,
    groupId = groupId,
    addedAt = addedAt
)

// ── Virtual Group ──────────────────────────────────────────────────

fun VirtualGroupEntity.toDomain() = VirtualGroup(
    id = id,
    name = name,
    iconEmoji = iconEmoji,
    position = position,
    createdAt = createdAt
)

fun VirtualGroup.toEntity() = VirtualGroupEntity(
    id = id,
    name = name,
    iconEmoji = iconEmoji,
    position = position,
    createdAt = createdAt
)
