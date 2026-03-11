package io.foxbird.doclibrary.domain.extractor

import android.graphics.Bitmap

/**
 * Extracts text from a pre-rendered page [Bitmap].
 *
 * The adapter layer (PdfInputAdapter / ImageInputAdapter) owns rendering bitmaps;
 * this interface owns the text-understanding step.
 */
interface IPageExtractor {
    val name: String
    suspend fun extract(bitmap: Bitmap): String
    fun isAvailable(): Boolean
}
