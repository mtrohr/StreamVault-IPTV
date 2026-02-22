package com.streamvault.domain.repository

import com.streamvault.domain.model.Provider
import com.streamvault.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface ProviderRepository {
    fun getProviders(): Flow<List<Provider>>
    fun getActiveProvider(): Flow<Provider?>
    suspend fun getProvider(id: Long): Provider?
    suspend fun addProvider(provider: Provider): Result<Long>
    suspend fun updateProvider(provider: Provider): Result<Unit>
    suspend fun deleteProvider(id: Long): Result<Unit>
    suspend fun setActiveProvider(id: Long): Result<Unit>
    suspend fun loginXtream(serverUrl: String, username: String, password: String, name: String, onProgress: ((String) -> Unit)? = null, id: Long? = null): Result<Provider>
    suspend fun validateM3u(url: String, name: String, onProgress: ((String) -> Unit)? = null, id: Long? = null): Result<Provider>
    suspend fun refreshProviderData(providerId: Long, onProgress: ((String) -> Unit)? = null): Result<Unit>
    suspend fun buildCatchUpUrl(providerId: Long, streamId: Long, start: Long, end: Long): String?
}
