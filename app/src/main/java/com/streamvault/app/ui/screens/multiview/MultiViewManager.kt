package com.streamvault.app.ui.screens.multiview

import com.streamvault.domain.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager that holds the pre-launch queue for the Multi-View (Split Screen) feature.
 * Users can add up to 4 channels before launching the MultiViewScreen.
 */
@Singleton
class MultiViewManager @Inject constructor() {

    private val _queue = MutableStateFlow<List<Channel>>(emptyList())
    val queue: StateFlow<List<Channel>> = _queue.asStateFlow()

    val isFull: Boolean get() = _queue.value.size >= MAX_SLOTS

    fun addChannel(channel: Channel) {
        val current = _queue.value
        if (current.size < MAX_SLOTS && current.none { it.id == channel.id }) {
            _queue.value = current + channel
        }
    }

    fun removeChannel(channelId: Long) {
        _queue.value = _queue.value.filter { it.id != channelId }
    }

    fun clearQueue() {
        _queue.value = emptyList()
    }

    fun hasChannels(): Boolean = _queue.value.isNotEmpty()

    companion object {
        const val MAX_SLOTS = 4
    }
}
