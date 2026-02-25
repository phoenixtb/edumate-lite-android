package io.foxbird.edgeai.engine

import arrow.core.left
import arrow.core.right
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import io.foxbird.edgeai.util.AppError
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps llama.cpp via Llamatik for GGUF model inference.
 *
 * Implements both [GenerationEngine] and [EmbeddingEngine] since the llama.cpp native
 * library handles tokenization internally for GGUF models — no external tokenizer needed.
 */
class LlamaCppEngine : GenerationEngine, EmbeddingEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"
    }

    // --- GenerationEngine ---

    override val engineType = EngineType.LLAMA_CPP
    override val capabilities = EngineCapabilities(
        supportsGpu = false,
        supportsNpu = false,
        supportsStreaming = true,
        supportedFormats = setOf("gguf")
    )

    @Volatile
    override var lastLoadError: String? = null
        private set

    private var generationModelLoaded = false
    private var embeddingModelLoaded = false
    private var contextSize: Int = 4096

    // --- GenerationEngine.loadModel ---
    override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean =
        withContext(Dispatchers.IO) {
            lastLoadError = null
            try {
                val file = File(modelPath)
                if (!file.exists() || !file.canRead()) {
                    lastLoadError = "Model file not found or unreadable: $modelPath"
                    return@withContext false
                }

                this@LlamaCppEngine.contextSize = contextSize

                val genOk = LlamaBridge.initGenerateModel(modelPath)
                if (!genOk) {
                    val embOk = LlamaBridge.initModel(modelPath)
                    if (!embOk) {
                        lastLoadError = "Llamatik failed to load model: $modelPath"
                        return@withContext false
                    }
                    embeddingModelLoaded = true
                } else {
                    generationModelLoaded = true
                    try {
                        LlamaBridge.initModel(modelPath)
                        embeddingModelLoaded = true
                    } catch (_: Exception) {
                        // Embedding init optional for generation-only models
                    }
                }

                Logger.i(TAG, "Llamatik model loaded: $modelPath")
                true
            } catch (e: Exception) {
                lastLoadError = e.message ?: e.toString()
                Logger.e(TAG, "Load failed", e)
                false
            }
        }

    override fun unloadModel() {
        try { LlamaBridge.shutdown() } catch (e: Exception) { Logger.e(TAG, "Error during unload", e) }
        lastLoadError = null
        generationModelLoaded = false
        embeddingModelLoaded = false
    }

    override fun isModelLoaded(): Boolean = generationModelLoaded || embeddingModelLoaded

    override fun generate(prompt: String, params: GenerationParams): Flow<String> = callbackFlow {
        if (!generationModelLoaded) {
            close(IllegalStateException("GGUF generation model not loaded"))
            return@callbackFlow
        }
        LlamaBridge.updateGenerateParams(
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            topP = params.topP,
            topK = params.topK,
            repeatPenalty = 1.1f
        )
        LlamaBridge.generateStream(prompt, object : GenStream {
            override fun onDelta(text: String) { trySend(text) }
            override fun onComplete() { close() }
            override fun onError(message: String) { close(Exception(message)) }
        })
        awaitClose { LlamaBridge.nativeCancelGenerate() }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateComplete(prompt: String, params: GenerationParams): AppResult<String> =
        withContext(Dispatchers.IO) {
            if (!generationModelLoaded) {
                return@withContext AppError.Llm.GenerationFailed("GGUF generation model not loaded").left()
            }
            try {
                LlamaBridge.updateGenerateParams(
                    temperature = params.temperature,
                    maxTokens = params.maxTokens,
                    topP = params.topP,
                    topK = params.topK,
                    repeatPenalty = 1.1f
                )
                LlamaBridge.generate(prompt).right()
            } catch (e: Exception) {
                Logger.e(TAG, "Generation failed", e)
                AppError.Llm.GenerationFailed(e.message ?: "Unknown error").left()
            }
        }

    override fun tokenCount(text: String): Int = (text.length * 0.3).toInt().coerceAtLeast(1)

    override fun getContextSize(): Int = contextSize

    override fun cancelGeneration() { LlamaBridge.nativeCancelGenerate() }

    // --- EmbeddingEngine.loadModel (tokenizerPath ignored — llama.cpp tokenizes internally) ---
    override suspend fun loadModel(modelPath: String, contextSize: Int, tokenizerPath: String?): Boolean =
        loadModel(modelPath, contextSize, 0)

    override suspend fun embed(text: String): AppResult<FloatArray> = withContext(Dispatchers.IO) {
        if (!embeddingModelLoaded) {
            return@withContext AppError.Llm.GenerationFailed("GGUF embedding model not loaded").left()
        }
        try {
            LlamaBridge.embed(text).right()
        } catch (e: Exception) {
            Logger.e(TAG, "Embed failed", e)
            AppError.Llm.GenerationFailed(e.message ?: "Embedding error").left()
        }
    }

    override fun getEmbeddingDimension(): Int = 0

    override fun destroy() { unloadModel() }
}
