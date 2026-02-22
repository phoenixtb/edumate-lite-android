package io.foxbird.edgeai.engine

import arrow.core.left
import arrow.core.right
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import io.foxbird.edgeai.util.AppError
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps Google's LiteRT-LM for text generation and LiteRT CompiledModel for embeddings.
 * Supports .tflite, .task, .litertlm model formats with hardware acceleration (GPU/NPU).
 */
class LiteRtEngine : InferenceEngine {

    companion object {
        private const val TAG = "LiteRtEngine"
    }

    override val engineType = EngineType.LITE_RT
    override val capabilities = EngineCapabilities(
        supportsGpu = true,
        supportsNpu = true,
        supportsEmbedding = true,
        supportsStreaming = true,
        supportedFormats = setOf("tflite", "task", "litertlm")
    )

    private var lmEngine: Engine? = null
    private var embeddingModel: CompiledModel? = null
    private var modelLoaded = false
    private var currentModelPath: String? = null
    private var contextSize: Int = 4096

    @Volatile
    private var cancelled = false

    @Volatile
    override var lastLoadError: String? = null
        private set

    override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean =
        withContext(Dispatchers.IO) {
            lastLoadError = null
            try {
                val file = File(modelPath)
                if (!file.exists()) {
                    lastLoadError = "Model file not found: $modelPath"
                    return@withContext false
                }

                this@LiteRtEngine.contextSize = contextSize
                val ext = file.extension.lowercase()

                when (ext) {
                    "task", "litertlm" -> loadLmModel(modelPath)
                    "tflite" -> loadCompiledModel(modelPath)
                    else -> {
                        lastLoadError = "Unsupported format: $ext"
                        Logger.e(TAG, "Unsupported format: $ext")
                        false
                    }
                }
            } catch (e: Exception) {
                lastLoadError = e.message ?: e.toString()
                Logger.e(TAG, "Load failed", e)
                false
            }
        }

    private fun loadLmModel(path: String): Boolean {
        return try {
            val cacheDir = File(path).parent
            val config = EngineConfig(
                modelPath = path,
                backend = Backend.CPU,
                cacheDir = cacheDir
            )
            val engine = Engine(config)
            engine.initialize()

            lastLoadError = null
            lmEngine = engine
            currentModelPath = path
            modelLoaded = true
            Logger.i(TAG, "LiteRT-LM engine loaded: $path")
            true
        } catch (e: Exception) {
            lastLoadError = e.message ?: e.toString()
            Logger.e(TAG, "Failed to load LiteRT-LM model", e)
            false
        }
    }

    private fun loadCompiledModel(path: String): Boolean {
        return try {
            val model = CompiledModel.create(path, CompiledModel.Options(Accelerator.CPU))

            lastLoadError = null
            embeddingModel = model
            currentModelPath = path
            modelLoaded = true
            Logger.i(TAG, "LiteRT CompiledModel loaded: $path")
            true
        } catch (e: Exception) {
            lastLoadError = e.message ?: e.toString()
            Logger.e(TAG, "Failed to load LiteRT CompiledModel", e)
            false
        }
    }

    override fun unloadModel() {
        try {
            lmEngine?.close()
            embeddingModel?.close()
        } catch (e: Exception) {
            Logger.e(TAG, "Error during unload", e)
        }
        lastLoadError = null
        lmEngine = null
        embeddingModel = null
        modelLoaded = false
        currentModelPath = null
    }

    override fun isModelLoaded(): Boolean = modelLoaded

    override fun generate(prompt: String, params: GenerationParams): Flow<String> = flow {
        val engine = lmEngine ?: throw IllegalStateException("LiteRT-LM engine not loaded")

        cancelled = false

        try {
            val convConfig = ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = params.topK,
                    topP = params.topP.toDouble(),
                    temperature = params.temperature.toDouble()
                )
            )

            engine.createConversation(convConfig).use { conversation ->
                conversation.sendMessageAsync(prompt).collect { message ->
                    if (!cancelled) {
                        emit(message.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "LiteRT generation failed", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateComplete(prompt: String, params: GenerationParams): AppResult<String> =
        withContext(Dispatchers.IO) {
            val engine = lmEngine
                ?: return@withContext AppError.Llm.GenerationFailed("LiteRT-LM engine not loaded").left()

            try {
                val convConfig = ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = params.topK,
                        topP = params.topP.toDouble(),
                        temperature = params.temperature.toDouble()
                    )
                )

                val text = engine.createConversation(convConfig).use { conversation ->
                    val response = conversation.sendMessage(prompt)
                    response.toString()
                }

                text.right()
            } catch (e: Exception) {
                Logger.e(TAG, "LiteRT generation failed", e)
                AppError.Llm.GenerationFailed(e.message ?: "LiteRT generation error").left()
            }
        }

    override suspend fun embed(text: String): AppResult<FloatArray> = withContext(Dispatchers.IO) {
        val model = embeddingModel
            ?: return@withContext AppError.Llm.GenerationFailed("LiteRT embedding model not loaded").left()

        try {
            val inputBuffers = model.createInputBuffers()
            val outputBuffers = model.createOutputBuffers()

            inputBuffers[0].writeFloat(text.toByteArray().map { it.toFloat() }.toFloatArray())
            model.run(inputBuffers, outputBuffers)

            val embedding = outputBuffers[0].readFloat()
            embedding.right()
        } catch (e: Exception) {
            Logger.e(TAG, "LiteRT embedding failed", e)
            AppError.Llm.GenerationFailed(e.message ?: "Embedding error").left()
        }
    }

    override fun tokenCount(text: String): Int {
        return (text.length * 0.3).toInt().coerceAtLeast(1)
    }

    override fun getEmbeddingDimension(): Int {
        return if (embeddingModel != null) 256 else 0
    }

    override fun getContextSize(): Int = contextSize

    override fun cancelGeneration() {
        cancelled = true
    }

    override fun destroy() {
        unloadModel()
    }
}
