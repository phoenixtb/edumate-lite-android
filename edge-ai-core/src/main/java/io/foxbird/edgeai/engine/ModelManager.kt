package io.foxbird.edgeai.engine

import android.content.Context
import io.foxbird.edgeai.model.DownloadEvent
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelDownloader
import io.foxbird.edgeai.model.ModelInfo
import io.foxbird.edgeai.model.ModelPurpose
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edgeai.util.DeviceInfo
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-level model lifecycle management: discovery, bundled asset copy,
 * download, load/unload with fallback, deletion.
 *
 * @param allModels The complete list of model configurations this app supports.
 * @param bundledModelsAssetDir The asset directory containing bundled models (default "models").
 */
class ModelManager(
    private val context: Context,
    private val orchestrator: EngineOrchestrator,
    private val downloader: ModelDownloader,
    private val deviceInfo: DeviceInfo,
    private val allModels: List<ModelConfig>,
    private val bundledModelsAssetDir: String = "models"
) {
    companion object {
        private const val TAG = "ModelManager"
    }

    init {
        orchestrator.registerModels(allModels)
    }

    val modelStates: StateFlow<Map<String, ModelState>> get() = orchestrator.modelStates
    val activeInferenceModelId: StateFlow<String?> get() = orchestrator.activeInferenceModelId
    val activeEmbeddingModelId: StateFlow<String?> get() = orchestrator.activeEmbeddingModelId

    suspend fun initialize() {
        val states = mutableMapOf<String, ModelState>()
        for (config in allModels) {
            states[config.id] = when {
                isModelAvailable(config) -> ModelState.Downloaded
                config.isBundled && isBundledAssetPresent(config) -> ModelState.Downloaded
                else -> ModelState.NotDownloaded
            }
        }
        states.forEach { (id, state) -> orchestrator.updateModelState(id, state) }

        copyBundledModelsIfNeeded()

        Logger.d(TAG, "Initialized: ${states.count { it.value == ModelState.Downloaded }} models available")
    }

    private fun isBundledAssetPresent(config: ModelConfig): Boolean {
        return try {
            context.assets.open("$bundledModelsAssetDir/${config.filename}").use { true }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun copyBundledModelsIfNeeded() = withContext(Dispatchers.IO) {
        val modelsDir = downloader.getModelsDirectory()
        modelsDir.mkdirs()

        for (config in allModels.filter { it.isBundled }) {
            val target = getModelPath(config)
            if (target.exists() && target.length() > 10 * 1024 * 1024) continue

            try {
                context.assets.open("$bundledModelsAssetDir/${config.filename}").use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
                orchestrator.updateModelState(config.id, ModelState.Downloaded)
                Logger.i(TAG, "Copied bundled model: ${config.filename}")
            } catch (_: Exception) {
                // Asset not present in this build
            }
        }
    }

    fun getModelPath(config: ModelConfig): File {
        return File(downloader.getModelsDirectory(), config.filename)
    }

    fun isModelAvailable(config: ModelConfig): Boolean {
        val file = getModelPath(config)
        return file.exists() && file.length() > 10 * 1024 * 1024
    }

    fun getModels(): List<ModelInfo> {
        val states = modelStates.value
        return allModels.map { config ->
            ModelInfo(
                config = config,
                state = states[config.id] ?: ModelState.NotDownloaded,
                localPath = if (isModelAvailable(config)) getModelPath(config).absolutePath else null
            )
        }
    }

    fun getInferenceModels(): List<ModelInfo> = getModels().filter { it.config.purpose == ModelPurpose.INFERENCE }
    fun getEmbeddingModels(): List<ModelInfo> = getModels().filter { it.config.purpose == ModelPurpose.EMBEDDING }

    fun downloadModel(config: ModelConfig): Flow<DownloadEvent> {
        orchestrator.updateModelState(config.id, ModelState.Downloading(0f))
        return downloader.downloadModel(config)
    }

    suspend fun loadModel(config: ModelConfig): Boolean {
        if (!isModelAvailable(config)) {
            Logger.e(TAG, "Cannot load — not available: ${config.id}")
            return false
        }

        val path = getModelPath(config).absolutePath
        val threads = deviceInfo.getOptimalThreadCount()

        val success = orchestrator.loadModel(config, path, threads)

        if (!success && config.fallbackModelId != null) {
            val fallback = allModels.find { it.id == config.fallbackModelId }
            if (fallback != null && isModelAvailable(fallback)) {
                Logger.w(TAG, "Primary failed, trying fallback: ${fallback.name}")
                val fbPath = getModelPath(fallback).absolutePath
                return orchestrator.loadModel(fallback, fbPath, threads)
            }
        }

        return success
    }

    fun unloadModel(config: ModelConfig) {
        orchestrator.unloadModel(config)
    }

    suspend fun deleteModel(config: ModelConfig): Boolean {
        if (activeInferenceModelId.value == config.id || activeEmbeddingModelId.value == config.id) {
            unloadModel(config)
        }
        val success = downloader.deleteModel(config)
        if (success) orchestrator.updateModelState(config.id, ModelState.NotDownloaded)
        return success
    }

    fun canRunModel(config: ModelConfig): Boolean {
        return deviceInfo.getTotalRamGB() >= config.requiredRamGB
    }

    fun getDeviceSummary(): String {
        val s = deviceInfo.getDeviceSummary()
        return "${s.manufacturer} ${s.model} | ${s.totalRamGB}GB RAM"
    }
}
