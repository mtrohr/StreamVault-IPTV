package com.streamvault.data.repository

import com.streamvault.data.local.dao.*
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.mapper.*
import com.streamvault.data.parser.M3uParser
import com.streamvault.data.remote.xtream.XtreamApiService
import com.streamvault.data.remote.xtream.XtreamProvider
import com.streamvault.domain.model.*
import com.streamvault.domain.provider.IptvProvider
import com.streamvault.domain.repository.EpgRepository
import com.streamvault.domain.repository.ProviderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepositoryImpl @Inject constructor(
    private val providerDao: ProviderDao,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val categoryDao: CategoryDao,
    private val xtreamApiService: XtreamApiService,
    private val m3uParser: M3uParser,
    private val epgRepository: EpgRepository,
    private val okHttpClient: OkHttpClient
) : ProviderRepository {

    override fun getProviders(): Flow<List<Provider>> =
        providerDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getActiveProvider(): Flow<Provider?> =
        providerDao.getActive().map { it?.toDomain() }

    override suspend fun getProvider(id: Long): Provider? =
        providerDao.getById(id)?.toDomain()

    override suspend fun addProvider(provider: Provider): Result<Long> = try {
        val id = providerDao.insert(provider.toEntity())
        Result.success(id)
    } catch (e: Exception) {
        Result.error("Failed to add provider: ${e.message}", e)
    }

    override suspend fun updateProvider(provider: Provider): Result<Unit> = try {
        providerDao.update(provider.toEntity())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error("Failed to update provider: ${e.message}", e)
    }

    override suspend fun deleteProvider(id: Long): Result<Unit> = try {
        channelDao.deleteByProvider(id)
        movieDao.deleteByProvider(id)
        seriesDao.deleteByProvider(id)
        categoryDao.deleteByProviderAndType(id, "LIVE")
        categoryDao.deleteByProviderAndType(id, "MOVIE")
        categoryDao.deleteByProviderAndType(id, "SERIES")
        providerDao.delete(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error("Failed to delete provider: ${e.message}", e)
    }

    override suspend fun setActiveProvider(id: Long): Result<Unit> = try {
        providerDao.deactivateAll()
        providerDao.activate(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error("Failed to set active provider: ${e.message}", e)
    }

    override suspend fun loginXtream(
        serverUrl: String,
        username: String,
        password: String,
        name: String
    ): Result<Provider> {
        val provider = createXtreamProvider(0, serverUrl, username, password)
        return when (val authResult = provider.authenticate()) {
            is Result.Success -> {
                // Use the provided name, or fallback to server's name if empty
                val providerData = authResult.data.copy(
                    name = name.ifBlank { authResult.data.name }
                )
                val id = providerDao.insert(providerData.toEntity())
                providerDao.deactivateAll()
                providerDao.activate(id)
                val savedProvider = providerData.copy(id = id)
                // Auto-refresh: fetch all channels, movies, series immediately
                try {
                    refreshXtreamData(savedProvider)
                    providerDao.updateSyncTime(id, System.currentTimeMillis())
                } catch (_: Exception) {
                    // Provider is added even if initial refresh fails
                }
                Result.success(savedProvider)
            }
            is Result.Error -> Result.error(authResult.message, authResult.exception)
            is Result.Loading -> Result.error("Unexpected loading state")
        }
    }

    override suspend fun validateM3u(url: String, name: String): Result<Provider> = try {
        // For M3U, we create a provider entry and validate the URL is reachable
        val providerName = name.ifBlank { 
            url.substringAfterLast("/").substringBefore("?").ifBlank { "M3U Playlist" } 
        }
        
        val provider = Provider(
            name = providerName,
            type = ProviderType.M3U,
            serverUrl = url,
            m3uUrl = url,
            status = ProviderStatus.ACTIVE
        )
        val id = providerDao.insert(provider.toEntity())
        providerDao.deactivateAll()
        providerDao.activate(id)
        val savedProvider = provider.copy(id = id)
        // Auto-refresh: download and parse M3U immediately
        try {
            refreshM3uData(savedProvider)
            providerDao.updateSyncTime(id, System.currentTimeMillis())
        } catch (_: Exception) {
            // Provider is added even if initial parsing fails
        }
        Result.success(savedProvider)
    } catch (e: Exception) {
        Result.error("Failed to add M3U provider: ${e.message}", e)
    }

    override suspend fun refreshProviderData(providerId: Long): Result<Unit> = try {
        val providerEntity = providerDao.getById(providerId)
            ?: return Result.error("Provider not found")
        val provider = providerEntity.toDomain()

        when (provider.type) {
            ProviderType.XTREAM_CODES -> refreshXtreamData(provider)
            ProviderType.M3U -> refreshM3uData(provider)
        }

        providerDao.updateSyncTime(providerId, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error("Failed to refresh provider data: ${e.message}", e)
    }

    private suspend fun refreshXtreamData(provider: Provider) {
        val xtreamProvider = createXtreamProvider(
            provider.id, provider.serverUrl, provider.username, provider.password
        )

        // Refresh live categories & channels
        xtreamProvider.getLiveCategories().getOrNull()?.let { categories ->
            categoryDao.replaceAll(
                provider.id, "LIVE",
                categories.map { it.toEntity(provider.id) }
            )
        }
        xtreamProvider.getLiveStreams().getOrNull()?.let { channels ->
            channelDao.replaceAll(provider.id, channels.map { it.toEntity() })
        }

        // Refresh VOD categories & movies
        xtreamProvider.getVodCategories().getOrNull()?.let { categories ->
            categoryDao.replaceAll(
                provider.id, "MOVIE",
                categories.map { it.toEntity(provider.id) }
            )
        }
        xtreamProvider.getVodStreams().getOrNull()?.let { movies ->
            movieDao.replaceAll(provider.id, movies.map { it.toEntity() })
        }

        // Refresh series categories & series
        xtreamProvider.getSeriesCategories().getOrNull()?.let { categories ->
            categoryDao.replaceAll(
                provider.id, "SERIES",
                categories.map { it.toEntity(provider.id) }
            )
        }
        xtreamProvider.getSeriesList().getOrNull()?.let { seriesList ->
            seriesDao.replaceAll(provider.id, seriesList.map { it.toEntity() })
        }

        // Refresh EPG (XMLTV)
        // Construct XMLTV URL: http://server:port/xmltv.php?username=...&password=...
        try {
            val serverUrl = provider.serverUrl.trimEnd('/')
            val xmltvUrl = "$serverUrl/xmltv.php?username=${provider.username}&password=${provider.password}"
            epgRepository.refreshEpg(provider.id, xmltvUrl)
        } catch (e: Exception) {
            // Log error but don't fail the whole refresh if EPG fails
            e.printStackTrace()
        }
    }

    private suspend fun refreshM3uData(provider: Provider) = withContext(Dispatchers.IO) {
        val m3uUrl = provider.m3uUrl.ifBlank { provider.serverUrl }
        val request = Request.Builder().url(m3uUrl).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to download M3U: HTTP ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty M3U response")

        val entries = body.byteStream().use { inputStream ->
            m3uParser.parse(inputStream)
        }

        // Separate live channels vs VOD (movies)
        val liveEntries = entries.filter { !isVodEntry(it) }
        val vodEntries = entries.filter { isVodEntry(it) }

        // Build categories from group titles
        val liveGroups = liveEntries.map { it.groupTitle }.distinct()
        val vodGroups = vodEntries.map { it.groupTitle }.distinct()

        // Insert live categories
        val liveCategories = liveGroups.mapIndexed { index, name ->
            CategoryEntity(
                categoryId = (index + 1).toLong(),
                name = name,
                parentId = 0,
                type = "LIVE",
                providerId = provider.id
            )
        }
        categoryDao.replaceAll(provider.id, "LIVE", liveCategories)

        // Insert VOD categories
        val vodCategories = vodGroups.mapIndexed { index, name ->
            CategoryEntity(
                categoryId = (index + 10000).toLong(),
                name = name,
                parentId = 0,
                type = "MOVIE",
                providerId = provider.id
            )
        }
        categoryDao.replaceAll(provider.id, "MOVIE", vodCategories)

        // Build category lookup maps
        val liveCategoryMap = liveGroups.withIndex().associate { (i, name) -> name to (i + 1).toLong() }
        val vodCategoryMap = vodGroups.withIndex().associate { (i, name) -> name to (i + 10000).toLong() }

        // Insert live channels
        val channels = liveEntries.mapIndexed { index, entry ->
            Channel(
                id = index.toLong() + 1,
                name = entry.name,
                logoUrl = entry.tvgLogo,
                groupTitle = entry.groupTitle,
                categoryId = liveCategoryMap[entry.groupTitle],
                categoryName = entry.groupTitle,
                epgChannelId = entry.tvgId ?: entry.tvgName,
                number = entry.tvgChno ?: (index + 1),
                streamUrl = entry.url,
                catchUpSupported = entry.catchUp != null,
                providerId = provider.id
            ).toEntity()
        }
        channelDao.replaceAll(provider.id, channels)

        // Insert movies
        val movies = vodEntries.mapIndexed { index, entry ->
            Movie(
                id = index.toLong() + 100000,
                name = entry.name,
                posterUrl = entry.tvgLogo,
                categoryId = vodCategoryMap[entry.groupTitle],
                categoryName = entry.groupTitle,
                streamUrl = entry.url,
                providerId = provider.id
            ).toEntity()
        }
        movieDao.replaceAll(provider.id, movies)
    }

    private fun isVodEntry(entry: M3uParser.M3uEntry): Boolean {
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

    fun createXtreamProvider(
        providerId: Long,
        serverUrl: String,
        username: String,
        password: String
    ): IptvProvider = XtreamProvider(
        providerId = providerId,
        api = xtreamApiService,
        serverUrl = serverUrl,
        username = username,
        password = password
    )
}
