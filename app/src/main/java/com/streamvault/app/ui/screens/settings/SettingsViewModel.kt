package com.streamvault.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.data.sync.SyncManager
import com.streamvault.data.sync.SyncRepairSection
import com.streamvault.domain.model.Provider
import com.streamvault.domain.model.ProviderStatus
import com.streamvault.domain.model.Result
import com.streamvault.domain.model.SyncState
import com.streamvault.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.streamvault.domain.manager.BackupManager

enum class ProviderWarningAction {
    EPG,
    MOVIES,
    SERIES
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val preferencesRepository: PreferencesRepository,
    private val backupManager: BackupManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                providerRepository.getProviders(),
                preferencesRepository.lastActiveProviderId,
                preferencesRepository.parentalControlLevel,
                preferencesRepository.appLanguage
            ) { providers, activeId, level, language ->
                arrayOf(providers, activeId, level, language)
            }.collect { values ->
                @Suppress("UNCHECKED_CAST")
                _uiState.update {
                    it.copy(
                        providers = values[0] as List<Provider>,
                        activeProviderId = values[1] as Long?,
                        parentalControlLevel = values[2] as Int,
                        appLanguage = values[3] as String
                    )
                }
            }
        }
    }

    fun setActiveProvider(providerId: Long) {
        viewModelScope.launch {
            preferencesRepository.setLastActiveProviderId(providerId)
            providerRepository.setActiveProvider(providerId)
            // Force sync on connect
            refreshProvider(providerId)
        }
    }

    fun setParentalControlLevel(level: Int) {
        viewModelScope.launch {
            preferencesRepository.setParentalControlLevel(level)
        }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            preferencesRepository.setAppLanguage(language)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return preferencesRepository.verifyParentalPin(pin)
    }

    fun changePin(newPin: String) {
        viewModelScope.launch {
            preferencesRepository.setParentalPin(newPin)
            _uiState.update { it.copy(userMessage = "PIN changed successfully") }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun refreshProvider(providerId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = providerRepository.refreshProviderData(providerId, force = true)
            val refreshedProvider = providerRepository.getProvider(providerId)
            _uiState.update { state ->
                val partialWarnings = (syncManager.syncState.value as? SyncState.Partial)?.warnings.orEmpty()
                val warningsMessage = partialWarnings
                    .take(3)
                    .joinToString(separator = ", ")
                    .ifBlank { "Some sections are incomplete." }
                state.copy(
                    isSyncing = false,
                    userMessage = when {
                        result is Result.Error -> "Refresh failed: ${result.message}"
                        refreshedProvider?.status == ProviderStatus.PARTIAL -> "Refresh completed with warnings: $warningsMessage"
                        else -> "Provider refreshed successfully"
                    },
                    syncWarningsByProvider = when {
                        result is Result.Error -> state.syncWarningsByProvider - providerId
                        refreshedProvider?.status == ProviderStatus.PARTIAL -> state.syncWarningsByProvider + (providerId to partialWarnings)
                        else -> state.syncWarningsByProvider - providerId
                    }
                )
            }
        }
    }

    fun retryWarningAction(providerId: Long, action: ProviderWarningAction) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val section = when (action) {
                ProviderWarningAction.EPG -> SyncRepairSection.EPG
                ProviderWarningAction.MOVIES -> SyncRepairSection.MOVIES
                ProviderWarningAction.SERIES -> SyncRepairSection.SERIES
            }
            val result = syncManager.retrySection(providerId, section)
            _uiState.update { state ->
                if (result is Result.Error) {
                    state.copy(
                        isSyncing = false,
                        userMessage = "Retry failed: ${result.message}"
                    )
                } else {
                    val currentWarnings = state.syncWarningsByProvider[providerId].orEmpty()
                    val updatedWarnings = currentWarnings.filterNot { warning ->
                        when (action) {
                            ProviderWarningAction.EPG -> warning.contains("EPG", ignoreCase = true)
                            ProviderWarningAction.MOVIES -> warning.contains("Movies", ignoreCase = true)
                            ProviderWarningAction.SERIES -> warning.contains("Series", ignoreCase = true)
                        }
                    }
                    state.copy(
                        isSyncing = false,
                        userMessage = if (updatedWarnings.isEmpty()) {
                            "Section retry succeeded. All current warnings cleared."
                        } else {
                            "Section retry succeeded."
                        },
                        syncWarningsByProvider = if (updatedWarnings.isEmpty()) {
                            state.syncWarningsByProvider - providerId
                        } else {
                            state.syncWarningsByProvider + (providerId to updatedWarnings)
                        }
                    )
                }
            }
        }
    }

    fun deleteProvider(providerId: Long) {
        viewModelScope.launch {
            providerRepository.deleteProvider(providerId)
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun exportConfig(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = backupManager.exportConfig(uriString)
            _uiState.update { state ->
                state.copy(
                    isSyncing = false,
                    userMessage = if (result is Result.Error)
                        "Export failed: ${result.message}"
                    else "Configuration exported successfully"
                )
            }
        }
    }

    fun importConfig(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = backupManager.importConfig(uriString)
            _uiState.update { state ->
                state.copy(
                    isSyncing = false,
                    userMessage = if (result is Result.Error)
                        "Import failed: ${result.message}"
                    else "Configuration imported successfully"
                )
            }
        }
    }
}

data class SettingsUiState(
    val providers: List<Provider> = emptyList(),
    val activeProviderId: Long? = null,
    val isSyncing: Boolean = false,
    val userMessage: String? = null,
    val syncWarningsByProvider: Map<Long, List<String>> = emptyMap(),
    val parentalControlLevel: Int = 0,
    val appLanguage: String = "system"
)
