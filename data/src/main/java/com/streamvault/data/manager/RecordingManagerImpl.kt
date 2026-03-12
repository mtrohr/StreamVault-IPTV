package com.streamvault.data.manager

import android.content.Context
import android.os.StatFs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streamvault.domain.manager.RecordingManager
import com.streamvault.domain.model.RecordingItem
import com.streamvault.domain.model.RecordingRequest
import com.streamvault.domain.model.RecordingStatus
import com.streamvault.domain.model.RecordingStorageState
import com.streamvault.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class RecordingManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : RecordingManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateFile by lazy { File(recordingsDir, "recordings_state.json") }
    private val recordingsDir by lazy { File(context.filesDir, "recordings").apply { mkdirs() } }

    private val itemsState = MutableStateFlow(loadState())
    private val storageState = MutableStateFlow(readStorageState())
    private val activeJobs = mutableMapOf<String, Job>()

    init {
        scope.launch {
            while (true) {
                processSchedules()
                storageState.value = readStorageState()
                delay(15_000L)
            }
        }
    }

    override fun observeRecordingItems(): Flow<List<RecordingItem>> = itemsState.asStateFlow()

    override fun observeStorageState(): Flow<RecordingStorageState> = storageState.asStateFlow()

    override suspend fun startManualRecording(request: RecordingRequest): Result<RecordingItem> = withContext(Dispatchers.IO) {
        if (isAdaptiveStream(request.streamUrl)) {
            return@withContext Result.error("Live recording currently supports direct stream URLs only. Adaptive streams are not recordable yet.")
        }
        if (!storageState.value.isWritable) {
            return@withContext Result.error("Recording storage is not writable.")
        }

        val item = RecordingItem(
            id = UUID.randomUUID().toString(),
            providerId = request.providerId,
            channelId = request.channelId,
            channelName = request.channelName,
            streamUrl = request.streamUrl,
            scheduledStartMs = System.currentTimeMillis(),
            scheduledEndMs = request.scheduledEndMs,
            programTitle = request.programTitle,
            outputPath = request.outputPath ?: buildOutputFile(request).absolutePath,
            status = RecordingStatus.RECORDING
        )
        itemsState.value = itemsState.value + item
        persistState()
        startCapture(item)
        storageState.value = readStorageState()
        Result.success(item)
    }

    override suspend fun scheduleRecording(request: RecordingRequest): Result<RecordingItem> = withContext(Dispatchers.IO) {
        val item = RecordingItem(
            id = UUID.randomUUID().toString(),
            providerId = request.providerId,
            channelId = request.channelId,
            channelName = request.channelName,
            streamUrl = request.streamUrl,
            scheduledStartMs = request.scheduledStartMs,
            scheduledEndMs = request.scheduledEndMs,
            programTitle = request.programTitle,
            outputPath = request.outputPath ?: buildOutputFile(request).absolutePath,
            status = RecordingStatus.SCHEDULED
        )
        itemsState.value = itemsState.value + item
        persistState()
        Result.success(item)
    }

    override suspend fun stopRecording(recordingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        activeJobs.remove(recordingId)?.cancel()
        val item = itemsState.value.firstOrNull { it.id == recordingId }
            ?: return@withContext Result.error("Recording not found")
        val fileLength = item.outputPath?.let { path -> File(path).takeIf { it.exists() }?.length() } ?: 0L
        updateItem(recordingId) {
            it.copy(
                status = if (fileLength > 0L) RecordingStatus.COMPLETED else RecordingStatus.CANCELLED,
                scheduledEndMs = System.currentTimeMillis()
            )
        }
        Result.success(Unit)
    }

    override suspend fun cancelRecording(recordingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        activeJobs.remove(recordingId)?.cancel()
        updateItem(recordingId) { it.copy(status = RecordingStatus.CANCELLED) }
        Result.success(Unit)
    }

    private fun processSchedules() {
        val now = System.currentTimeMillis()
        itemsState.value
            .filter { it.status == RecordingStatus.SCHEDULED && it.scheduledStartMs <= now }
            .forEach { scheduled ->
                if (isAdaptiveStream(scheduled.streamUrl)) {
                    updateItem(scheduled.id) {
                        it.copy(
                            status = RecordingStatus.FAILED,
                            failureReason = "Scheduled recording supports direct stream URLs only."
                        )
                    }
                } else {
                    startCapture(scheduled.copy(status = RecordingStatus.RECORDING))
                    updateItem(scheduled.id) { current -> current.copy(status = RecordingStatus.RECORDING) }
                }
            }

        itemsState.value
            .filter { it.status == RecordingStatus.RECORDING && it.scheduledEndMs in 1 until now }
            .forEach { stopCandidate ->
                activeJobs.remove(stopCandidate.id)?.cancel()
                updateItem(stopCandidate.id) { it.copy(status = RecordingStatus.COMPLETED) }
            }
    }

    private fun startCapture(item: RecordingItem) {
        if (activeJobs.containsKey(item.id)) return
        val job = scope.launch {
            val outputFile = File(item.outputPath ?: return@launch)
            outputFile.parentFile?.mkdirs()
            runCatching {
                val connection = URL(item.streamUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = true
                connection.connect()
                connection.inputStream.use { input ->
                    FileOutputStream(outputFile, true).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val bytes = input.read(buffer)
                            if (bytes <= 0) break
                            output.write(buffer, 0, bytes)
                            if (!kotlin.coroutines.coroutineContext.isActive) break
                        }
                    }
                }
            }.onSuccess {
                updateItem(item.id) { current ->
                    current.copy(status = RecordingStatus.COMPLETED)
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) {
                    val fileLength = outputFile.takeIf { it.exists() }?.length() ?: 0L
                    updateItem(item.id) { current ->
                        current.copy(status = if (fileLength > 0L) RecordingStatus.COMPLETED else RecordingStatus.CANCELLED)
                    }
                } else {
                    updateItem(item.id) { current ->
                        current.copy(status = RecordingStatus.FAILED, failureReason = error.message)
                    }
                }
            }
            activeJobs.remove(item.id)
            storageState.value = readStorageState()
        }
        activeJobs[item.id] = job
    }

    private fun updateItem(recordingId: String, transform: (RecordingItem) -> RecordingItem) {
        itemsState.value = itemsState.value.map { item ->
            if (item.id == recordingId) transform(item) else item
        }
        persistState()
    }

    private fun buildOutputFile(request: RecordingRequest): File {
        val safeName = request.channelName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(recordingsDir, "${safeName}_${request.scheduledStartMs}.ts")
    }

    private fun readStorageState(): RecordingStorageState {
        recordingsDir.mkdirs()
        val stat = StatFs(recordingsDir.absolutePath)
        return RecordingStorageState(
            outputDirectory = recordingsDir.absolutePath,
            availableBytes = stat.availableBytes,
            isWritable = recordingsDir.canWrite()
        )
    }

    private fun persistState() {
        stateFile.parentFile?.mkdirs()
        stateFile.writeText(gson.toJson(itemsState.value))
    }

    private fun loadState(): List<RecordingItem> {
        if (!stateFile.exists()) return emptyList()
        return runCatching {
            val listType = object : TypeToken<List<RecordingItem>>() {}.type
            gson.fromJson<List<RecordingItem>>(FileInputStream(stateFile).bufferedReader(), listType).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun isAdaptiveStream(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains(".mpd")
    }
}
