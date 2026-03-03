package com.streamvault.data.repository

import com.streamvault.data.local.dao.ProgramDao
import com.streamvault.data.mapper.toDomain
import com.streamvault.data.mapper.toEntity
import com.streamvault.data.parser.XmltvParser
import com.streamvault.domain.model.Program
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.EpgRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val programDao: ProgramDao,
    private val xmltvParser: XmltvParser,
    private val okHttpClient: OkHttpClient
) : EpgRepository {

    override fun getProgramsForChannel(
        channelId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<Program>> =
        programDao.getForChannel(channelId, startTime, endTime)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getNowPlaying(channelId: String): Flow<Program?> =
        programDao.getNowPlaying(channelId, System.currentTimeMillis())
            .map { it?.toDomain() }

    override fun getNowPlayingForChannels(channelIds: List<String>): Flow<Map<String, List<Program>>> =
        programDao.getNowPlayingForChannels(channelIds, System.currentTimeMillis())
            .map { entities -> 
                entities.map { it.toDomain() }
                        .groupBy { it.channelId }
            }

    override fun getNowAndNext(channelId: String): Flow<Pair<Program?, Program?>> =
        programDao.getForChannel(
            channelId,
            System.currentTimeMillis() - 3600000, // 1 hour ago
            System.currentTimeMillis() + 7200000   // 2 hours from now
        ).map { entities ->
            val programs = entities.map { it.toDomain() }
            val now = System.currentTimeMillis()
            val current = programs.find { it.startTime <= now && it.endTime > now }
            val next = programs.find { it.startTime > now }
            Pair(current, next)
        }

    override suspend fun refreshEpg(providerId: Long, epgUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(epgUrl).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.error("Failed to download EPG: HTTP ${response.code}")
                }

                val body = response.body ?: return@withContext Result.error("Empty EPG response")

                val programs = body.byteStream().use { inputStream ->
                    xmltvParser.parse(inputStream)
                }

                // Batch insert in chunks to avoid memory pressure
                val entities = programs.map { it.toEntity() }
                entities.chunked(500).forEach { chunk ->
                    programDao.insertAll(chunk)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error("Failed to refresh EPG: ${e.message}", e)
            }
        }

    override suspend fun clearOldPrograms(beforeTime: Long) {
        programDao.deleteOld(beforeTime)
    }
}
