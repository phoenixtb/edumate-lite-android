package io.foxbird.edgeai.engine

import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.util.AppResult

/**
 * Narrow interface for text-generation capability.
 *
 * Implemented by [EngineOrchestrator]. [ConceptExtractor] and similar domain services depend
 * on this interface rather than on the concrete orchestrator, making them reusable with any
 * inference backend that can generate text and report the active model's configuration.
 */
interface ITextGenerator {
    suspend fun generateComplete(prompt: String, params: GenerationParams = GenerationParams()): AppResult<String>

    /**
     * Returns the [ModelConfig] of the currently active inference model, or null if no model
     * is loaded. Used by callers that need model-specific behaviour (e.g. think-tag stripping).
     */
    fun findActiveModelConfig(): ModelConfig?
}
