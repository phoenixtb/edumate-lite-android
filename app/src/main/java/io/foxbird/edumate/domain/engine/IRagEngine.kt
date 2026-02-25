package io.foxbird.edumate.domain.engine

import io.foxbird.edgeai.engine.GenerationParams
import kotlinx.coroutines.flow.Flow

interface IRagEngine {
    suspend fun retrieve(
        query: String,
        materialIds: List<Long>? = null,
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
