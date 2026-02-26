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
 * Routes inference/embedding requests to the appropriate engine.
 *
 * Generation engines ([GenerationEngine]) and embedding engines ([EmbeddingEngine]) are held
 * in separate maps keyed by [EngineType]. This eliminates the need for any engine-type-specific
 * branching (previously required by the monolithic LiteRtEngine).
 *
 * One inference model and one embedding model may be loaded simultaneously.
 */
class EngineOrchestrator(
    private val llamaCppEngine: LlamaCppEngine,
    private val liteRtGenEngine: LiteRtGenerationEngine,
    private val liteRtEmbEngine: LiteRtEmbeddingEngine,
    private val memoryMonitor: MemoryMonitor
) {
    companion object {
        private const val TAG = "EngineOrchestrator"
    }

    private val loadMutex = Mutex()

    private val generationEngines: Map<EngineType, GenerationEngine> = mapOf(
        EngineType.LLAMA_CPP to llamaCppEngine,
        EngineType.LITE_RT to liteRtGenEngine
    )

    private val embeddingEngines: Map<EngineType, EmbeddingEngine> = mapOf(
        EngineType.LLAMA_CPP to llamaCppEngine,
        EngineType.LITE_RT to liteRtEmbEngine
    )

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

    fun registerModels(models: List<ModelConfig>) { allModels = models }

    fun findModelById(id: String): ModelConfig? = allModels.find { it.id == id }

    fun updateModelState(modelId: String, state: ModelState) {
        _modelStates.value = _modelStates.value.toMutableMap().apply { this[modelId] = state }
    }

    // -------------------------------------------------------------------------
    // Load / Unload
    // -------------------------------------------------------------------------

    suspend fun loadModel(
        config: ModelConfig,
        modelPath: String,
        threads: Int,
        tokenizerPath: String? = null
    ): Boolean = loadMutex.withLock {
        val requiredMb = config.fileSizeMB.toLong()
        if (!memoryMonitor.canAllocateMb(requiredMb)) {
            Logger.e(TAG, "Insufficient memory for ${config.name} (need ${requiredMb}MB)")
            updateModelState(config.id, ModelState.LoadFailed("Insufficient memory"))
            return false
        }

        updateModelState(config.id, ModelState.Loading)

        val success = when (config.purpose) {
            ModelPurpose.INFERENCE -> {
                val engine = generationEngines[config.engineType]
                    ?: return failLoad(config, "No generation engine for ${config.engineType}")
                // Unload any currently active inference model on the same engine
                if (_activeInferenceEngine.value == config.engineType) {
                    engine.unloadModel()
                    _activeInferenceModelId.value?.let { updateModelState(it, ModelState.Downloaded) }
                }
                engine.loadModel(modelPath, config.contextLength, threads)
            }
            ModelPurpose.EMBEDDING -> {
                val engine = embeddingEngines[config.engineType]
                    ?: return failLoad(config, "No embedding engine for ${config.engineType}")
                // Unload any currently active embedding model on the same engine
                if (_activeEmbeddingEngine.value == config.engineType) {
                    engine.unloadModel()
                    _activeEmbeddingModelId.value?.let { updateModelState(it, ModelState.Downloaded) }
                }
                engine.loadModel(modelPath, config.contextLength, tokenizerPath)
            }
        }

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

        val errorMsg = when (config.purpose) {
            ModelPurpose.INFERENCE -> generationEngines[config.engineType]?.lastLoadError
            ModelPurpose.EMBEDDING -> embeddingEngines[config.engineType]?.lastLoadError
        } ?: "Engine failed to load model"

        updateModelState(config.id, ModelState.LoadFailed(errorMsg))
        return false
    }

    private fun failLoad(config: ModelConfig, reason: String): Boolean {
        updateModelState(config.id, ModelState.LoadFailed(reason))
        return false
    }

    fun unloadModel(config: ModelConfig) {
        when (config.purpose) {
            ModelPurpose.INFERENCE -> {
                generationEngines[config.engineType]?.unloadModel()
                if (_activeInferenceModelId.value == config.id) {
                    _activeInferenceEngine.value = null
                    _activeInferenceModelId.value = null
                }
            }
            ModelPurpose.EMBEDDING -> {
                embeddingEngines[config.engineType]?.unloadModel()
                if (_activeEmbeddingModelId.value == config.id) {
                    _activeEmbeddingEngine.value = null
                    _activeEmbeddingModelId.value = null
                }
            }
        }
        updateModelState(config.id, ModelState.Downloaded)
    }

    // -------------------------------------------------------------------------
    // Inference / Embedding delegation
    // -------------------------------------------------------------------------

    fun generate(prompt: String, params: GenerationParams = GenerationParams()): Flow<String> {
        val engine = _activeInferenceEngine.value?.let { generationEngines[it] }
            ?: throw IllegalStateException("No inference model loaded")
        return engine.generate(prompt, params)
    }

    suspend fun generateComplete(
        prompt: String,
        params: GenerationParams = GenerationParams()
    ): AppResult<String> {
        val engine = _activeInferenceEngine.value?.let { generationEngines[it] }
            ?: return AppError.Llm.GenerationFailed("No inference model loaded").left()
        return engine.generateComplete(prompt, params)
    }

    suspend fun embed(text: String): AppResult<FloatArray> {
        val engine = _activeEmbeddingEngine.value?.let { embeddingEngines[it] }
            ?: return AppError.Llm.GenerationFailed("No embedding model loaded").left()
        return engine.embed(text)
    }

    suspend fun embedBatch(texts: List<String>): AppResult<List<FloatArray>> {
        val engine = _activeEmbeddingEngine.value?.let { embeddingEngines[it] }
            ?: return AppError.Llm.GenerationFailed("No embedding model loaded").left()
        return engine.embedBatch(texts)
    }

    fun tokenCount(text: String): Int {
        return _activeInferenceEngine.value?.let { generationEngines[it]?.tokenCount(text) }
            ?: (text.length * 0.3).toInt().coerceAtLeast(1)
    }

    fun getEmbeddingDimension(): Int =
        _activeEmbeddingEngine.value?.let { embeddingEngines[it]?.getEmbeddingDimension() } ?: 0

    fun getContextSize(): Int =
        _activeInferenceEngine.value?.let { generationEngines[it]?.getContextSize() } ?: 0

    fun cancelGeneration() {
        _activeInferenceEngine.value?.let { generationEngines[it]?.cancelGeneration() }
    }

    fun isInferenceReady(): Boolean =
        _activeInferenceEngine.value?.let { generationEngines[it]?.isModelLoaded() } == true

    fun isEmbeddingReady(): Boolean =
        _activeEmbeddingEngine.value?.let { embeddingEngines[it]?.isModelLoaded() } == true

    suspend fun handleMemoryPressure() {
        Logger.w(TAG, "Memory pressure — unloading embedding model")
        val embId = _activeEmbeddingModelId.value ?: return
        val config = findModelById(embId) ?: return
        unloadModel(config)
    }

    fun destroy() {
        llamaCppEngine.destroy()
        liteRtGenEngine.destroy()
        liteRtEmbEngine.destroy()
    }
}
