package io.foxbird.doclibrary.domain.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class ExtractedPage(
    val pageNumber: Int,
    val text: String,
    val imagePath: String? = null,
    val extractionMethod: String = "text"
)

class PdfInputAdapter(private val context: Context) {

    companion object {
        private const val TAG = "PdfInputAdapter"
        private var pdfBoxInitialized = false
    }

    private fun ensurePdfBoxInit() {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context)
            pdfBoxInitialized = true
        }
    }

    fun extractText(uri: Uri): Flow<ExtractedPage> = flow {
        ensurePdfBoxInit()
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open PDF")

        val document = PDDocument.load(inputStream)
        val stripper = PDFTextStripper()
        val totalPages = document.numberOfPages

        Logger.i(TAG, "Extracting text from $totalPages pages")

        for (page in 1..totalPages) {
            stripper.startPage = page
            stripper.endPage = page
            var text = stripper.getText(document)
            text = fixSpacedCharacters(text)
            emit(ExtractedPage(pageNumber = page, text = text.trim()))
        }

        document.close()
        inputStream.close()
    }.flowOn(Dispatchers.IO)

    fun getPageCount(uri: Uri): Int {
        ensurePdfBoxInit()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
        val document = PDDocument.load(inputStream)
        val count = document.numberOfPages
        document.close()
        inputStream.close()
        return count
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

    private fun fixSpacedCharacters(text: String): String =
        text.lines().joinToString("\n") { line ->
            if (isSpacedText(line)) line.replace(" ", "").replace("  ", " ") else line
        }

    private fun isSpacedText(line: String): Boolean {
        if (line.length < 5) return false
        val words = line.trim().split("\\s+".toRegex())
        val singleCharWords = words.count { it.length == 1 }
        return singleCharWords.toFloat() / words.size > 0.5f
    }
}

class TextInputAdapter {
    fun extractText(text: String): ExtractedPage =
        ExtractedPage(pageNumber = 1, text = text.trim())
}

class ImageInputAdapter(private val context: Context) {
    fun loadImage(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Logger.e("ImageInputAdapter", "Failed to load image", e)
            null
        }
    }
}
