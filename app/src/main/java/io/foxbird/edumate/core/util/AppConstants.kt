package io.foxbird.edumate.core.util

object AppConstants {
    // Embedding
    const val EMBEDDING_DIMENSION = 768
    const val MAX_EMBEDDING_TOKENS = 2048

    // Inference
    const val INFERENCE_TEMPERATURE = 0.4f
    const val INFERENCE_TOP_K = 20
    const val MAX_INFERENCE_TOKENS = 2048
    const val DEFAULT_CONTEXT_SIZE = 4096

    // Chunking
    const val TARGET_CHUNK_SIZE_TOKENS = 1800
    const val MAX_CHUNK_SIZE_TOKENS = 1950
    const val CHUNK_OVERLAP_TOKENS = 150
    const val CHUNK_OVERLAP_CHARS = 200

    // RAG
    const val RETRIEVAL_TOP_K = 3
    const val SIMILARITY_THRESHOLD = 0.5f
    const val MAX_CONTEXT_TOKENS = 2000
    const val BM25_VECTOR_WEIGHT = 0.7f
    const val BM25_KEYWORD_WEIGHT = 0.3f

    // Conversation
    const val MAX_CONTEXT_MESSAGES = 6

    // File limits
    const val MAX_PDF_SIZE_MB = 500
    const val MAX_IMAGE_SIZE_MB = 10
    const val MAX_PDF_PAGES = 3000

    // Processing batches
    const val PDF_PAGE_BATCH_SIZE = 10
    const val EMBEDDING_BATCH_SIZE = 20
    const val STORAGE_BATCH_SIZE = 50

    // Threading
    const val DEFAULT_N_THREADS = 4
}
