package io.foxbird.doclibrary.domain.processor

enum class ProcessingMode {
    /** Quick text extraction. Best for text-based PDFs. */
    FAST,

    /** AI reads each page. Better for scanned docs, equations, diagrams. Slower. */
    THOROUGH
}
