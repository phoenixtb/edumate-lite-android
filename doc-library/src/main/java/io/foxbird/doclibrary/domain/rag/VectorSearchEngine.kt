package io.foxbird.doclibrary.domain.rag

import io.foxbird.doclibrary.data.local.converter.Converters
import io.foxbird.doclibrary.data.local.entity.ChunkEntity
import io.foxbird.doclibrary.data.repository.ChunkRepository
import io.foxbird.edgeai.util.Logger
import kotlin.math.sqrt

class VectorSearchEngine(private val chunkRepository: ChunkRepository) {

    companion object {
        private const val TAG = "VectorSearchEngine"

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

    private val converters = Converters()

    /** Load all embedded chunks for the given scope (null = entire library). */
    suspend fun loadCandidates(documentIds: List<Long>?): List<ChunkEntity> =
        if (documentIds != null && documentIds.isNotEmpty()) {
            chunkRepository.getEmbeddedChunksByDocuments(documentIds)
        } else {
            chunkRepository.getAllEmbeddedChunks()
        }

    /**
     * Ranks [candidates] by cosine similarity against [queryEmbedding].
     * Skips chunks whose stored embedding dimension mismatches the query — prevents
     * meaningless scores when the embedding model changes between ingestion and retrieval.
     */
    fun rankBySimilarity(
        queryEmbedding: FloatArray,
        candidates: List<ChunkEntity>,
        threshold: Float = 0.0f
    ): List<SearchResult> {
        var dimensionMismatches = 0
        val results = candidates.mapNotNull { chunk ->
            val embeddingBytes = chunk.embedding ?: return@mapNotNull null
            val embedding = converters.toFloatArray(embeddingBytes) ?: return@mapNotNull null
            if (embedding.size != queryEmbedding.size) {
                dimensionMismatches++
                return@mapNotNull null
            }
            val score = cosineSimilarity(queryEmbedding, embedding)
            if (score >= threshold) SearchResult(chunk, score) else null
        }.sortedByDescending { it.score }

        if (dimensionMismatches > 0) {
            Logger.w(
                TAG,
                "$dimensionMismatches chunk(s) skipped: embedding dimension mismatch " +
                "(query=${queryEmbedding.size}). Re-embed documents after switching embedding models."
            )
        }
        return results
    }

    /** Convenience: load candidates and rank in one call. */
    suspend fun search(
        queryEmbedding: FloatArray,
        documentIds: List<Long>? = null,
        topK: Int = 3,
        threshold: Float = 0.0f
    ): List<SearchResult> =
        rankBySimilarity(queryEmbedding, loadCandidates(documentIds), threshold).take(topK)
}
