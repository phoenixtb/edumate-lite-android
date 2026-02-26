package io.foxbird.edumate.domain.service

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import io.foxbird.doclibrary.domain.rag.IRagEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class WorksheetConfig(
    val title: String,
    val materialIds: List<Long>,
    val numQuestions: Int = 10,
    val difficulty: String = "medium",
    val questionTypes: List<String> = listOf("multiple_choice", "short_answer", "true_false")
)

class WorksheetService(
    private val context: Context,
    private val ragEngine: IRagEngine,
    private val orchestrator: EngineOrchestrator
) {
    companion object {
        private const val TAG = "WorksheetService"
    }

    suspend fun generateWorksheet(config: WorksheetConfig): String? {
        val query = "Generate practice questions about the key topics"
        val ragContext = ragEngine.retrieve(query, config.materialIds, topK = 5, threshold = 0.0f)

        if (ragContext.contextText.isBlank()) {
            Logger.e(TAG, "No context available for worksheet")
            return null
        }

        val prompt = buildString {
            append("System: Generate a worksheet with ${config.numQuestions} questions ")
            append("at ${config.difficulty} difficulty level. Include these question types: ")
            append("${config.questionTypes.joinToString(", ")}. ")
            append("Format each question clearly with numbering. ")
            append("Include an answer key at the end.\n\n")
            append("Study Material:\n${ragContext.contextText}\n\n")
            append("Title: ${config.title}\n\nWorksheet:")
        }

        val result = orchestrator.generateComplete(
            prompt = prompt,
            params = GenerationParams(maxTokens = 2048, temperature = 0.3f)
        )

        return result.fold(
            ifLeft = { error ->
                Logger.e(TAG, "Worksheet generation failed: ${error.message}")
                null
            },
            ifRight = { it }
        )
    }

    suspend fun exportToPdf(content: String, title: String): File? = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val paint = Paint().apply {
                textSize = 12f
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val pageWidth = 595
            val pageHeight = 842
            val margin = 50f
            val lineHeight = 18f
            val maxWidth = pageWidth - 2 * margin

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var y = margin + 30f

            canvas.drawText(title, margin, y, titlePaint)
            y += lineHeight * 2

            val lines = content.split("\n")
            for (line in lines) {
                if (y + lineHeight > pageHeight - margin) {
                    document.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin
                }

                val wrappedLines = wrapText(line, paint, maxWidth)
                for (wrapped in wrappedLines) {
                    canvas.drawText(wrapped, margin, y, paint)
                    y += lineHeight
                }
            }

            document.finishPage(page)

            val file = File(context.cacheDir, "${title.take(30).replace(" ", "_")}_worksheet.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            Logger.i(TAG, "PDF exported: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Logger.e(TAG, "PDF export failed", e)
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) {
                current = StringBuilder(test)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
