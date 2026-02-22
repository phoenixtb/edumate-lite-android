package io.foxbird.edgeai.engine

import io.foxbird.edgeai.util.AppResult
import kotlinx.coroutines.flow.Flow

enum class EngineType {
    LLAMA_CPP,
    LITE_RT
}

data class EngineCapabilities(
    val supportsGpu: Boolean = false,
    val supportsNpu: Boolean = false,
    val supportsEmbedding: Boolean = true,
    val supportsStreaming: Boolean = true,
    val supportedFormats: Set<String> = emptySet()
)

data class GenerationParams(
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40
)

interface InferenceEngine {

    val engineType: EngineType
    val capabilities: EngineCapabilities
    val lastLoadError: String?

    suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean
    fun unloadModel()
    fun isModelLoaded(): Boolean

    fun generate(prompt: String, params: GenerationParams = GenerationParams()): Flow<String>

    suspend fun generateComplete(prompt: String, params: GenerationParams = GenerationParams()): AppResult<String>

    suspend fun embed(text: String): AppResult<FloatArray>

    fun tokenCount(text: String): Int
    fun getEmbeddingDimension(): Int
    fun getContextSize(): Int

    fun cancelGeneration()
    fun destroy()
}
