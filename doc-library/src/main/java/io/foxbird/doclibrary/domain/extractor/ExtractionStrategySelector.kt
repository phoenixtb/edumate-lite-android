package io.foxbird.doclibrary.domain.extractor

import io.foxbird.doclibrary.domain.adapter.ExtractedPage
import io.foxbird.doclibrary.domain.processor.ProcessingMode

/**
 * Decides which [IPageExtractor] to use for a given page, based on text quality and processing mode.
 *
 * Decision logic:
 * - If the page already has good text (not scanned, ≥ [minWordsForGoodText] words): return null
 *   (caller skips extraction and uses the existing PDFBox text as-is).
 * - FAST mode, poor/no text: [OcrPageExtractor] (fast, no model required).
 * - THOROUGH mode, poor/no text, VLM available: [VlmPageExtractor] (semantic + OCR via vision model).
 * - THOROUGH mode, poor/no text, VLM unavailable: [OcrPageExtractor].
 *
 * @param minWordsForGoodText Minimum word count for a page to be considered "text-rich enough" to
 *   skip extraction. Default 20. Tune this per domain (e.g. lower for children's books, higher for
 *   legal documents).
 */
class ExtractionStrategySelector(
    private val ocrExtractor: OcrPageExtractor,
    private val vlmExtractor: VlmPageExtractor,
    private val minWordsForGoodText: Int = 20
) {
    fun select(mode: ProcessingMode, page: ExtractedPage): IPageExtractor? {
        val wordCount = page.text.split("\\s+".toRegex()).count { it.isNotBlank() }
        val hasGoodText = !page.isScanned && wordCount >= minWordsForGoodText
        if (hasGoodText) return null
        return if (mode == ProcessingMode.THOROUGH && vlmExtractor.isAvailable()) vlmExtractor
               else ocrExtractor
    }
}
