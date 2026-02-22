package io.foxbird.edumate.domain.engine

import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.domain.service.EmbeddingService
import io.foxbird.edumate.domain.service.PromptTemplates
import kotlinx.coroutines.flow.Flow

data class RagContext(
    val chunks: List<SearchResult>,
    val contextText: String
)

class RagEngine(
    private val vectorSearch: VectorSearchEngine,
    private val bm25Scorer: BM25Scorer,
    private val embeddingService: EmbeddingService,
    private val orchestrator: EngineOrchestrator,
    private val chunkingEngine: ChunkingEngine
) {
    companion object {
        private const val TAG = "RagEngine"
    }

    suspend fun retrieve(
        query: String,
        materialIds: List<Long>? = null,
        topK: Int = AppConstants.RETRIEVAL_TOP_K,
        threshold: Float = AppConstants.SIMILARITY_THRESHOLD
    ): RagContext {
        val queryEmbedding = embeddingService.embed(query)
        if (queryEmbedding == null) {
            Logger.e(TAG, "Failed to embed query")
            return RagContext(emptyList(), "")
        }

        val vectorResults = vectorSearch.search(
            queryEmbedding = queryEmbedding,
            materialIds = materialIds,
            topK = topK * 2,
            threshold = threshold
        )

        if (vectorResults.isEmpty()) {
            return RagContext(emptyList(), "")
        }

        val documents = vectorResults.map { it.chunk.content }
        val bm25Scores = bm25Scorer.score(query, documents)

        val hybridResults = vectorResults.mapIndexed { index, result ->
            val hybridScore = AppConstants.BM25_VECTOR_WEIGHT * result.score +
                    AppConstants.BM25_KEYWORD_WEIGHT * (bm25Scores.getOrElse(index) { 0f })
            result.copy(score = hybridScore)
        }
            .sortedByDescending { it.score }
            .take(topK)

        val contextBuilder = StringBuilder()
        val selectedChunks = mutableListOf<SearchResult>()
        var totalTokens = 0

        for (result in hybridResults) {
            val chunkTokens = chunkingEngine.countTokens(result.chunk.content)
            if (totalTokens + chunkTokens > AppConstants.MAX_CONTEXT_TOKENS) break
            contextBuilder.append("[Source ${selectedChunks.size + 1}]\n")
            contextBuilder.append(result.chunk.content)
            contextBuilder.append("\n\n")
            selectedChunks.add(result)
            totalTokens += chunkTokens
        }

        return RagContext(selectedChunks, contextBuilder.toString().trim())
    }

    fun generateStream(
        query: String,
        context: RagContext,
        conversationHistory: String = "",
        maxTokens: Int = AppConstants.MAX_INFERENCE_TOKENS,
        temperature: Float = AppConstants.INFERENCE_TEMPERATURE
    ): Flow<String> {
        val prompt = PromptTemplates.qaPrompt(
            context = context.contextText,
            query = query,
            conversationHistory = conversationHistory
        )
        return orchestrator.generate(
            prompt = prompt,
            params = GenerationParams(
                maxTokens = maxTokens,
                temperature = temperature,
                topK = AppConstants.INFERENCE_TOP_K
            )
        )
    }
}
