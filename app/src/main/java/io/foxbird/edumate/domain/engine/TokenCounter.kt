package io.foxbird.edumate.domain.engine

import io.foxbird.edgeai.engine.EngineOrchestrator

/**
 * Counts tokens in a string. Abstracted so [ChunkingEngine] does not couple directly
 * to [EngineOrchestrator] — the counter can be swapped or bypassed in tests.
 */
fun interface TokenCounter {
    fun count(text: String): Int
}

/** Delegates to the active inference engine; falls back to character estimation if no model is loaded. */
class OrchestratorTokenCounter(private val orchestrator: EngineOrchestrator) : TokenCounter {
    override fun count(text: String): Int {
        val count = try { orchestrator.tokenCount(text) } catch (_: Exception) { 0 }
        return if (count > 0) count else FallbackTokenCounter.count(text)
    }
}

/** Pure character-based approximation (~0.3 tokens/char for English). No model required. */
object FallbackTokenCounter : TokenCounter {
    override fun count(text: String): Int = (text.length * 0.3).toInt().coerceAtLeast(1)
}
