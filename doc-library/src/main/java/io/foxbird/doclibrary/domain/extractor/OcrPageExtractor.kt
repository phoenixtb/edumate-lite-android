package io.foxbird.doclibrary.domain.extractor

import android.graphics.Bitmap
import io.foxbird.doclibrary.domain.adapter.runMlKitOcr

/** Extracts text from a page bitmap using ML Kit Text Recognition (fully on-device, no model required). */
class OcrPageExtractor : IPageExtractor {
    override val name = "ocr"
    override fun isAvailable() = true
    override suspend fun extract(bitmap: Bitmap): String = runMlKitOcr(bitmap)
}
