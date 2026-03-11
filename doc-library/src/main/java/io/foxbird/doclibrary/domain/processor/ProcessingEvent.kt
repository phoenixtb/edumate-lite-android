package io.foxbird.doclibrary.domain.processor

sealed class ProcessingEvent {
    data class Progress(val stage: String, val current: Int, val total: Int) : ProcessingEvent()
    data class Complete(val documentId: Long, val chunkCount: Int) : ProcessingEvent()
    data class Error(val message: String) : ProcessingEvent()
    /** Emitted when a document with the same file hash already exists in the library. */
    data class Duplicate(val existingDocumentId: Long, val existingTitle: String) : ProcessingEvent()
}
