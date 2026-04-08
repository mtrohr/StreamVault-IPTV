package com.streamvault.data.manager.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecordingRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                RecordingForegroundService.requestReconcile(context)
                RecordingReconcileWorker.enqueueOneShot(context)
            }
        }
    }
}
