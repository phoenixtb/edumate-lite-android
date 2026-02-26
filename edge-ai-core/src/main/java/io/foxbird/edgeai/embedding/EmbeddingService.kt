package io.foxbird.edgeai.embedding

import arrow.core.right
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger

/**
 * [IEmbeddingService] implementation backed by [EngineOrchestrator].
 *
 * Returns [AppResult] — callers must handle the Left (error) case explicitly.
 */
class EmbeddingService(private val orchestrator: EngineOrchestrator) : IEmbeddingService {

    companion object {
        private const val TAG = "EmbeddingService"
        private const val DEFAULT_BATCH_SIZE = 20
    }

    override suspend fun embed(text: String): AppResult<FloatArray> = orchestrator.embed(text)

    override suspend fun embedBatch(
        texts: List<String>,
        batchSize: Int,
        onProgress: (suspend (Int, Int) -> Unit)?
    ): List<AppResult<FloatArray>> {
        if (texts.isEmpty()) return emptyList()

        val results = mutableListOf<AppResult<FloatArray>>()
        for (i in texts.indices step batchSize) {
            val batch = texts.subList(i, minOf(i + batchSize, texts.size))
            val batchResult = orchestrator.embedBatch(batch)
            batchResult.fold(
                ifLeft = { error ->
                    Logger.w(TAG, "Batch embed failed for chunk $i–${i + batch.size}: ${error.message}, falling back")
                    batch.forEach { results.add(embed(it)) }
                },
                ifRight = { embeddings -> embeddings.forEach { results.add(it.right()) } }
            )
            onProgress?.invoke(minOf(i + batchSize, texts.size), texts.size)
        }
        return results
    }
}
