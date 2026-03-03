package com.streamvault.data.manager

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.streamvault.data.local.dao.FavoriteDao
import com.streamvault.data.local.dao.VirtualGroupDao
import com.streamvault.data.mapper.toDomain
import com.streamvault.data.mapper.toEntity
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.domain.manager.BackupData
import com.streamvault.domain.manager.BackupManager
import com.streamvault.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

class BackupManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val favoriteDao: FavoriteDao,
    private val virtualGroupDao: VirtualGroupDao,
    private val gson: Gson
) : BackupManager {

    override suspend fun exportConfig(uriString: String): com.streamvault.domain.model.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            
            // 1. Gather Data
            val prefs = mapOf(
                "parentalControlLevel" to preferencesRepository.parentalControlLevel.first().toString(),
                "parentalPin" to (preferencesRepository.parentalPin.first() ?: ""),
                // We're leaving out provider credentials/URLs from the backup for security/complexity,
                // but we could back them up as well if requested.
            )

            // Gather all favorites across all types
            val liveFavs = favoriteDao.getAllByType("LIVE").first().map { it.toDomain() }
            val movieFavs = favoriteDao.getAllByType("MOVIE").first().map { it.toDomain() }
            val seriesFavs = favoriteDao.getAllByType("SERIES").first().map { it.toDomain() }
            val allFavorites = liveFavs + movieFavs + seriesFavs

            // Gather all custom groups
            val liveGroups = virtualGroupDao.getByType("LIVE").first().map { it.toDomain() }
            val movieGroups = virtualGroupDao.getByType("MOVIE").first().map { it.toDomain() }
            val seriesGroups = virtualGroupDao.getByType("SERIES").first().map { it.toDomain() }
            val allGroups = liveGroups + movieGroups + seriesGroups

            val backupData = BackupData(
                version = 1,
                preferences = prefs,
                favorites = allFavorites,
                virtualGroups = allGroups
            )

            // 2. Serialize and write to URI
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    gson.toJson(backupData, writer)
                }
            } ?: return@withContext com.streamvault.domain.model.Result.error("Failed to open output stream")

            com.streamvault.domain.model.Result.success(Unit)
        } catch (e: Exception) {
            com.streamvault.domain.model.Result.error("Failed to export backup: ${e.message}", e)
        }
    }

    override suspend fun importConfig(uriString: String): com.streamvault.domain.model.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)

            // 1. Read and Deserialize
            val backupData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, BackupData::class.java)
                }
            } ?: return@withContext com.streamvault.domain.model.Result.error("Failed to open input stream")

            if (backupData.version > 1) {
                return@withContext com.streamvault.domain.model.Result.error("Unsupported backup version")
            }

            // 2. Restore Preferences
            backupData.preferences?.let { prefs ->
                prefs["parentalControlLevel"]?.toIntOrNull()?.let {
                    preferencesRepository.setParentalControlLevel(it)
                }
                prefs["parentalPin"]?.let { pin ->
                    if (pin.isNotBlank()) preferencesRepository.setParentalPin(pin)
                }
            }

            // 3. Restore Virtual Groups
            backupData.virtualGroups?.let { groups ->
                groups.forEach { group ->
                    virtualGroupDao.insert(group.toEntity())
                }
            }

            // 4. Restore Favorites
            backupData.favorites?.let { favs ->
                favs.forEach { fav ->
                    favoriteDao.insert(fav.toEntity())
                }
            }

            com.streamvault.domain.model.Result.success(Unit)
        } catch (e: Exception) {
            com.streamvault.domain.model.Result.error("Failed to import backup: ${e.message}", e)
        }
    }
}
