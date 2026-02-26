package io.foxbird.doclibrary.domain.rag

import io.foxbird.doclibrary.data.local.entity.ChunkEntity
import kotlinx.coroutines.flow.Flow

data class SearchResult(
    val chunk: ChunkEntity,
    val score: Float
)

data class RagContext(
    val chunks: List<SearchResult>,
    val contextText: String
)

interface IRagEngine {
    suspend fun retrieve(
        query: String,
        documentIds: List<Long>? = null,
        topK: Int,
        threshold: Float
    ): RagContext

    fun generateStream(
        query: String,
        context: RagContext,
        conversationHistory: String = "",
        maxTokens: Int,
        temperature: Float
    ): Flow<String>
}
