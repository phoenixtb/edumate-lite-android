package io.foxbird.doclibrary.domain.processor

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Live snapshot of an in-progress document processing job. */
data class ProcessingState(
    val documentName: String,
    val stage: String,
    val current: Int,
    val total: Int
) {
    val progress: Float get() = if (total > 0) current.toFloat() / total else 0f
}

interface IDocumentProcessor {
    /** Null when no processing is active. Shared across all observers. */
    val processingState: StateFlow<ProcessingState?>

    fun processPdf(
        uri: Uri,
        title: String,
        subject: String? = null,
        gradeLevel: Int? = null,
        mode: ProcessingMode = ProcessingMode.FAST
    ): Flow<ProcessingEvent>

    fun processImages(
        uris: List<Uri>,
        title: String,
        subject: String? = null,
        gradeLevel: Int? = null,
        mode: ProcessingMode = ProcessingMode.FAST
    ): Flow<ProcessingEvent>

    fun processText(
        text: String,
        title: String,
        subject: String? = null
    ): Flow<ProcessingEvent>
}
