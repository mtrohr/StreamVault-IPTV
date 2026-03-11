package com.streamvault.domain.model

data class Program(
    val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val lang: String = "",
    val hasArchive: Boolean = false,
    val isNowPlaying: Boolean = false,
    val providerId: Long = 0L
) {
    val durationMinutes: Int
        get() = ((endTime - startTime) / 60000).toInt()

    val progressPercent: Float
        get() {
            if (!isNowPlaying) return 0f
            val now = System.currentTimeMillis()
            if (now < startTime || endTime <= startTime) return 0f
            return ((now - startTime).toFloat() / (endTime - startTime).toFloat()).coerceIn(0f, 1f)
        }
}
