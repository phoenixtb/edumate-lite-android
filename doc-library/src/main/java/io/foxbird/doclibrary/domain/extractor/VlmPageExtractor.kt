package io.foxbird.doclibrary.domain.extractor

import android.graphics.Bitmap
import io.foxbird.doclibrary.config.FeatureFlags
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.engine.IVisionEngine

/**
 * Extracts text and semantic content from a page bitmap using the active vision-language model.
 *
 * Gated by [FeatureFlags.vlmExtractionEnabled] AND the active engine's [EngineOrchestrator.supportsVision].
 * When disabled, [isAvailable] returns false and [ExtractionStrategySelector] falls back to [OcrPageExtractor].
 *
 * To activate: set [FeatureFlags.vlmExtractionEnabled] = true and load a vision-capable engine
 * (see docs/vision-extraction-engine.md for the MediaPipe implementation spec).
 */
class VlmPageExtractor(
    private val visionEngine: IVisionEngine,
    private val featureFlags: FeatureFlags
) : IPageExtractor {

    override val name = "vlm"

    override fun isAvailable() =
        featureFlags.vlmExtractionEnabled && visionEngine.supportsVision()

    override suspend fun extract(bitmap: Bitmap): String {
        check(isAvailable()) { "VLM extraction not available — check FeatureFlags and engine vision support" }
        return visionEngine.generateWithImage(
            prompt = VLM_PROMPT,
            bitmap = bitmap,
            params = GenerationParams(maxTokens = 2048, temperature = 0.1f)
        ).fold(ifLeft = { "" }, ifRight = { it })
    }

    companion object {
        const val VLM_PROMPT =
            "You are an academic OCR and content analysis assistant. " +
            "Extract all text verbatim from this page image. " +
            "For every diagram, chart, graph, or illustration: describe its content, axes, labels, and key data points. " +
            "For mathematical expressions: write them out in plain text (e.g. 'x squared plus 2x plus 1'). " +
            "Preserve the logical reading order. Return plain text only — no markdown, no explanations."
    }
}
