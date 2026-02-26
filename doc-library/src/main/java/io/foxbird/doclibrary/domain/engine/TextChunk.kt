package io.foxbird.doclibrary.domain.engine

data class TextChunk(
    val content: String,
    val pageNumber: Int? = null,
    val sequenceIndex: Int,
    val chunkType: String = "paragraph",
    val wordCount: Int = content.split("\\s+".toRegex()).size,
    val sentenceCount: Int = content.split("[.!?]+".toRegex()).filter { it.isNotBlank() }.size
)
