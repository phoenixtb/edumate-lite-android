package io.foxbird.edumate.domain.engine

import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.domain.service.IEmbeddingService
import io.foxbird.edumate.domain.service.PromptTemplates
import kotlinx.coroutines.flow.Flow

data class RagContext(
    val chunks: List<SearchResult>,
    val contextText: String
)

/**
 * Hybrid retrieval-augmented generation: vector similarity + BM25 keyword scoring.
 *
 * Uses [IEmbeddingService] rather than [EngineOrchestrator] directly, keeping
 * the retrieval path decoupled from the engine implementation.
 */
class RagEngine(
    private val vectorSearch: VectorSearchEngine,
    private val bm25Scorer: BM25Scorer,
    private val embeddingService: IEmbeddingService,
    private val orchestrator: EngineOrchestrator,
    private val chunkingEngine: IChunkingEngine
) : IRagEngine {

    companion object {
        private const val TAG = "RagEngine"
    }

    override suspend fun retrieve(
        query: String,
        materialIds: List<Long>?,
        topK: Int,
        threshold: Float
    ): RagContext {
        val embeddingResult = embeddingService.embed(query)
        val queryEmbedding = embeddingResult.fold(
            ifLeft = { error ->
                Logger.e(TAG, "Failed to embed query: ${error.message}")
                return RagContext(emptyList(), "")
            },
            ifRight = { it }
        )

        val vectorResults = vectorSearch.search(
            queryEmbedding = queryEmbedding,
            materialIds = materialIds,
            topK = topK * 2,
            threshold = threshold
        )

        if (vectorResults.isEmpty()) return RagContext(emptyList(), "")

        val documents = vectorResults.map { it.chunk.content }
        val bm25Scores = bm25Scorer.score(query, documents)

        val hybridResults = vectorResults.mapIndexed { index, result ->
            val hybridScore = AppConstants.BM25_VECTOR_WEIGHT * result.score +
                    AppConstants.BM25_KEYWORD_WEIGHT * (bm25Scores.getOrElse(index) { 0f })
            result.copy(score = hybridScore)
        }.sortedByDescending { it.score }.take(topK)

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

    override fun generateStream(
        query: String,
        context: RagContext,
        conversationHistory: String,
        maxTokens: Int,
        temperature: Float
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
