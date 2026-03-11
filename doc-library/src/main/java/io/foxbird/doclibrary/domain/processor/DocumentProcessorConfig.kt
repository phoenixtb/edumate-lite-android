package io.foxbird.doclibrary.domain.processor

/**
 * Tunable configuration for [DocumentProcessor].
 *
 * Provided as a Koin singleton. Host apps can override any value:
 * ```kotlin
 * single { DocumentProcessorConfig(targetChunkSizeTokens = 256) }
 * ```
 * All defaults are calibrated for general educational content with a 512-token embedding model.
 */
data class DocumentProcessorConfig(
    /** Target size for each chunk in tokens. Chunks will be close to but not exceed [maxChunkSizeTokens]. */
    val targetChunkSizeTokens: Int = 512,
    /** Hard ceiling for chunk size. Chunks exceeding this are split further. */
    val maxChunkSizeTokens: Int = 640,
    /** Overlap between adjacent chunks in tokens, to preserve cross-chunk context. */
    val chunkOverlapTokens: Int = 64,
    /** Number of texts sent to the embedding model in a single batch call. */
    val embeddingBatchSize: Int = 20,
    /** Number of [ChunkEntity] rows written to Room in a single transaction. */
    val storageBatchSize: Int = 50
)
