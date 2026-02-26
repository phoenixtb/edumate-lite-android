package io.foxbird.doclibrary.domain.rag

import io.foxbird.doclibrary.data.local.converter.Converters
import io.foxbird.doclibrary.data.repository.ChunkRepository
import kotlin.math.sqrt

class VectorSearchEngine(private val chunkRepository: ChunkRepository) {

    private val converters = Converters()

    suspend fun search(
        queryEmbedding: FloatArray,
        documentIds: List<Long>? = null,
        topK: Int = 3,
        threshold: Float = 0.0f
    ): List<SearchResult> {
        val chunks = if (documentIds != null && documentIds.isNotEmpty()) {
            chunkRepository.getEmbeddedChunksByDocuments(documentIds)
        } else {
            chunkRepository.getAllEmbeddedChunks()
        }

        return chunks.mapNotNull { chunk ->
            val embeddingBytes = chunk.embedding ?: return@mapNotNull null
            val embedding = converters.toFloatArray(embeddingBytes) ?: return@mapNotNull null
            val score = cosineSimilarity(queryEmbedding, embedding)
            if (score >= threshold) SearchResult(chunk, score) else null
        }
            .sortedByDescending { it.score }
            .take(topK)
    }

    companion object {
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var dot = 0f; var normA = 0f; var normB = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denom = sqrt(normA) * sqrt(normB)
            return if (denom > 0f) dot / denom else 0f
        }
    }
}
