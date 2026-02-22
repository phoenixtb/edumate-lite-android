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
 * Supports text generation (streaming + complete) and embeddings.
 */
class LlamaCppEngine : InferenceEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"
    }

    override val engineType = EngineType.LLAMA_CPP
    override val capabilities = EngineCapabilities(
        supportsGpu = false,
        supportsNpu = false,
        supportsEmbedding = true,
        supportsStreaming = true,
        supportedFormats = setOf("gguf")
    )

    @Volatile
    override var lastLoadError: String? = null
        private set

    private var generationModelLoaded = false
    private var embeddingModelLoaded = false
    private var currentModelPath: String? = null
    private var contextSize: Int = 4096

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

                currentModelPath = modelPath
                Logger.i(TAG, "Llamatik model loaded: $modelPath")
                true
            } catch (e: Exception) {
                lastLoadError = e.message ?: e.toString()
                Logger.e(TAG, "Load failed", e)
                false
            }
        }

    override fun unloadModel() {
        try {
            LlamaBridge.shutdown()
        } catch (e: Exception) {
            Logger.e(TAG, "Error during unload", e)
        }
        lastLoadError = null
        generationModelLoaded = false
        embeddingModelLoaded = false
        currentModelPath = null
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
            override fun onDelta(text: String) {
                trySend(text)
            }
            override fun onComplete() {
                close()
            }
            override fun onError(message: String) {
                close(Exception(message))
            }
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
                val result = LlamaBridge.generate(prompt)
                result.right()
            } catch (e: Exception) {
                Logger.e(TAG, "Generation failed", e)
                AppError.Llm.GenerationFailed(e.message ?: "Unknown error").left()
            }
        }

    override suspend fun embed(text: String): AppResult<FloatArray> = withContext(Dispatchers.IO) {
        if (!embeddingModelLoaded) {
            return@withContext AppError.Llm.GenerationFailed("GGUF embedding model not loaded").left()
        }

        try {
            val embedding = LlamaBridge.embed(text)
            embedding.right()
        } catch (e: Exception) {
            Logger.e(TAG, "Embed failed", e)
            AppError.Llm.GenerationFailed(e.message ?: "Embedding error").left()
        }
    }

    override fun tokenCount(text: String): Int {
        return (text.length * 0.3).toInt().coerceAtLeast(1)
    }

    override fun getEmbeddingDimension(): Int = 0

    override fun getContextSize(): Int = contextSize

    override fun cancelGeneration() {
        LlamaBridge.nativeCancelGenerate()
    }

    override fun destroy() {
        unloadModel()
    }
}
