package io.foxbird.edgeai.engine

import arrow.core.left
import arrow.core.right
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
 * [GenerationEngine] backed by LiteRT-LM for `.litertlm` / `.task` model formats.
 * Hardware-accelerated text generation (GPU/NPU).
 * Does NOT handle embeddings — see [LiteRtEmbeddingEngine].
 */
class LiteRtGenerationEngine : GenerationEngine {

    companion object {
        private const val TAG = "LiteRtGenerationEngine"
    }

    override val engineType = EngineType.LITE_RT
    override val capabilities = EngineCapabilities(
        supportsGpu = true,
        supportsNpu = true,
        supportsStreaming = true,
        supportedFormats = setOf("task", "litertlm")
    )

    @Volatile
    override var lastLoadError: String? = null
        private set

    private var lmEngine: Engine? = null
    private var loaded = false
    private var contextSize: Int = 4096

    @Volatile
    private var cancelled = false

    override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean =
        withContext(Dispatchers.IO) {
            lastLoadError = null
            try {
                val file = File(modelPath)
                if (!file.exists()) {
                    lastLoadError = "Model file not found: $modelPath"
                    return@withContext false
                }
                val ext = file.extension.lowercase()
                if (ext !in setOf("task", "litertlm")) {
                    lastLoadError = "Unsupported format for LiteRT generation: $ext"
                    return@withContext false
                }

                this@LiteRtGenerationEngine.contextSize = contextSize
                val cacheDir = file.parent
                val config = EngineConfig(modelPath = modelPath, backend = Backend.CPU, cacheDir = cacheDir)
                val engine = Engine(config)
                engine.initialize()

                lmEngine = engine
                loaded = true
                lastLoadError = null
                Logger.i(TAG, "LiteRT-LM engine loaded: $modelPath")
                true
            } catch (e: Exception) {
                lastLoadError = e.message ?: e.toString()
                Logger.e(TAG, "Load failed", e)
                false
            }
        }

    override fun unloadModel() {
        try { lmEngine?.close() } catch (e: Exception) { Logger.e(TAG, "Error unloading LM", e) }
        lmEngine = null
        loaded = false
        lastLoadError = null
    }

    override fun isModelLoaded(): Boolean = loaded

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
                    if (!cancelled) emit(message.toString())
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Generation failed", e)
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
                    conversation.sendMessage(prompt).toString()
                }
                text.right()
            } catch (e: Exception) {
                Logger.e(TAG, "Generation failed", e)
                AppError.Llm.GenerationFailed(e.message ?: "LiteRT generation error").left()
            }
        }

    override fun tokenCount(text: String): Int = (text.length * 0.3).toInt().coerceAtLeast(1)

    override fun getContextSize(): Int = contextSize

    override fun cancelGeneration() { cancelled = true }

    override fun destroy() { unloadModel() }
}
