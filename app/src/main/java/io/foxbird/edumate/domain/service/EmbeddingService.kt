package io.foxbird.edumate.domain.service

import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants

/**
 * [IEmbeddingService] implementation backed by [EngineOrchestrator].
 *
 * Returns [AppResult] — callers must handle the Left (error) case explicitly.
 * Errors are no longer silently swallowed; the full [io.foxbird.edgeai.util.AppError]
 * is preserved so callers can surface meaningful messages to the user.
 */
class EmbeddingService(private val orchestrator: EngineOrchestrator) : IEmbeddingService {

    companion object {
        private const val TAG = "EmbeddingService"
    }

    override suspend fun embed(text: String): AppResult<FloatArray> = orchestrator.embed(text)

    override suspend fun embedBatch(
        texts: List<String>,
        batchSize: Int,
        onProgress: (suspend (Int, Int) -> Unit)?
    ): List<AppResult<FloatArray>> {
        val results = mutableListOf<AppResult<FloatArray>>()
        for (i in texts.indices step batchSize) {
            val batch = texts.subList(i, minOf(i + batchSize, texts.size))
            for (text in batch) {
                results.add(embed(text))
            }
            onProgress?.invoke(minOf(i + batchSize, texts.size), texts.size)
        }
        return results
    }
}
