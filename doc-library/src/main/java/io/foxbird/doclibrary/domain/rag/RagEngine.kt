package io.foxbird.doclibrary.domain.rag

import io.foxbird.doclibrary.domain.engine.BM25Scorer
import io.foxbird.doclibrary.domain.engine.IChunkingEngine
import io.foxbird.edgeai.embedding.IEmbeddingService
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.flow.Flow

/**
 * Hybrid retrieval-augmented generation: vector similarity + BM25 keyword scoring.
 *
 * The [config] is provided by the host app, allowing each app to inject its own
 * system prompt / persona without changing this implementation.
 */
class RagEngine(
    private val vectorSearch: VectorSearchEngine,
    private val bm25Scorer: BM25Scorer,
    private val embeddingService: IEmbeddingService,
    private val orchestrator: EngineOrchestrator,
    private val chunkingEngine: IChunkingEngine,
    private val config: RagConfig = RagConfig()
) : IRagEngine {

    companion object {
        private const val TAG = "RagEngine"
    }

    override suspend fun retrieve(
        query: String,
        documentIds: List<Long>?,
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
            documentIds = documentIds,
            topK = topK * 2,
            threshold = threshold
        )

        if (vectorResults.isEmpty()) return RagContext(emptyList(), "")

        val documents = vectorResults.map { it.chunk.content }
        val bm25Scores = bm25Scorer.score(query, documents)

        val hybridResults = vectorResults.mapIndexed { index, result ->
            val hybridScore = config.vectorWeight * result.score +
                    config.bm25Weight * (bm25Scores.getOrElse(index) { 0f })
            result.copy(score = hybridScore)
        }.sortedByDescending { it.score }.take(topK)

        val contextBuilder = StringBuilder()
        val selectedChunks = mutableListOf<SearchResult>()
        var totalTokens = 0

        for (result in hybridResults) {
            val chunkTokens = chunkingEngine.countTokens(result.chunk.content)
            if (totalTokens + chunkTokens > config.maxContextTokens) break
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
        val prompt = buildPrompt(context.contextText, query, conversationHistory)
        return orchestrator.generate(
            prompt = prompt,
            params = GenerationParams(
                maxTokens = maxTokens,
                temperature = temperature,
                topK = 20
            )
        )
    }

    private fun buildPrompt(contextText: String, query: String, history: String): String =
        buildString {
            append("System: ${config.systemPrompt}\n\n")
            append("Study Material:\n$contextText\n")
            if (history.isNotBlank()) append("\nConversation so far:\n$history\n")
            append("\nUser: $query\n\nAssistant:")
        }
}
