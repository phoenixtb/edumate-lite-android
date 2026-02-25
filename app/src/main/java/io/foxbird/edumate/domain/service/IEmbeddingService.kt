package io.foxbird.edumate.domain.service

import io.foxbird.edgeai.util.AppResult

/**
 * Contract for text embedding. Implementations produce dense float vectors
 * suitable for semantic similarity / RAG retrieval.
 */
interface IEmbeddingService {
    suspend fun embed(text: String): AppResult<FloatArray>

    suspend fun embedBatch(
        texts: List<String>,
        batchSize: Int,
        onProgress: (suspend (done: Int, total: Int) -> Unit)? = null
    ): List<AppResult<FloatArray>>
}
