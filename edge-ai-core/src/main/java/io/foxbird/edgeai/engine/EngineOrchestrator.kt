package io.foxbird.edgeai.engine

import arrow.core.left
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelPurpose
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edgeai.util.AppError
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central facade that routes inference/embedding requests to the appropriate engine
 * based on the currently loaded models. Manages engine lifecycles, fallback logic,
 * and concurrent model support (one inference + one embedding model simultaneously).
 */
class EngineOrchestrator(
    private val llamaCppEngine: LlamaCppEngine,
    private val liteRtEngine: LiteRtEngine,
    private val memoryMonitor: MemoryMonitor
) {
    companion object {
        private const val TAG = "EngineOrchestrator"
    }

    private val loadMutex = Mutex()

    private val _activeInferenceEngine = MutableStateFlow<EngineType?>(null)
    val activeInferenceEngine: StateFlow<EngineType?> = _activeInferenceEngine.asStateFlow()

    private val _activeEmbeddingEngine = MutableStateFlow<EngineType?>(null)
    val activeEmbeddingEngine: StateFlow<EngineType?> = _activeEmbeddingEngine.asStateFlow()

    private val _activeInferenceModelId = MutableStateFlow<String?>(null)
    val activeInferenceModelId: StateFlow<String?> = _activeInferenceModelId.asStateFlow()

    private val _activeEmbeddingModelId = MutableStateFlow<String?>(null)
    val activeEmbeddingModelId: StateFlow<String?> = _activeEmbeddingModelId.asStateFlow()

    private val _modelStates = MutableStateFlow<Map<String, ModelState>>(emptyMap())
    val modelStates: StateFlow<Map<String, ModelState>> = _modelStates.asStateFlow()

    private var allModels: List<ModelConfig> = emptyList()

    fun registerModels(models: List<ModelConfig>) {
        allModels = models
    }

    fun findModelById(id: String): ModelConfig? = allModels.find { it.id == id }

    fun updateModelState(modelId: String, state: ModelState) {
        _modelStates.value = _modelStates.value.toMutableMap().apply { this[modelId] = state }
    }

    fun getEngineForType(type: EngineType): InferenceEngine = when (type) {
        EngineType.LLAMA_CPP -> llamaCppEngine
        EngineType.LITE_RT -> liteRtEngine
    }

    private fun inferenceEngine(): InferenceEngine? {
        return _activeInferenceEngine.value?.let { getEngineForType(it) }
    }

    private fun embeddingEngine(): InferenceEngine? {
        return _activeEmbeddingEngine.value?.let { getEngineForType(it) }
    }

    suspend fun loadModel(config: ModelConfig, modelPath: String, threads: Int): Boolean = loadMutex.withLock {
        val requiredMb = config.fileSizeMB.toLong()
        if (!memoryMonitor.canAllocateMb(requiredMb)) {
            Logger.e(TAG, "Insufficient memory for ${config.name} (need ${requiredMb}MB)")
            updateModelState(config.id, ModelState.LoadFailed("Insufficient memory"))
            return false
        }

        val engine = getEngineForType(config.engineType)

        when (config.purpose) {
            ModelPurpose.INFERENCE -> {
                if (_activeInferenceEngine.value == config.engineType) {
                    engine.unloadModel()
                    _activeInferenceModelId.value?.let { updateModelState(it, ModelState.Downloaded) }
                }
            }
            ModelPurpose.EMBEDDING -> {
                if (_activeEmbeddingEngine.value == config.engineType) {
                    engine.unloadModel()
                    _activeEmbeddingModelId.value?.let { updateModelState(it, ModelState.Downloaded) }
                }
            }
        }

        updateModelState(config.id, ModelState.Loading)

        val success = engine.loadModel(modelPath, config.contextLength, threads)
        if (success) {
            updateModelState(config.id, ModelState.Ready)
            when (config.purpose) {
                ModelPurpose.INFERENCE -> {
                    _activeInferenceEngine.value = config.engineType
                    _activeInferenceModelId.value = config.id
                }
                ModelPurpose.EMBEDDING -> {
                    _activeEmbeddingEngine.value = config.engineType
                    _activeEmbeddingModelId.value = config.id
                }
            }
            Logger.i(TAG, "Loaded ${config.name} on ${config.engineType}")
            return true
        }

        if (config.engineType == EngineType.LITE_RT && config.fallbackModelId != null) {
            Logger.w(TAG, "LiteRT failed for ${config.name}, fallback not auto-triggered (caller must handle)")
        }

        val errorMsg = engine.lastLoadError ?: "Engine failed to load model"
        updateModelState(config.id, ModelState.LoadFailed(errorMsg))
        return false
    }

    fun unloadModel(config: ModelConfig) {
        val engine = getEngineForType(config.engineType)
        engine.unloadModel()
        updateModelState(config.id, ModelState.Downloaded)
        when (config.purpose) {
            ModelPurpose.INFERENCE -> {
                if (_activeInferenceModelId.value == config.id) {
                    _activeInferenceEngine.value = null
                    _activeInferenceModelId.value = null
                }
            }
            ModelPurpose.EMBEDDING -> {
                if (_activeEmbeddingModelId.value == config.id) {
                    _activeEmbeddingEngine.value = null
                    _activeEmbeddingModelId.value = null
                }
            }
        }
    }

    fun generate(prompt: String, params: GenerationParams = GenerationParams()): Flow<String> {
        val engine = inferenceEngine()
            ?: throw IllegalStateException("No inference model loaded")
        return engine.generate(prompt, params)
    }

    suspend fun generateComplete(prompt: String, params: GenerationParams = GenerationParams()): AppResult<String> {
        val engine = inferenceEngine()
            ?: return AppError.Llm.GenerationFailed("No inference model loaded").left()
        return engine.generateComplete(prompt, params)
    }

    suspend fun embed(text: String): AppResult<FloatArray> {
        val engine = embeddingEngine()
            ?: return AppError.Llm.GenerationFailed("No embedding model loaded").left()
        return engine.embed(text)
    }

    fun tokenCount(text: String): Int {
        return inferenceEngine()?.tokenCount(text)
            ?: embeddingEngine()?.tokenCount(text)
            ?: (text.length * 0.3).toInt().coerceAtLeast(1)
    }

    fun getEmbeddingDimension(): Int = embeddingEngine()?.getEmbeddingDimension() ?: 0

    fun getContextSize(): Int = inferenceEngine()?.getContextSize() ?: 0

    fun cancelGeneration() {
        inferenceEngine()?.cancelGeneration()
    }

    fun isInferenceReady(): Boolean = inferenceEngine()?.isModelLoaded() == true
    fun isEmbeddingReady(): Boolean = embeddingEngine()?.isModelLoaded() == true

    suspend fun handleMemoryPressure() {
        Logger.w(TAG, "Handling memory pressure — unloading embedding model if possible")
        val embId = _activeEmbeddingModelId.value
        if (embId != null) {
            val config = findModelById(embId) ?: return
            unloadModel(config)
        }
    }

    fun destroy() {
        llamaCppEngine.destroy()
        liteRtEngine.destroy()
    }
}
