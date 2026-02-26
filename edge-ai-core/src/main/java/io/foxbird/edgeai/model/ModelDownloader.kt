package io.foxbird.edgeai.model

import android.content.Context
import io.foxbird.edgeai.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import kotlinx.coroutines.currentCoroutineContext

sealed class DownloadEvent {
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadEvent() {
        val progressPercent: Float
            get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    }
    data class Complete(val filePath: String) : DownloadEvent()
    data class Error(val message: String, val isRetryable: Boolean = true) : DownloadEvent()
    data class Retrying(val attempt: Int, val maxAttempts: Int, val reason: String) : DownloadEvent()
}

class ModelDownloader(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloader"
        private const val MODELS_DIR = "models"
        private const val BUFFER_SIZE = 8 * 1024
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000L
            socketTimeoutMillis = 60_000L
        }
    }

    fun getModelsDirectory(): File {
        val dir = File(context.filesDir, MODELS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelPath(config: ModelConfig): File {
        return File(getModelsDirectory(), config.filename)
    }

    fun isModelDownloaded(config: ModelConfig): Boolean {
        val file = getModelPath(config)
        return file.exists() && file.length() > 10 * 1024 * 1024
    }

    fun downloadModel(config: ModelConfig): Flow<DownloadEvent> = flow {
        val targetFile = getModelPath(config)
        val tempFile = File(targetFile.parent, "${config.filename}.tmp")

        Logger.i(TAG, "Starting download: ${config.name} from ${config.downloadUrl}")

        var lastError: Exception? = null

        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            try {
                val success = attemptDownload(config, targetFile, tempFile) { event ->
                    when (event) {
                        is DownloadEvent.Progress -> emit(event)
                        is DownloadEvent.Complete -> emit(event)
                        else -> {}
                    }
                }
                if (success) return@flow
            } catch (e: java.net.UnknownHostException) {
                emit(DownloadEvent.Error("No internet connection", isRetryable = true))
                return@flow
            } catch (e: java.net.SocketTimeoutException) {
                lastError = e
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    emit(DownloadEvent.Retrying(attempt + 1, MAX_RETRY_ATTEMPTS, "Connection timeout"))
                    delay(RETRY_DELAY_MS * attempt)
                }
            } catch (e: java.io.IOException) {
                lastError = e
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    emit(DownloadEvent.Retrying(attempt + 1, MAX_RETRY_ATTEMPTS, "Network error"))
                    delay(RETRY_DELAY_MS * attempt)
                }
            } catch (e: Exception) {
                tempFile.delete()
                emit(DownloadEvent.Error(e.message ?: "Unknown error", isRetryable = false))
                return@flow
            }
        }

        tempFile.delete()
        emit(DownloadEvent.Error(
            "Download failed after $MAX_RETRY_ATTEMPTS attempts: ${lastError?.message}",
            isRetryable = true
        ))
    }.flowOn(Dispatchers.IO)

    private suspend fun attemptDownload(
        config: ModelConfig,
        targetFile: File,
        tempFile: File,
        onEvent: suspend (DownloadEvent) -> Unit
    ): Boolean {
        val ctx = currentCoroutineContext()
        client.prepareGet(config.downloadUrl).execute { response ->
            if (!response.status.isSuccess()) {
                throw Exception("HTTP ${response.status.value}")
            }

            val totalBytes = response.contentLength() ?: (config.fileSizeMB * 1024L * 1024L)
            var bytesDownloaded = 0L
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(BUFFER_SIZE)

            tempFile.outputStream().use { output ->
                while (!channel.isClosedForRead && ctx.isActive) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead <= 0) break
                    output.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead
                    onEvent(DownloadEvent.Progress(bytesDownloaded, totalBytes))
                }
            }

            if (!ctx.isActive) {
                tempFile.delete()
                return@execute false
            }

            if (targetFile.exists()) targetFile.delete()
            if (tempFile.renameTo(targetFile)) {
                onEvent(DownloadEvent.Complete(targetFile.absolutePath))
                return@execute true
            } else {
                throw Exception("Failed to save model file")
            }
        }
        return true
    }

    suspend fun deleteModel(config: ModelConfig): Boolean {
        val file = getModelPath(config)
        return if (file.exists()) file.delete() else true
    }

    fun getAvailableStorage(): Long = getModelsDirectory().freeSpace

    fun close() { client.close() }
}
