package io.foxbird.edumate.domain.engine

import io.foxbird.edumate.data.local.converter.Converters
import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.entity.ChunkEntity
import kotlin.math.sqrt

data class SearchResult(
    val chunk: ChunkEntity,
    val score: Float
)

class VectorSearchEngine(private val chunkDao: ChunkDao) {

    private val converters = Converters()

    suspend fun search(
        queryEmbedding: FloatArray,
        materialIds: List<Long>? = null,
        topK: Int = 3,
        threshold: Float = 0.0f
    ): List<SearchResult> {
        val chunks = if (materialIds != null && materialIds.isNotEmpty()) {
            chunkDao.getEmbeddedChunksByMaterials(materialIds)
        } else {
            chunkDao.getAllEmbeddedChunks()
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
            var dot = 0f
            var normA = 0f
            var normB = 0f
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
