package com.streamvault.domain.model

enum class RecordingStatus {
    SCHEDULED,
    RECORDING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class RecordingRequest(
    val providerId: Long,
    val channelId: Long,
    val channelName: String,
    val streamUrl: String,
    val scheduledStartMs: Long,
    val scheduledEndMs: Long,
    val programTitle: String? = null,
    val outputPath: String? = null
)

data class RecordingItem(
    val id: String,
    val providerId: Long,
    val channelId: Long,
    val channelName: String,
    val streamUrl: String,
    val scheduledStartMs: Long,
    val scheduledEndMs: Long,
    val programTitle: String? = null,
    val outputPath: String? = null,
    val status: RecordingStatus = RecordingStatus.SCHEDULED,
    val failureReason: String? = null
)

data class RecordingStorageState(
    val outputDirectory: String? = null,
    val availableBytes: Long? = null,
    val isWritable: Boolean = false
)
