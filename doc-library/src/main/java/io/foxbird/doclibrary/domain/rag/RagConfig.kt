package io.foxbird.doclibrary.domain.rag

/**
 * Configuration injected into [RagEngine] by the host app.
 * Each app provides its own [systemPrompt] to define the AI persona.
 *
 * [maxContextTokens] is the minimum guaranteed context budget. The engine will use
 * the model's actual context size minus overhead when that is larger.
 */
data class RagConfig(
    val systemPrompt: String = "You are a helpful AI assistant. Answer questions based on the provided context only.",
    val retrievalTopK: Int = 5,
    val similarityThreshold: Float = 0.2f,
    val maxContextTokens: Int = 4096,
    val bm25Weight: Float = 0.3f,
    val vectorWeight: Float = 0.7f
)
