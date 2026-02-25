package io.foxbird.edgeai.engine

import io.foxbird.edgeai.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Contract for text-generation engines (streaming + complete).
 * Does NOT include embedding — see [EmbeddingEngine].
 */
interface GenerationEngine {

    val engineType: EngineType
    val capabilities: EngineCapabilities
    val lastLoadError: String?

    suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean
    fun unloadModel()
    fun isModelLoaded(): Boolean

    fun generate(prompt: String, params: GenerationParams = GenerationParams()): Flow<String>
    suspend fun generateComplete(prompt: String, params: GenerationParams = GenerationParams()): AppResult<String>

    fun tokenCount(text: String): Int
    fun getContextSize(): Int

    fun cancelGeneration()
    fun destroy()
}

data class EngineCapabilities(
    val supportsGpu: Boolean = false,
    val supportsNpu: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportedFormats: Set<String> = emptySet()
)

data class GenerationParams(
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40
)
