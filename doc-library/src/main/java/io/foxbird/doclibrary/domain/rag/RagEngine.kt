package io.foxbird.doclibrary.domain.rag

import io.foxbird.doclibrary.domain.engine.BM25Scorer
import io.foxbird.doclibrary.domain.engine.IChunkingEngine
import io.foxbird.edgeai.embedding.IEmbeddingService
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.flow.Flow

private const val RRF_K = 60

/**
 * Hybrid retrieval-augmented generation using Reciprocal Rank Fusion (RRF).
 *
 * Unlike simple re-ranking, both vector search and BM25 run independently over the full
 * candidate set. Their ranked lists are then merged with RRF so that a chunk ranking
 * highly in either modality can surface — not only those that passed a vector threshold.
 *
 * RRF score = Σ 1/(k + rank_i)  where k=60 (standard constant).
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
        /** Tokens reserved for system prompt + query + generation headroom. */
        private const val CONTEXT_OVERHEAD_TOKENS = 512
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

        // Load all candidates once — shared between vector and BM25
        val candidates = vectorSearch.loadCandidates(documentIds)
        if (candidates.isEmpty()) return RagContext(emptyList(), "")

        // --- Vector ranking ---
        val vectorRanked = vectorSearch.rankBySimilarity(queryEmbedding, candidates, threshold)

        // --- BM25 ranking over full candidate set ---
        val candidateContents = candidates.map { it.content }
        val bm25Scores = bm25Scorer.score(query, candidateContents)
        val bm25Ranked = candidates
            .mapIndexed { idx, chunk -> Pair(chunk, bm25Scores.getOrElse(idx) { 0f }) }
            .sortedByDescending { it.second }

        // --- Reciprocal Rank Fusion ---
        val chunkIdToRrfScore = mutableMapOf<Long, Float>()

        vectorRanked.forEachIndexed { rank, result ->
            val id = result.chunk.id
            chunkIdToRrfScore[id] = (chunkIdToRrfScore[id] ?: 0f) + 1f / (RRF_K + rank + 1)
        }
        bm25Ranked.forEachIndexed { rank, (chunk, _) ->
            val id = chunk.id
            chunkIdToRrfScore[id] = (chunkIdToRrfScore[id] ?: 0f) + 1f / (RRF_K + rank + 1)
        }

        val chunkById = candidates.associateBy { it.id }
        val rrfResults = chunkIdToRrfScore.entries
            .sortedByDescending { it.value }
            .take(topK)
            .mapNotNull { (id, score) -> chunkById[id]?.let { SearchResult(it, score) } }

        if (rrfResults.isEmpty()) return RagContext(emptyList(), "")

        // --- Token budget gating (dynamic: use model context size when available) ---
        val modelContextSize = orchestrator.getContextSize()
        val effectiveMaxContext = if (modelContextSize > 0) {
            (modelContextSize - CONTEXT_OVERHEAD_TOKENS).coerceAtLeast(config.maxContextTokens)
        } else {
            config.maxContextTokens
        }

        val contextBuilder = StringBuilder()
        val selectedChunks = mutableListOf<SearchResult>()
        var totalTokens = 0

        for (result in rrfResults) {
            val chunkTokens = chunkingEngine.countTokens(result.chunk.content)
            if (totalTokens + chunkTokens > effectiveMaxContext) break
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
            params = GenerationParams(maxTokens = maxTokens, temperature = temperature, topK = 20)
        )
    }

    private fun buildPrompt(contextText: String, query: String, history: String): String =
        buildString {
            append("System: ${config.systemPrompt}\n\n")
            if (contextText.isNotBlank()) append("Study Material:\n$contextText\n")
            if (history.isNotBlank()) append("\nConversation so far:\n$history\n")
            append("\nUser: $query\n\nAssistant:")
        }
}
