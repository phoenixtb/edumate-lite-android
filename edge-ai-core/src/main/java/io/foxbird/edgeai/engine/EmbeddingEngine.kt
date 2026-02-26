package io.foxbird.edgeai.engine

import arrow.core.right
import io.foxbird.edgeai.util.AppResult

/**
 * Contract for embedding engines.
 * Completely separate from [GenerationEngine] — an engine may implement one or both.
 */
interface EmbeddingEngine {

    val engineType: EngineType
    val lastLoadError: String?

    /**
     * @param tokenizerPath Path to the tokenizer/vocab file required by this model (e.g. sentencepiece.model).
     *                      Null if the model bundles its own tokenizer (e.g. llama.cpp GGUF).
     */
    suspend fun loadModel(modelPath: String, contextSize: Int, tokenizerPath: String? = null): Boolean
    fun unloadModel()
    fun isModelLoaded(): Boolean

    suspend fun embed(text: String): AppResult<FloatArray>

    /** Send all texts in one native model call — significantly faster than looping [embed]. */
    suspend fun embedBatch(texts: List<String>): AppResult<List<FloatArray>> =
        texts.map { embed(it) }.let { results ->
            val embeddings = results.mapNotNull { it.getOrNull() }
            embeddings.right()
        }

    fun getEmbeddingDimension(): Int

    fun destroy()
}
