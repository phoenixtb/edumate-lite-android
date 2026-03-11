package io.foxbird.doclibrary.domain.engine

class ChunkingEngine(private val tokenCounter: TokenCounter) : IChunkingEngine {

    override fun chunkText(
        text: String,
        pageNumber: Int?,
        targetTokens: Int,
        maxTokens: Int,
        overlapTokens: Int
    ): List<TextChunk> {
        if (text.isBlank()) return emptyList()

        // Cache chars-per-token from the full text to avoid repeated tokenizer calls in getOverlapText
        val fullTokenCount = countTokens(text)
        val charsPerToken = if (fullTokenCount > 0) text.length.toFloat() / fullTokenCount else 4f

        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }.map { it.trim() }
        val rawChunks = mutableListOf<TextChunk>()
        var currentChunk = StringBuilder()
        var sequenceIndex = 0

        for (paragraph in paragraphs) {
            val combined = if (currentChunk.isEmpty()) paragraph
                           else "${currentChunk}\n\n$paragraph"
            val tokenCount = countTokens(combined)

            when {
                tokenCount > maxTokens && currentChunk.isNotEmpty() -> {
                    rawChunks.add(createChunk(currentChunk.toString(), pageNumber, sequenceIndex++))
                    val overlap = getOverlapText(currentChunk.toString(), overlapTokens, charsPerToken)
                    currentChunk = if (overlap.isNotBlank()) {
                        StringBuilder(overlap).append("\n\n").append(paragraph)
                    } else {
                        StringBuilder(paragraph)
                    }
                }
                tokenCount > maxTokens -> {
                    splitLongParagraph(paragraph, maxTokens, overlapTokens, charsPerToken).forEach { sub ->
                        rawChunks.add(createChunk(sub, pageNumber, sequenceIndex++))
                    }
                    currentChunk = StringBuilder()
                }
                tokenCount >= targetTokens -> {
                    rawChunks.add(createChunk(combined, pageNumber, sequenceIndex++))
                    val overlap = getOverlapText(combined, overlapTokens, charsPerToken)
                    currentChunk = if (overlap.isNotBlank()) StringBuilder(overlap) else StringBuilder()
                }
                else -> currentChunk = StringBuilder(combined)
            }
        }

        if (currentChunk.isNotBlank()) {
            rawChunks.add(createChunk(currentChunk.toString(), pageNumber, sequenceIndex))
        }

        return injectHeadingContext(rawChunks)
    }

    /**
     * Splits [text] into sub-chunks ≤ [maxTokens]. Uses a tiered strategy:
     * 1. Sentence boundaries (.!?) — handles most academic prose
     * 2. Semicolons — handles enumerated clauses and equations with no period
     * 3. Newlines — handles bulleted items, numbered equations, and code-like text
     * If none reduce the size sufficiently, the oversized segment is emitted as-is rather than dropped.
     */
    private fun splitLongParagraph(
        text: String,
        maxTokens: Int,
        overlapTokens: Int,
        charsPerToken: Float
    ): List<String> {
        val segments = splitIntoSegments(text)
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        for (segment in segments) {
            val combined = if (current.isEmpty()) segment else "$current $segment"
            if (countTokens(combined) > maxTokens && current.isNotEmpty()) {
                chunks.add(current.toString())
                val overlap = getOverlapText(current.toString(), overlapTokens, charsPerToken)
                current = if (overlap.isNotBlank()) {
                    StringBuilder(overlap).append(" ").append(segment)
                } else {
                    StringBuilder(segment)
                }
            } else {
                current = StringBuilder(combined)
            }
        }
        if (current.isNotBlank()) chunks.add(current.toString())
        return chunks
    }

    /**
     * Splits [text] using progressively finer delimiters until more than one segment results.
     * Falls back to returning the whole text as a single segment if no boundary is found.
     */
    private fun splitIntoSegments(text: String): List<String> {
        val bySentence = text.split("(?<=[.!?])\\s+".toRegex()).filter { it.isNotBlank() }
        if (bySentence.size > 1) return bySentence

        val bySemicolon = text.split("(?<=;)\\s*".toRegex()).filter { it.isNotBlank() }
        if (bySemicolon.size > 1) return bySemicolon

        val byNewline = text.split("\n").filter { it.isNotBlank() }
        if (byNewline.size > 1) return byNewline

        return listOf(text)
    }

    /**
     * Returns the last [overlapTokens] worth of text from [text], trimmed to a sentence boundary.
     * Uses the pre-computed [charsPerToken] ratio to avoid re-tokenizing the same chunk.
     */
    private fun getOverlapText(text: String, overlapTokens: Int, charsPerToken: Float): String {
        if (overlapTokens <= 0) return ""
        if (text.isBlank()) return ""

        val targetChars = (overlapTokens * charsPerToken).toInt().coerceAtLeast(1)
        val overlap = if (text.length <= targetChars) text else text.takeLast(targetChars)

        val sentenceStart = overlap.indexOfFirst { it == '.' || it == '!' || it == '?' }
        return if (sentenceStart >= 0 && sentenceStart < overlap.length - 1) {
            overlap.substring(sentenceStart + 1).trim()
        } else {
            overlap.trimStart()
        }
    }

    /**
     * When a heading chunk is immediately followed by a non-heading chunk from the same
     * (or unspecified) page, prepend the heading to the next chunk's content. This preserves
     * document hierarchy in the retrieval unit without creating standalone heading-only chunks.
     */
    private fun injectHeadingContext(chunks: List<TextChunk>): List<TextChunk> {
        if (chunks.size <= 1) return chunks
        val result = mutableListOf<TextChunk>()
        var i = 0
        while (i < chunks.size) {
            val chunk = chunks[i]
            if (chunk.chunkType == "heading" && i + 1 < chunks.size) {
                val next = chunks[i + 1]
                val samePage = chunk.pageNumber == null || next.pageNumber == null ||
                               chunk.pageNumber == next.pageNumber
                if (samePage && next.chunkType != "heading") {
                    val merged = "${chunk.content}\n\n${next.content}"
                    result.add(
                        createChunk(merged, next.pageNumber ?: chunk.pageNumber, next.sequenceIndex)
                            .copy(sequenceIndex = next.sequenceIndex)
                    )
                    i += 2
                    continue
                }
            }
            result.add(chunk)
            i++
        }
        return result
    }

    private fun createChunk(text: String, pageNumber: Int?, sequenceIndex: Int): TextChunk {
        val trimmed = text.trim()
        return TextChunk(
            content = trimmed,
            pageNumber = pageNumber,
            sequenceIndex = sequenceIndex,
            chunkType = detectChunkType(trimmed),
            wordCount = trimmed.split("\\s+".toRegex()).count { it.isNotBlank() },
            sentenceCount = trimmed.split("[.!?]+".toRegex()).count { it.isNotBlank() }
        )
    }

    private fun detectChunkType(text: String): String {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "paragraph"

        // Heading: ALL-CAPS line, or section-numbered line (1. / 1.2 / Chapter 3), or short underlined
        if (lines.size <= 3 && text.length < 150) {
            val firstLine = lines.first()
            if (firstLine == firstLine.uppercase() && firstLine.length > 2) return "heading"
            if (SECTION_NUMBER_RE.matches(firstLine)) return "heading"
            if (CHAPTER_RE.containsMatchIn(firstLine)) return "heading"
        }

        // Definition: starts with a standard academic keyword
        if (DEFINITION_RE.containsMatchIn(text)) return "definition"

        // Equation: LaTeX macros, display math, or isolated short formula lines
        if (EQUATION_RE.any { it.containsMatchIn(text) }) return "equation"

        // Code: fenced code, consistent indentation, or keyword-starting lines
        if (CODE_FENCE_RE.containsMatchIn(text)) return "code"
        val indentedLines = lines.count { it.startsWith("    ") || it.startsWith("\t") }
        if (lines.size >= 3 && indentedLines.toFloat() / lines.size > 0.5f) return "code"
        if (CODE_KEYWORD_RE.containsMatchIn(text) && lines.size <= 10) return "code"

        // List: majority of lines start with a bullet or numbering
        val listLines = lines.count { LIST_LINE_RE.containsMatchIn(it) }
        if (lines.size >= 2 && listLines.toFloat() / lines.size > 0.6f) return "list"

        return "paragraph"
    }

    override fun countTokens(text: String): Int = tokenCounter.count(text)

    companion object {
        private val SECTION_NUMBER_RE = "^\\d+(\\.\\d+)*\\.?\\s+\\w+".toRegex()
        private val CHAPTER_RE = "(?i)^(chapter|section|part|unit|module|appendix)\\s+\\d+".toRegex()
        private val DEFINITION_RE =
            "(?im)^(Definition|Theorem|Lemma|Corollary|Remark|Proof|Proposition|Axiom|Note|Example):?\\s".toRegex()
        private val EQUATION_RE = listOf(
            "\\\\(?:frac|sum|int|sqrt|alpha|beta|gamma|theta|pi|sigma|delta|lambda|nabla|partial)".toRegex(),
            "\\$[^$\n]{2,}\\$".toRegex(),
            "\\\\\\[".toRegex()
        )
        private val CODE_FENCE_RE = "```".toRegex()
        private val CODE_KEYWORD_RE =
            "(?m)^(?:def |class |import |public |private |protected |function |const |var |let |#include|fn |impl |struct |enum )".toRegex()
        private val LIST_LINE_RE =
            "^\\s*(?:[-•*·▸▹◦]|\\d+[.)\\s]|[a-zA-Z][.)\\s])\\s+\\S".toRegex()
    }
}
