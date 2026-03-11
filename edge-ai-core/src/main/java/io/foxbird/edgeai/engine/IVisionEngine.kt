package io.foxbird.edgeai.engine

import android.graphics.Bitmap
import io.foxbird.edgeai.util.AppResult

/**
 * Narrow interface for vision-capable inference engines.
 *
 * Implemented by [EngineOrchestrator]. [VlmPageExtractor] depends on this interface rather
 * than on the concrete orchestrator class, so it is reusable with any engine infrastructure
 * that can answer these two questions.
 */
interface IVisionEngine {
    fun supportsVision(): Boolean
    suspend fun generateWithImage(
        prompt: String,
        bitmap: Bitmap,
        params: GenerationParams = GenerationParams()
    ): AppResult<String>
}
