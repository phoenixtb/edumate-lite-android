package io.foxbird.doclibrary.domain.engine

interface IChunkingEngine {
    fun chunkText(
        text: String,
        pageNumber: Int? = null,
        targetTokens: Int,
        maxTokens: Int,
        overlapChars: Int
    ): List<TextChunk>

    fun countTokens(text: String): Int
}
