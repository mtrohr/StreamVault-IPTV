package com.streamvault.domain.manager

import kotlinx.coroutines.flow.Flow
import com.streamvault.domain.model.Result

data class BackupData(
    val version: Int = 1,
    val preferences: Map<String, String>? = null,
    val favorites: List<com.streamvault.domain.model.Favorite>? = null,
    val virtualGroups: List<com.streamvault.domain.model.VirtualGroup>? = null
)

interface BackupManager {
    /**
     * Exports the configuration to the provided URI string (SAF document URI)
     */
    suspend fun exportConfig(uriString: String): com.streamvault.domain.model.Result<Unit>

    /**
     * Imports the configuration from the provided URI string (SAF document URI)
     */
    suspend fun importConfig(uriString: String): com.streamvault.domain.model.Result<Unit>
}
