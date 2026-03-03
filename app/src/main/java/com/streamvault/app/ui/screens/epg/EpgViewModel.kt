package com.streamvault.app.ui.screens.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.EpgRepository
import com.streamvault.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpgUiState(
    val channels: List<Channel> = emptyList(),
    val programsByChannel: Map<String, List<Program>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpgUiState())
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                providerRepository.getActiveProvider().firstOrNull()?.let { provider ->
                    channelRepository.getChannels(provider.id).collect { channels ->
                        if (channels.isNotEmpty()) {
                            val epgIds = channels.mapNotNull { it.epgChannelId }.distinct()
                            
                            // Listen for EPG updates for all these channels
                            epgRepository.getNowPlayingForChannels(epgIds).collect { programsDict ->
                                _uiState.update { 
                                    it.copy(
                                        channels = channels.take(100), // Limit to 100 for now to avoid grid overload
                                        programsByChannel = programsDict,
                                        isLoading = false
                                    ) 
                                }
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false, channels = emptyList()) }
                        }
                    }
                } ?: run {
                    _uiState.update { it.copy(isLoading = false, error = "No active provider") }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load EPG data"
                    ) 
                }
            }
        }
    }
}
