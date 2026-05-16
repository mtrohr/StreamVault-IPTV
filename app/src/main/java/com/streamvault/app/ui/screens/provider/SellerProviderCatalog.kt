package com.streamvault.app.ui.screens.provider

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Central provider catalog for seller-managed Xtream services.
 *
 * Phase 2 behavior:
 * 1) Primary remote config
 * 2) Backup remote config
 * 3) Last-known-good local cache
 * 4) Hardcoded defaults (bootstrap only)
 */
data class SellerXtreamService(
    val id: String,
    val displayName: String,
    val serverUrl: String,
    val defaultPlaylistName: String = displayName
)

data class SellerCatalogStatus(
    val version: Int,
    val source: String,
    val fetchedAtMs: Long
)

object SellerProviderCatalog {
    const val REMOTE_CONFIG_URL_PRIMARY: String = "https://astranettv.com/streamvault/config.json"
    const val REMOTE_CONFIG_URL_BACKUP: String = "https://astrahosting.xyz/streamvault/config.json"

    private const val PREFS_NAME = "seller_provider_catalog"
    private const val KEY_CACHE_JSON = "cache_json"
    private const val KEY_CACHE_VERSION = "cache_version"
    private const val KEY_CACHE_TTL_SECONDS = "cache_ttl_seconds"
    private const val KEY_CACHE_FETCHED_AT_MS = "cache_fetched_at_ms"

    private const val CONNECT_TIMEOUT_MS = 7000
    private const val READ_TIMEOUT_MS = 7000
    private const val DEFAULT_TTL_SECONDS = 3600L

    private val defaultServices: List<SellerXtreamService> = listOf(
        SellerXtreamService(
            id = "apex",
            displayName = "Apex",
            serverUrl = "https://hyper-apex.com"
        ),
        SellerXtreamService(
            id = "combo",
            displayName = "Combo",
            serverUrl = "https://toastybread.fyi"
        ),
        SellerXtreamService(
            id = "premium",
            displayName = "Premium",
            serverUrl = "https://musclesx.cc"
        )
    )

    @Volatile
    private var inMemoryServices: List<SellerXtreamService> = defaultServices

    @Volatile
    private var inMemoryStatus: SellerCatalogStatus = SellerCatalogStatus(
        version = 0,
        source = "default",
        fetchedAtMs = 0L
    )

    val services: List<SellerXtreamService>
        get() = inMemoryServices

    val status: SellerCatalogStatus
        get() = inMemoryStatus

    /**
     * Loads the catalog with fallback order:
     * fresh cache -> primary remote -> backup remote -> stale cache -> hardcoded defaults.
     */
    suspend fun load(context: Context): List<SellerXtreamService> = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = readCachedConfig(prefs)

        // 1) Fresh cache
        if (cached != null && isCacheFresh(cached)) {
            applyConfig(cached, source = "cache-fresh")
            return@withContext inMemoryServices
        }

        val cachedVersion = cached?.version ?: 0

        // 2) Primary remote config
        val primary = fetchAndValidate(REMOTE_CONFIG_URL_PRIMARY, minAcceptedVersion = cachedVersion)
        if (primary != null) {
            saveCache(prefs, primary)
            applyConfig(primary, source = "primary")
            return@withContext inMemoryServices
        }

        // 3) Backup remote config
        val backup = fetchAndValidate(REMOTE_CONFIG_URL_BACKUP, minAcceptedVersion = cachedVersion)
        if (backup != null) {
            saveCache(prefs, backup)
            applyConfig(backup, source = "backup")
            return@withContext inMemoryServices
        }

        // 4) Last-known-good cache (even if stale)
        if (cached != null && cached.services.isNotEmpty()) {
            applyConfig(cached, source = "cache-stale")
            return@withContext inMemoryServices
        }

        // 5) Hardcoded defaults as bootstrap safety net
        inMemoryServices = defaultServices
        inMemoryStatus = SellerCatalogStatus(version = 0, source = "default", fetchedAtMs = 0L)
        return@withContext inMemoryServices
    }

    /**
     * Force remote refresh (ignore TTL), while still honoring anti-rollback.
     */
    suspend fun forceRefresh(context: Context): List<SellerXtreamService> = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = readCachedConfig(prefs)
        val cachedVersion = cached?.version ?: 0

        val primary = fetchAndValidate(REMOTE_CONFIG_URL_PRIMARY, minAcceptedVersion = cachedVersion)
        if (primary != null) {
            saveCache(prefs, primary)
            applyConfig(primary, source = "primary")
            return@withContext inMemoryServices
        }

        val backup = fetchAndValidate(REMOTE_CONFIG_URL_BACKUP, minAcceptedVersion = cachedVersion)
        if (backup != null) {
            saveCache(prefs, backup)
            applyConfig(backup, source = "backup")
            return@withContext inMemoryServices
        }

        if (cached != null && cached.services.isNotEmpty()) {
            applyConfig(cached, source = "cache-stale")
            return@withContext inMemoryServices
        }

        inMemoryServices = defaultServices
        inMemoryStatus = SellerCatalogStatus(version = 0, source = "default", fetchedAtMs = 0L)
        return@withContext inMemoryServices
    }

    fun findById(id: String?): SellerXtreamService? = services.firstOrNull { it.id == id }

    private fun applyConfig(config: ParsedConfig, source: String) {
        inMemoryServices = config.services
        inMemoryStatus = SellerCatalogStatus(
            version = config.version,
            source = source,
            fetchedAtMs = config.fetchedAtMs
        )
    }

    private data class ParsedConfig(
        val version: Int,
        val ttlSeconds: Long,
        val fetchedAtMs: Long,
        val rawJson: String,
        val services: List<SellerXtreamService>
    )

    private fun isCacheFresh(config: ParsedConfig): Boolean {
        val now = System.currentTimeMillis()
        val ageMs = now - config.fetchedAtMs
        return ageMs in 0..(config.ttlSeconds * 1000)
    }

    private fun readCachedConfig(prefs: android.content.SharedPreferences): ParsedConfig? {
        val rawJson = prefs.getString(KEY_CACHE_JSON, null) ?: return null
        val cachedVersion = prefs.getInt(KEY_CACHE_VERSION, 0)
        val cachedTtl = prefs.getLong(KEY_CACHE_TTL_SECONDS, DEFAULT_TTL_SECONDS).coerceAtLeast(60)
        val fetchedAt = prefs.getLong(KEY_CACHE_FETCHED_AT_MS, 0L)
        if (fetchedAt <= 0L) return null

        val parsed = parseServicesFromJson(rawJson) ?: return null
        return ParsedConfig(
            version = cachedVersion,
            ttlSeconds = cachedTtl,
            fetchedAtMs = fetchedAt,
            rawJson = rawJson,
            services = parsed
        )
    }

    private fun saveCache(prefs: android.content.SharedPreferences, config: ParsedConfig) {
        prefs.edit()
            .putString(KEY_CACHE_JSON, config.rawJson)
            .putInt(KEY_CACHE_VERSION, config.version)
            .putLong(KEY_CACHE_TTL_SECONDS, config.ttlSeconds)
            .putLong(KEY_CACHE_FETCHED_AT_MS, config.fetchedAtMs)
            .apply()
    }

    private fun fetchAndValidate(url: String, minAcceptedVersion: Int): ParsedConfig? {
        val response = fetchJson(url) ?: return null
        val json = runCatching { JSONObject(response) }.getOrNull() ?: return null

        val version = json.optInt("version", 0)
        // Anti-rollback: reject configs older than the last-known-good cached version.
        if (version < minAcceptedVersion) return null

        val ttlSeconds = json.optLong("ttl_seconds", DEFAULT_TTL_SECONDS).coerceAtLeast(60)
        val parsedServices = parseServicesFromJson(response) ?: return null
        if (parsedServices.isEmpty()) return null

        return ParsedConfig(
            version = version,
            ttlSeconds = ttlSeconds,
            fetchedAtMs = System.currentTimeMillis(),
            rawJson = response,
            services = parsedServices
        )
    }

    private fun fetchJson(url: String): String? {
        val connection = runCatching {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
        }.getOrNull() ?: return null

        return runCatching {
            connection.connect()
            if (connection.responseCode !in 200..299) return@runCatching null
            connection.inputStream.bufferedReader().use { it.readText() }
        }.getOrNull().also {
            connection.disconnect()
        }
    }

    private fun parseServicesFromJson(rawJson: String): List<SellerXtreamService>? {
        val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null

        // Preferred schema: services is an array of objects
        val servicesArray = root.optJSONArray("services")
        if (servicesArray != null) {
            val parsed = mutableListOf<SellerXtreamService>()
            for (i in 0 until servicesArray.length()) {
                val item = servicesArray.optJSONObject(i) ?: continue
                val id = item.optString("id").trim()
                val displayName = item.optString("display_name").trim()
                val serverUrl = item.optString("server_url").trim()
                if (id.isBlank() || displayName.isBlank() || serverUrl.isBlank()) continue
                val defaultPlaylistName = item.optString("default_playlist_name").trim().ifBlank { displayName }

                parsed += SellerXtreamService(
                    id = id,
                    displayName = displayName,
                    serverUrl = serverUrl,
                    defaultPlaylistName = defaultPlaylistName
                )
            }
            if (parsed.isNotEmpty()) return parsed
        }

        // Backward-compatible schema: services is a map of "Name": "https://url"
        val servicesObject = root.optJSONObject("services") ?: return null
        val parsedFromMap = mutableListOf<SellerXtreamService>()
        val keys = servicesObject.keys()
        while (keys.hasNext()) {
            val displayName = keys.next().trim()
            val serverUrl = servicesObject.optString(displayName).trim()
            if (displayName.isBlank() || serverUrl.isBlank()) continue
            val id = displayName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            if (id.isBlank()) continue
            parsedFromMap += SellerXtreamService(
                id = id,
                displayName = displayName,
                serverUrl = serverUrl,
                defaultPlaylistName = displayName
            )
        }

        return parsedFromMap.takeIf { it.isNotEmpty() }
    }
}
