package io.foxbird.edumate.domain.service

import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants

class EmbeddingService(private val orchestrator: EngineOrchestrator) {

    companion object {
        private const val TAG = "EmbeddingService"
    }

    suspend fun embed(text: String): FloatArray? {
        return orchestrator.embed(text).fold(
            ifLeft = { error ->
                Logger.e(TAG, "Embedding failed: ${error.message}")
                null
            },
            ifRight = { it }
        )
    }

    suspend fun embedBatch(
        texts: List<String>,
        batchSize: Int = AppConstants.EMBEDDING_BATCH_SIZE,
        onProgress: (suspend (Int, Int) -> Unit)? = null
    ): List<FloatArray?> {
        val results = mutableListOf<FloatArray?>()
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
