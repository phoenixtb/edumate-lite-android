package io.foxbird.doclibrary.domain.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ExtractedPage(
    val pageNumber: Int,
    val text: String,
    val imagePath: String? = null,
    val extractionMethod: String = "text",
    val isScanned: Boolean = false
)

class PdfInputAdapter(private val context: Context) {

    companion object {
        private const val TAG = "PdfInputAdapter"
        private var pdfBoxInitialized = false

        private val PAGE_NOISE_PATTERNS = listOf(
            "^\\s*\\d+\\s*$".toRegex(),
            "^\\s*-\\s*\\d+\\s*-\\s*$".toRegex(),
            "^\\s*Page\\s+\\d+\\s+(of\\s+\\d+)?\\s*$".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*©.{0,60}$".toRegex(),
            "^\\s*All rights reserved\\.?\\s*$".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*CHAPTER\\s+\\d+.*$".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*[A-Z][A-Z\\s]{2,40}$".toRegex(),
        )
    }

    private fun ensurePdfBoxInit() {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context)
            pdfBoxInitialized = true
        }
    }

    /** Hash computed during the most recent [extractText] call; null if not yet run or on error. */
    @Volatile
    var lastComputedHash: String? = null
        private set

    /**
     * Extracts text from all pages of the PDF. As a side-channel, computes the SHA-256 hash of the
     * file using [DigestInputStream] so PDFBox and the hasher share a single I/O pass. The hash is
     * stored in [lastComputedHash] after the flow completes.
     */
    fun extractText(uri: Uri): Flow<ExtractedPage> = flow {
        ensurePdfBoxInit()
        lastComputedHash = null
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open PDF")

        val digest = MessageDigest.getInstance("SHA-256")
        val digestStream = DigestInputStream(inputStream, digest)
        val document = PDDocument.load(digestStream)
        val stripper = PDFTextStripper()
        val totalPages = document.numberOfPages
        Logger.i(TAG, "Extracting text from $totalPages pages")

        try {
            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                var text = stripper.getText(document)
                text = fixSpacedCharacters(text)
                text = filterPageNoise(text)
                val isScanned = text.isBlank()
                emit(
                    ExtractedPage(
                        pageNumber = page,
                        text = text.trim(),
                        extractionMethod = if (isScanned) "scanned" else "text",
                        isScanned = isScanned
                    )
                )
            }
        } finally {
            document.close()
            digestStream.close()
            lastComputedHash = digest.digest().joinToString("") { "%02x".format(it) }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Extracts text from a single scanned/image-based PDF page using ML Kit Text Recognition.
     * Fully on-device; no network or inference model required.
     */
    suspend fun extractPageViaOcr(uri: Uri, pageIndex: Int): ExtractedPage {
        val bitmap = renderPageToImage(uri, pageIndex)
            ?: return ExtractedPage(
                pageNumber = pageIndex + 1,
                text = "",
                extractionMethod = "ocr_failed",
                isScanned = true
            )

        return try {
            val recognizedText = runMlKitOcr(bitmap)
            bitmap.recycle()
            ExtractedPage(
                pageNumber = pageIndex + 1,
                text = recognizedText.trim(),
                extractionMethod = if (recognizedText.isBlank()) "ocr_empty" else "ocr",
                isScanned = recognizedText.isBlank()
            )
        } catch (e: Exception) {
            bitmap.recycle()
            Logger.e(TAG, "OCR failed for page ${pageIndex + 1}: ${e.message}")
            ExtractedPage(
                pageNumber = pageIndex + 1,
                text = "",
                extractionMethod = "ocr_failed",
                isScanned = true
            )
        }
    }

    fun getPageCount(uri: Uri): Int {
        ensurePdfBoxInit()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
        return try {
            val document = PDDocument.load(inputStream)
            try {
                document.numberOfPages
            } finally {
                document.close()
            }
        } catch (e: Exception) {
            Logger.w(TAG, "getPageCount failed: ${e.message}")
            0
        } finally {
            inputStream.close()
        }
    }

    fun renderPageToImage(uri: Uri, pageIndex: Int): Bitmap? {
        return try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(fd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close(); fd.close(); return null
            }
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); fd.close()
            bitmap
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to render page $pageIndex", e)
            null
        }
    }

    /**
     * Conservative fix for PDFs that encode letters as individual spaced characters
     * (e.g., "H e l l o" → "Hello"). Only applies to lines where ALL non-space tokens are single
     * alphabetic characters AND total word count is ≥ 4, avoiding chemistry/math abbreviations.
     */
    private fun fixSpacedCharacters(text: String): String =
        text.lines().joinToString("\n") { line ->
            if (line.length >= 10 && isFullySpacedText(line)) line.replace(" ", "") else line
        }

    private fun isFullySpacedText(line: String): Boolean {
        val tokens = line.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.size < 4) return false
        return tokens.all { it.length == 1 && it[0].isLetter() }
    }

    /**
     * Removes standalone page numbers, running headers/footers from the first 3 and last 3 lines.
     */
    private fun filterPageNoise(text: String): String {
        val lines = text.lines().toMutableList()
        if (lines.size <= 4) return text

        fun isNoiseLine(line: String) = line.isBlank() || PAGE_NOISE_PATTERNS.any { it.matches(line.trim()) }

        // Strip up to 3 noise lines from the top
        var removed = 0
        while (removed < 3 && lines.isNotEmpty() && isNoiseLine(lines.first())) {
            lines.removeAt(0)
            removed++
        }

        // Strip up to 3 noise lines from the bottom
        removed = 0
        while (removed < 3 && lines.isNotEmpty() && isNoiseLine(lines.last())) {
            lines.removeAt(lines.lastIndex)
            removed++
        }

        return lines.joinToString("\n")
    }
}

class TextInputAdapter {
    fun extractText(text: String): ExtractedPage =
        ExtractedPage(pageNumber = 1, text = text.trim())
}

class ImageInputAdapter(private val context: Context) {

    companion object {
        private const val TAG = "ImageInputAdapter"
    }

    fun loadImage(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load image", e)
            null
        }
    }

    /**
     * Extracts text from an image file using ML Kit Text Recognition (fully on-device).
     */
    suspend fun extractTextViaOcr(uri: Uri, pageNumber: Int): ExtractedPage {
        val bitmap = loadImage(uri)
            ?: return ExtractedPage(
                pageNumber = pageNumber,
                text = "",
                extractionMethod = "ocr_failed",
                isScanned = true
            )

        return try {
            val recognizedText = runMlKitOcr(bitmap)
            bitmap.recycle()
            ExtractedPage(
                pageNumber = pageNumber,
                text = recognizedText.trim(),
                extractionMethod = if (recognizedText.isBlank()) "ocr_empty" else "ocr"
            )
        } catch (e: Exception) {
            bitmap.recycle()
            Logger.e(TAG, "OCR failed for image $pageNumber: ${e.message}")
            ExtractedPage(
                pageNumber = pageNumber,
                text = "",
                extractionMethod = "ocr_failed",
                isScanned = true
            )
        }
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return out.toByteArray()
    }
}

/**
 * Shared ML Kit OCR helper. Converts [bitmap] to an [InputImage] and runs Latin text recognition.
 * Suspends until the recognition task completes; throws on failure.
 * Internal visibility allows [io.foxbird.doclibrary.domain.extractor.OcrPageExtractor] to delegate here.
 */
internal suspend fun runMlKitOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            recognizer.close()
            cont.resume(visionText.text)
        }
        .addOnFailureListener { e ->
            recognizer.close()
            cont.resumeWithException(e)
        }
    cont.invokeOnCancellation { recognizer.close() }
}
