package com.streamvault.app.ui.screens.multiview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.domain.model.Channel
import com.streamvault.player.PlayerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class MultiViewViewModel @Inject constructor(
    private val multiViewManager: MultiViewManager,
    private val playerEngineProvider: Provider<PlayerEngine>
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiViewUiState())
    val uiState: StateFlow<MultiViewUiState> = _uiState.asStateFlow()

    val queueFlow = multiViewManager.queue

    private val playerEngines = mutableListOf<PlayerEngine>()

    fun initSlots() {
        val channels = multiViewManager.queue.value
        val slots = channels.mapIndexed { index, channel ->
            val engine = playerEngineProvider.get()
            playerEngines.add(engine)
            MultiViewSlot(
                index = index,
                streamUrl = channel.streamUrl,
                title = channel.name,
                playerEngine = engine,
                isLoading = true
            )
        }
        // Fill remaining empty slots
        val allSlots = slots + (slots.size until 4).map { MultiViewSlot(index = it) }
        _uiState.value = MultiViewUiState(slots = allSlots.take(4))

        // Start all engines
        slots.forEachIndexed { index, slot ->
            viewModelScope.launch {
                try {
                    slot.playerEngine?.prepare(
                        com.streamvault.domain.model.StreamInfo(
                            url = slot.streamUrl
                        )
                    )
                    slot.playerEngine?.play()
                    updateSlot(index) { it.copy(isLoading = false) }
                } catch (e: Exception) {
                    updateSlot(index) { it.copy(isLoading = false, hasError = true) }
                }
            }
        }

        // Mute all except focused
        applyFocusAudio(0)
    }

    fun setFocus(slotIndex: Int) {
        _uiState.value = _uiState.value.copy(focusedSlotIndex = slotIndex)
        applyFocusAudio(slotIndex)
    }

    private fun applyFocusAudio(focusedIndex: Int) {
        playerEngines.forEachIndexed { index, engine ->
            if (index == focusedIndex) engine.setVolume(1f) else engine.setVolume(0f)
        }
    }

    fun addToQueue(channel: Channel) {
        multiViewManager.addChannel(channel)
    }

    fun removeFromQueue(channelId: Long) {
        multiViewManager.removeChannel(channelId)
    }

    fun clearQueue() {
        multiViewManager.clearQueue()
    }

    private fun updateSlot(index: Int, transform: (MultiViewSlot) -> MultiViewSlot) {
        val updated = _uiState.value.slots.toMutableList()
        if (index < updated.size) {
            updated[index] = transform(updated[index])
            _uiState.value = _uiState.value.copy(slots = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerEngines.forEach { it.release() }
        playerEngines.clear()
    }
}
