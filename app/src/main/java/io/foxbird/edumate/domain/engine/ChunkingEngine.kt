package io.foxbird.edumate.domain.engine

import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edumate.core.util.AppConstants

data class TextChunk(
    val content: String,
    val pageNumber: Int? = null,
    val sequenceIndex: Int,
    val chunkType: String = "paragraph",
    val wordCount: Int = content.split("\\s+".toRegex()).size,
    val sentenceCount: Int = content.split("[.!?]+".toRegex()).filter { it.isNotBlank() }.size
)

class ChunkingEngine(private val orchestrator: EngineOrchestrator) {

    fun chunkText(
        text: String,
        pageNumber: Int? = null,
        targetTokens: Int = AppConstants.TARGET_CHUNK_SIZE_TOKENS,
        maxTokens: Int = AppConstants.MAX_CHUNK_SIZE_TOKENS,
        overlapChars: Int = AppConstants.CHUNK_OVERLAP_CHARS
    ): List<TextChunk> {
        if (text.isBlank()) return emptyList()

        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }.map { it.trim() }
        val chunks = mutableListOf<TextChunk>()
        var currentChunk = StringBuilder()
        var sequenceIndex = 0

        for (paragraph in paragraphs) {
            val combined = if (currentChunk.isEmpty()) paragraph
            else "${currentChunk}\n\n$paragraph"

            val tokenCount = countTokens(combined)

            if (tokenCount > maxTokens && currentChunk.isNotEmpty()) {
                chunks.add(createChunk(currentChunk.toString(), pageNumber, sequenceIndex++))
                val overlap = getOverlapText(currentChunk.toString(), overlapChars)
                currentChunk = StringBuilder(overlap)
                currentChunk.append("\n\n").append(paragraph)
            } else if (tokenCount > maxTokens) {
                val subChunks = splitLongParagraph(paragraph, maxTokens, overlapChars)
                for (sub in subChunks) {
                    chunks.add(createChunk(sub, pageNumber, sequenceIndex++))
                }
                currentChunk = StringBuilder()
            } else if (tokenCount >= targetTokens) {
                chunks.add(createChunk(combined, pageNumber, sequenceIndex++))
                val overlap = getOverlapText(combined, overlapChars)
                currentChunk = StringBuilder(overlap)
            } else {
                currentChunk = StringBuilder(combined)
            }
        }

        if (currentChunk.isNotBlank()) {
            chunks.add(createChunk(currentChunk.toString(), pageNumber, sequenceIndex))
        }

        return chunks
    }

    private fun splitLongParagraph(text: String, maxTokens: Int, overlapChars: Int): List<String> {
        val sentences = text.split("(?<=[.!?])\\s+".toRegex()).filter { it.isNotBlank() }
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        for (sentence in sentences) {
            val combined = if (current.isEmpty()) sentence else "$current $sentence"
            if (countTokens(combined) > maxTokens && current.isNotEmpty()) {
                chunks.add(current.toString())
                val overlap = getOverlapText(current.toString(), overlapChars)
                current = StringBuilder(overlap).append(" ").append(sentence)
            } else {
                current = StringBuilder(combined)
            }
        }
        if (current.isNotBlank()) chunks.add(current.toString())
        return chunks
    }

    private fun getOverlapText(text: String, overlapChars: Int): String {
        if (text.length <= overlapChars) return text
        val overlap = text.takeLast(overlapChars)
        val sentenceStart = overlap.indexOfFirst { it == '.' || it == '!' || it == '?' }
        return if (sentenceStart > 0 && sentenceStart < overlap.length - 1) {
            overlap.substring(sentenceStart + 1).trim()
        } else {
            overlap.trim()
        }
    }

    private fun createChunk(text: String, pageNumber: Int?, sequenceIndex: Int): TextChunk {
        val trimmed = text.trim()
        return TextChunk(
            content = trimmed,
            pageNumber = pageNumber,
            sequenceIndex = sequenceIndex,
            chunkType = detectChunkType(trimmed),
            wordCount = trimmed.split("\\s+".toRegex()).size,
            sentenceCount = trimmed.split("[.!?]+".toRegex()).filter { it.isNotBlank() }.size
        )
    }

    private fun detectChunkType(text: String): String {
        return when {
            text.lines().all { it.trimStart().startsWith("-") || it.trimStart().startsWith("•") || it.trimStart().matches("^\\d+[.)].*".toRegex()) } -> "list"
            text.lines().size <= 2 && text.length < 100 -> "heading"
            text.contains("\\$.*\\$".toRegex()) || text.contains("=") && text.contains("(") -> "equation"
            text.startsWith("def ", true) || text.contains(": ", false) && text.lines().size <= 3 -> "definition"
            else -> "paragraph"
        }
    }

    fun countTokens(text: String): Int {
        return try {
            val count = orchestrator.tokenCount(text)
            if (count > 0) count else estimateTokens(text)
        } catch (_: Exception) {
            estimateTokens(text)
        }
    }

    private fun estimateTokens(text: String): Int = (text.length * 0.3).toInt().coerceAtLeast(1)
}
