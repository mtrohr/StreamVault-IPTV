package com.streamvault.data.manager

import com.streamvault.domain.model.RecordingItem
import com.streamvault.domain.model.RecordingStatus

internal fun Iterable<RecordingItem>.findRecordingConflict(
    candidateStartMs: Long,
    candidateEndMs: Long,
    statuses: Set<RecordingStatus>
): RecordingItem? {
    return firstOrNull { item ->
        item.status in statuses &&
            item.scheduledStartMs < candidateEndMs &&
            item.scheduledEndMs > candidateStartMs
    }
}
