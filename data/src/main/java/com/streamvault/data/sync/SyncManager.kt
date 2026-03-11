package com.streamvault.data.sync

import android.util.Log
import com.streamvault.data.local.dao.CategoryDao
import com.streamvault.data.local.dao.ChannelDao
import com.streamvault.data.local.dao.MovieDao
import com.streamvault.data.local.dao.ProviderDao
import com.streamvault.data.local.dao.SeriesDao
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.mapper.*
import com.streamvault.data.parser.M3uParser
import com.streamvault.data.remote.xtream.XtreamApiService
import com.streamvault.data.remote.xtream.XtreamProvider
import com.streamvault.data.security.CredentialCrypto
import com.streamvault.data.util.AdultContentClassifier
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.Provider
import com.streamvault.domain.model.ProviderType
import com.streamvault.domain.model.SyncState
import com.streamvault.domain.repository.EpgRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.streamvault.domain.repository.SyncMetadataRepository
import com.streamvault.domain.model.SyncMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncManager"

enum class SyncRepairSection {
    EPG,
    MOVIES,
    SERIES
}

/**
 * Centralised sync engine that owns the state machine and all data refresh logic.
 *
 * Formerly this logic was scattered inside `ProviderRepositoryImpl`. Moving it here
 * gives us:
 *  - A single source of truth for `SyncState`
 *  - A cancellable coroutine scope independent of ViewModel lifetimes
 *  - Stable, hash-based M3U entry IDs (no more index-based fragility)
 *  - Testable sync logic (can be unit-tested with fakes for all DAOs)
 */
@Singleton
class SyncManager @Inject constructor(
    private val providerDao: ProviderDao,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val categoryDao: CategoryDao,
    private val xtreamApiService: XtreamApiService,
    private val m3uParser: M3uParser,
    private val epgRepository: EpgRepository,
    private val okHttpClient: OkHttpClient,
    private val syncMetadataRepository: SyncMetadataRepository
) {
    private data class SyncOutcome(
        val partial: Boolean = false,
        val warnings: List<String> = emptyList()
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Perform a full data refresh for [providerId].
     * [onProgress] is an optional UI callback for fine-grained status strings.
     */
    suspend fun sync(
        providerId: Long,
        force: Boolean = false,
        onProgress: ((String) -> Unit)? = null
    ): com.streamvault.domain.model.Result<Unit> {
        val providerEntity = providerDao.getById(providerId)
            ?: return com.streamvault.domain.model.Result.error("Provider $providerId not found")

        val provider = providerEntity
            .copy(password = CredentialCrypto.decryptIfNeeded(providerEntity.password))
            .toDomain()
        _syncState.value = SyncState.Syncing("Starting…")

        return try {
            val outcome = when (provider.type) {
                ProviderType.XTREAM_CODES -> syncXtream(provider, force, onProgress)
                ProviderType.M3U -> syncM3u(provider, force, onProgress)
            }
            providerDao.updateSyncTime(providerId, System.currentTimeMillis())
            if (outcome.partial) {
                _syncState.value = SyncState.Partial(
                    message = "Sync completed with warnings",
                    warnings = outcome.warnings
                )
            } else {
                _syncState.value = SyncState.Success()
            }
            com.streamvault.domain.model.Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for provider $providerId: ${e.message}", e)
            _syncState.value = SyncState.Error(e.message ?: "Unknown error", e)
            com.streamvault.domain.model.Result.error(e.message ?: "Sync failed", e)
        }
    }

    /** Fire-and-forget variant — launches on the internal scope so callers don't need to await. */
    fun syncAsync(providerId: Long, force: Boolean = false, onProgress: ((String) -> Unit)? = null) {
        scope.launch { sync(providerId, force, onProgress) }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }

    suspend fun retrySection(
        providerId: Long,
        section: SyncRepairSection,
        onProgress: ((String) -> Unit)? = null
    ): com.streamvault.domain.model.Result<Unit> {
        val providerEntity = providerDao.getById(providerId)
            ?: return com.streamvault.domain.model.Result.error("Provider $providerId not found")

        val provider = providerEntity
            .copy(password = CredentialCrypto.decryptIfNeeded(providerEntity.password))
            .toDomain()

        return try {
            when (section) {
                SyncRepairSection.EPG -> syncEpgOnly(provider, onProgress)
                SyncRepairSection.MOVIES -> syncMoviesOnly(provider, onProgress)
                SyncRepairSection.SERIES -> syncSeriesOnly(provider, onProgress)
            }
            _syncState.value = SyncState.Success()
            com.streamvault.domain.model.Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Section retry failed for provider $providerId [$section]: ${e.message}", e)
            _syncState.value = SyncState.Error(e.message ?: "Retry failed", e)
            com.streamvault.domain.model.Result.error(e.message ?: "Retry failed", e)
        }
    }

    // ── Xtream sync ─────────────────────────────────────────────────

    private suspend fun syncXtream(
        provider: Provider,
        force: Boolean,
        onProgress: ((String) -> Unit)?
    ): SyncOutcome {
        val warnings = mutableListOf<String>()
        progress(onProgress, "Connecting to server…")
        val api = XtreamProvider(
            providerId = provider.id,
            api = xtreamApiService,
            serverUrl = provider.serverUrl,
            username = provider.username,
            password = provider.password
        )

        var metadata = syncMetadataRepository.getMetadata(provider.id) ?: SyncMetadata(provider.id)
        val now = System.currentTimeMillis()

        // Live (TTL 24h)
        if (force || !isCacheValid(metadata.lastLiveSync, TTL_24_HOURS, now)) {
            progress(onProgress, "Downloading Live TV…")
            val cats = retryTransient { api.getLiveCategories().getOrThrow("Live categories") }
            Log.d(TAG, "Saving ${cats.size} live categories")
            categoryDao.replaceAll(provider.id, "LIVE", cats.map { it.toEntity(provider.id) })

            val channels = retryTransient { api.getLiveStreams().getOrThrow("Live streams") }
            Log.d(TAG, "Saving ${channels.size} channels")
            channelDao.replaceAll(provider.id, channels.map { it.toEntity() })
            
            metadata = metadata.copy(lastLiveSync = now, liveCount = channels.size)
            syncMetadataRepository.updateMetadata(metadata)
        } else {
            Log.d(TAG, "Skipping Live TV sync (cache still valid)")
        }

        // VOD (TTL 24h)
        if (force || !isCacheValid(metadata.lastMovieSync, TTL_24_HOURS, now)) {
            progress(onProgress, "Downloading Movies…")
            val catsResult = runCatching { retryTransient { api.getVodCategories().getOrThrow("VOD categories") } }
            val cats = catsResult.getOrNull()
            if (cats != null) {
                Log.d(TAG, "Saving ${cats.size} VOD categories")
                categoryDao.replaceAll(provider.id, "MOVIE", cats.map { it.toEntity(provider.id) })
            } else {
                warnings.add("Movies categories sync failed")
            }
            
            val moviesResult = runCatching { retryTransient { api.getVodStreams().getOrThrow("VOD streams") } }
            val movies = moviesResult.getOrNull()
            if (movies != null) {
                Log.d(TAG, "Saving ${movies.size} movies")
                movieDao.replaceAll(provider.id, movies.map { it.toEntity() })
                metadata = metadata.copy(lastMovieSync = now, movieCount = movies.size)
                syncMetadataRepository.updateMetadata(metadata)
            } else {
                warnings.add("Movies streams sync failed")
            }
        } else {
            Log.d(TAG, "Skipping Movies sync (cache still valid)")
        }

        // Series (TTL 24h)
        if (force || !isCacheValid(metadata.lastSeriesSync, TTL_24_HOURS, now)) {
            progress(onProgress, "Downloading Series…")
            val catsResult = runCatching { retryTransient { api.getSeriesCategories().getOrThrow("Series categories") } }
            val cats = catsResult.getOrNull()
            if (cats != null) {
                Log.d(TAG, "Saving ${cats.size} series categories")
                categoryDao.replaceAll(provider.id, "SERIES", cats.map { it.toEntity(provider.id) })
            } else {
                warnings.add("Series categories sync failed")
            }
            
            val seriesResult = runCatching { retryTransient { api.getSeriesList().getOrThrow("Series list") } }
            val seriesList = seriesResult.getOrNull()
            if (seriesList != null) {
                Log.d(TAG, "Saving ${seriesList.size} series")
                seriesDao.replaceAll(provider.id, seriesList.map { it.toEntity() })
                metadata = metadata.copy(lastSeriesSync = now, seriesCount = seriesList.size)
                syncMetadataRepository.updateMetadata(metadata)
            } else {
                warnings.add("Series list sync failed")
            }
        } else {
            Log.d(TAG, "Skipping Series sync (cache still valid)")
        }

        // EPG (TTL 6h)
        if (force || !isCacheValid(metadata.lastEpgSync, TTL_6_HOURS, now)) {
            try {
                progress(onProgress, "Downloading EPG…")
                val base = provider.serverUrl.trimEnd('/')
                val xmltvUrl = provider.epgUrl.ifBlank { "$base/xmltv.php?username=${provider.username}&password=${provider.password}" }
                retryTransient { epgRepository.refreshEpg(provider.id, xmltvUrl) }
                
                metadata = metadata.copy(lastEpgSync = now)
                syncMetadataRepository.updateMetadata(metadata)
            } catch (e: Exception) {
                Log.e(TAG, "EPG sync failed (non-fatal): ${e.message}")
                warnings.add("EPG sync failed")
            }
        } else {
            Log.d(TAG, "Skipping EPG sync (cache still valid)")
        }

        return if (warnings.isEmpty()) SyncOutcome() else SyncOutcome(partial = true, warnings = warnings)
    }

    // ── M3U sync ────────────────────────────────────────────────────

    private suspend fun syncM3u(
        provider: Provider,
        force: Boolean,
        onProgress: ((String) -> Unit)?
    ): SyncOutcome {
        val warnings = mutableListOf<String>()
        var metadata = syncMetadataRepository.getMetadata(provider.id) ?: SyncMetadata(provider.id)
        val now = System.currentTimeMillis()

        // M3U must be fully re-downloaded (no granular deltas), so we use lastLiveSync
        // as the general indicator for the playlist payload TTL (24h)
        if (force || !isCacheValid(metadata.lastLiveSync, TTL_24_HOURS, now)) {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Starting M3U refresh for ${provider.name}")
                progress(onProgress, "Downloading Playlist…")
                val urlStr = provider.m3uUrl.ifBlank { provider.serverUrl }
                val inputStream = if (urlStr.startsWith("file://")) {
                    java.io.File(java.net.URI(urlStr)).inputStream()
                } else {
                    val payload = retryTransient {
                        okHttpClient.newCall(Request.Builder().url(urlStr).build()).execute().use { response ->
                            if (!response.isSuccessful) {
                                if (response.code in 500..599 || response.code == 429) {
                                    throw IOException("Transient HTTP ${response.code}")
                                }
                                throw IllegalStateException("Failed to download M3U: HTTP ${response.code}")
                            }
                            response.body?.bytes() ?: throw IllegalStateException("Empty M3U response")
                        }
                    }
                    payload.inputStream()
                }

                progress(onProgress, "Parsing Playlist…")
                val parseResult = inputStream.use { m3uParser.parse(it) }
                val entries = parseResult.entries
                val header = parseResult.header
                Log.d(TAG, "Parsed ${entries.size} M3U entries")

                if (entries.isEmpty()) {
                    throw IllegalStateException("Playlist is empty or contains no supported entries")
                }

                // Auto-update EPG URL from header if missing
                if (provider.epgUrl.isBlank() && !header.tvgUrl.isNullOrBlank()) {
                    Log.d(TAG, "Auto-discovered EPG URL from header: ${redactUrlForLogs(header.tvgUrl)}")
                    providerDao.updateEpgUrl(provider.id, header.tvgUrl)
                }

                val liveEntries = entries.filter { !isVodEntry(it) }
                val vodEntries = entries.filter { isVodEntry(it) }
                if (liveEntries.isEmpty() && vodEntries.isEmpty()) {
                    throw IllegalStateException("Playlist contains no playable live or VOD entries")
                }

                // ── Categories ──────────────────────────────────────
                val liveGroups = liveEntries.map { it.groupTitle }.distinct()
                val vodGroups = vodEntries.map { it.groupTitle }.distinct()

                val liveCategories = liveGroups.mapIndexed { i, name ->
                    CategoryEntity(categoryId = (i + 1).toLong(), name = name,
                        parentId = 0, type = "LIVE", providerId = provider.id,
                        isAdult = AdultContentClassifier.isAdultCategoryName(name))
                }
                val vodCategories = vodGroups.mapIndexed { i, name ->
                    CategoryEntity(categoryId = (i + 10_000).toLong(), name = name,
                        parentId = 0, type = "MOVIE", providerId = provider.id,
                        isAdult = AdultContentClassifier.isAdultCategoryName(name))
                }

                progress(onProgress, "Saving Channels…")
                categoryDao.replaceAll(provider.id, "LIVE", liveCategories)
                categoryDao.replaceAll(provider.id, "MOVIE", vodCategories)

                val liveCategoryMap = liveGroups.withIndex().associate { (i, n) -> n to (i + 1).toLong() }
                val vodCategoryMap  = vodGroups.withIndex().associate { (i, n) -> n to (i + 10_000).toLong() }

                // ── Channels with stable hash IDs ───────────────────
                val channels = liveEntries.map { entry ->
                    val stableStreamId = stableId(provider.id, entry.tvgId, entry.url)
                    Channel(
                        id = 0,
                        name = entry.name,
                        logoUrl = entry.tvgLogo,
                        groupTitle = entry.groupTitle,
                        categoryId = liveCategoryMap[entry.groupTitle],
                        categoryName = entry.groupTitle,
                        epgChannelId = entry.tvgId ?: entry.tvgName,
                        number = entry.tvgChno ?: 0,
                        streamUrl = entry.url,
                        catchUpSupported = entry.catchUp != null,
                        catchUpDays = entry.catchUpDays ?: 0,
                        catchUpSource = entry.catchUpSource,
                        providerId = provider.id,
                        isAdult = AdultContentClassifier.isAdultCategoryName(entry.groupTitle),
                        streamId = stableStreamId
                    ).toEntity()
                }
                Log.d(TAG, "Saving ${channels.size} channels")
                channelDao.replaceAll(provider.id, channels)

                // ── Movies with stable hash IDs ─────────────────────
                progress(onProgress, "Saving Movies…")
                val movies = vodEntries.map { entry ->
                    val stableStreamId = stableId(provider.id, entry.tvgId, entry.url)
                    Movie(
                        id = 0,
                        name = entry.name,
                        posterUrl = entry.tvgLogo,
                        categoryId = vodCategoryMap[entry.groupTitle],
                        categoryName = entry.groupTitle,
                        streamUrl = entry.url,
                        providerId = provider.id,
                        rating = entry.rating?.toFloatOrNull() ?: 0f,
                        year = entry.year,
                        genre = entry.genre,
                        isAdult = AdultContentClassifier.isAdultCategoryName(entry.groupTitle),
                        streamId = stableStreamId
                    ).toEntity()
                }
                Log.d(TAG, "Saving ${movies.size} movies")
                movieDao.replaceAll(provider.id, movies)
                Log.d(TAG, "M3U refresh complete")

                metadata = metadata.copy(
                    lastLiveSync = now, lastMovieSync = now, lastSeriesSync = now, // treat as single payload
                    liveCount = channels.size, movieCount = movies.size
                )
                syncMetadataRepository.updateMetadata(metadata)
            }
        } else {
            Log.d(TAG, "Skipping M3U playlist sync (cache still valid)")
        }

        // Try EPG refresh if standard M3U provider linked an EPG URL or auto-discovered one
        val currentEpgUrl = providerDao.getById(provider.id)?.epgUrl ?: provider.epgUrl
        if (!currentEpgUrl.isNullOrBlank()) {
            if (force || !isCacheValid(metadata.lastEpgSync, TTL_6_HOURS, now)) {
                try {
                    progress(onProgress, "Downloading EPG…")
                    retryTransient { epgRepository.refreshEpg(provider.id, currentEpgUrl) }
                    metadata = metadata.copy(lastEpgSync = now)
                    syncMetadataRepository.updateMetadata(metadata)
                } catch (e: Exception) {
                    Log.e(TAG, "EPG sync failed (non-fatal): ${e.message}")
                    warnings.add("EPG sync failed")
                }
            } else {
                Log.d(TAG, "Skipping EPG sync (cache still valid)")
            }
        }

        return if (warnings.isEmpty()) SyncOutcome() else SyncOutcome(partial = true, warnings = warnings)
    }

    private suspend fun syncEpgOnly(
        provider: Provider,
        onProgress: ((String) -> Unit)?
    ) {
        progress(onProgress, "Retrying EPG…")
        val epgUrl = when (provider.type) {
            ProviderType.XTREAM_CODES -> {
                val base = provider.serverUrl.trimEnd('/')
                provider.epgUrl.ifBlank { "$base/xmltv.php?username=${provider.username}&password=${provider.password}" }
            }
            ProviderType.M3U -> {
                providerDao.getById(provider.id)?.epgUrl ?: provider.epgUrl
            }
        }
        if (epgUrl.isBlank()) {
            throw IllegalStateException("No EPG URL configured for this provider")
        }
        retryTransient { epgRepository.refreshEpg(provider.id, epgUrl) }
        val now = System.currentTimeMillis()
        val metadata = (syncMetadataRepository.getMetadata(provider.id) ?: SyncMetadata(provider.id))
            .copy(lastEpgSync = now)
        syncMetadataRepository.updateMetadata(metadata)
    }

    private suspend fun syncMoviesOnly(
        provider: Provider,
        onProgress: ((String) -> Unit)?
    ) {
        val now = System.currentTimeMillis()
        when (provider.type) {
            ProviderType.XTREAM_CODES -> {
                progress(onProgress, "Retrying Movies…")
                val api = XtreamProvider(
                    providerId = provider.id,
                    api = xtreamApiService,
                    serverUrl = provider.serverUrl,
                    username = provider.username,
                    password = provider.password
                )
                val cats = retryTransient { api.getVodCategories().getOrThrow("VOD categories") }
                categoryDao.replaceAll(provider.id, "MOVIE", cats.map { it.toEntity(provider.id) })

                val movies = retryTransient { api.getVodStreams().getOrThrow("VOD streams") }
                movieDao.replaceAll(provider.id, movies.map { it.toEntity() })

                val metadata = (syncMetadataRepository.getMetadata(provider.id) ?: SyncMetadata(provider.id))
                    .copy(lastMovieSync = now, movieCount = movies.size)
                syncMetadataRepository.updateMetadata(metadata)
            }
            ProviderType.M3U -> {
                progress(onProgress, "Retrying Movies…")
                withContext(Dispatchers.IO) {
                    val parseResult = loadM3uParseResult(provider)
                    val entries = parseResult.entries
                    val vodEntries = entries.filter { isVodEntry(it) }
                    if (vodEntries.isEmpty()) {
                        throw IllegalStateException("Playlist contains no movie entries")
                    }
                    val vodGroups = vodEntries.map { it.groupTitle }.distinct()
                    val vodCategories = vodGroups.mapIndexed { i, name ->
                        CategoryEntity(
                            categoryId = (i + 10_000).toLong(),
                            name = name,
                            parentId = 0,
                            type = "MOVIE",
                            providerId = provider.id,
                            isAdult = AdultContentClassifier.isAdultCategoryName(name)
                        )
                    }
                    categoryDao.replaceAll(provider.id, "MOVIE", vodCategories)
                    val vodCategoryMap = vodGroups.withIndex().associate { (i, n) -> n to (i + 10_000).toLong() }
                    val movies = vodEntries.map { entry ->
                        val stableStreamId = stableId(provider.id, entry.tvgId, entry.url)
                        Movie(
                            id = 0,
                            name = entry.name,
                            posterUrl = entry.tvgLogo,
                            categoryId = vodCategoryMap[entry.groupTitle],
                            categoryName = entry.groupTitle,
                            streamUrl = entry.url,
                            providerId = provider.id,
                            rating = entry.rating?.toFloatOrNull() ?: 0f,
                            year = entry.year,
                            genre = entry.genre,
                            isAdult = AdultContentClassifier.isAdultCategoryName(entry.groupTitle),
                            streamId = stableStreamId
                        ).toEntity()
                    }
                    movieDao.replaceAll(provider.id, movies)
                    val metadata = (syncMetadataRepository.getMetadata(provider.id) ?: SyncMetadata(provider.id))
                        .copy(lastMovieSync = now, movieCount = movies.size)
                    syncMetadataRepository.updateMetadata(metadata)
                }
            }
        }
    }

    private suspend fun syncSeriesOnly(
        provider: Provider,
        onProgress: ((String) -> Unit)?
    ) {
        if (provider.type != ProviderType.XTREAM_CODES) {
            throw IllegalStateException("Series retry is available only for Xtream providers")
        }
        progress(onProgress, "Retrying Series…")
        val api = XtreamProvider(
            providerId = provider.id,
            api = xtreamApiService,
            serverUrl = provider.serverUrl,
            username = provider.username,
            password = provider.password
        )

        val cats = retryTransient { api.getSeriesCategories().getOrThrow("Series categories") }
        categoryDao.replaceAll(provider.id, "SERIES", cats.map { it.toEntity(provider.id) })

        val seriesList = retryTransient { api.getSeriesList().getOrThrow("Series list") }
        seriesDao.replaceAll(provider.id, seriesList.map { it.toEntity() })

        val now = System.currentTimeMillis()
        val metadata = (syncMetadataRepository.getMetadata(provider.id) ?: SyncMetadata(provider.id))
            .copy(lastSeriesSync = now, seriesCount = seriesList.size)
        syncMetadataRepository.updateMetadata(metadata)
    }

    private fun loadM3uParseResult(provider: Provider): M3uParser.ParseResult {
        val urlStr = provider.m3uUrl.ifBlank { provider.serverUrl }
        val inputStream = if (urlStr.startsWith("file://")) {
            java.io.File(java.net.URI(urlStr)).inputStream()
        } else {
            val payload = okHttpClient.newCall(Request.Builder().url(urlStr).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Failed to download M3U: HTTP ${response.code}")
                }
                response.body?.bytes() ?: throw IllegalStateException("Empty M3U response")
            }
            payload.inputStream()
        }
        return inputStream.use { m3uParser.parse(it) }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    /**
     * Generate a stable, collision-resistant ID for an M3U entry.
     *
     * Priority:
     *  1. tvg-id  (explicit EPG key — most stable)
     *  2. SHA-256(providerId + streamUrl)  (URL changes invalidate the record — acceptable)
     *
     * This replaces the previous `index.toLong() + offset` scheme which caused
     * data corruption whenever the playlist order changed.
     */
    private fun stableId(providerId: Long, tvgId: String?, url: String): Long {
        if (!tvgId.isNullOrBlank()) {
            // Hash the tvgId to fit in a Long
            return hashToLong("$providerId:tvg:$tvgId")
        }
        return hashToLong("$providerId:url:$url")
    }

    private fun hashToLong(input: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        // Take first 8 bytes and fold into a Long (masks sign bit to keep positive)
        var result = 0L
        for (i in 0 until 8) {
            result = (result shl 8) or (digest[i].toLong() and 0xFF)
        }
        return result and Long.MAX_VALUE // ensure positive
    }

    /**
     * Heuristic to distinguish VOD (movie) entries from live streams in M3U playlists.
     * Consolidated here as the single source of truth (previously duplicated).
     */
    internal fun isVodEntry(entry: M3uParser.M3uEntry): Boolean {
        val url = entry.url.lowercase()
        val group = entry.groupTitle.lowercase()
        return url.endsWith(".mp4") ||
                url.endsWith(".mkv") ||
                url.endsWith(".avi") ||
                url.contains("/movie/") ||
                group.contains("movie") ||
                group.contains("vod") ||
                group.contains("film")
    }

    private fun isCacheValid(lastSync: Long, ttlMillis: Long, now: Long = System.currentTimeMillis()): Boolean {
        // If lastSync is 0, cache is invalid. If now - lastSync < ttl, cache is valid.
        return lastSync > 0 && (now - lastSync) < ttlMillis
    }

    private fun progress(callback: ((String) -> Unit)?, message: String) {
        _syncState.value = SyncState.Syncing(message)
        callback?.invoke(message)
    }

    private fun redactUrlForLogs(url: String?): String {
        if (url.isNullOrBlank()) return "<empty>"
        return runCatching {
            val parsed = java.net.URI(url)
            val scheme = parsed.scheme ?: "http"
            val host = parsed.host ?: return@runCatching "<redacted>"
            val path = parsed.path.orEmpty()
            "$scheme://$host$path"
        }.getOrDefault("<redacted>")
    }

    private suspend fun <T> retryTransient(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 700L,
        block: suspend () -> T
    ): T {
        var attempt = 0
        var delayMs = initialDelayMs
        var lastError: Throwable? = null

        while (attempt < maxAttempts) {
            try {
                return block()
            } catch (t: Throwable) {
                lastError = t
                attempt++
                if (attempt >= maxAttempts || !isRetryable(t)) {
                    throw t
                }
                delay(delayMs)
                delayMs *= 2
            }
        }

        throw lastError ?: IllegalStateException("Unknown sync retry failure")
    }

    private fun isRetryable(error: Throwable): Boolean {
        if (error is IOException) return true

        val message = error.message.orEmpty().lowercase()
        return message.contains("timeout") ||
            message.contains("timed out") ||
            message.contains("unable to resolve host") ||
            message.contains("connection reset") ||
            message.contains("connect") ||
            message.contains("network")
    }

    companion object {
        const val TTL_24_HOURS = 24L * 60 * 60 * 1000L
        const val TTL_6_HOURS = 6L * 60 * 60 * 1000L
    }

    // Extension to convert Result<T> to T-or-throw for mandatory resources
    private fun <T> com.streamvault.domain.model.Result<T>.getOrThrow(resourceName: String): T {
        return when (this) {
            is com.streamvault.domain.model.Result.Success -> data
            is com.streamvault.domain.model.Result.Error ->
                throw Exception("Failed to fetch $resourceName: $message")
            is com.streamvault.domain.model.Result.Loading ->
                throw Exception("Unexpected loading state for $resourceName")
        }
    }
}
