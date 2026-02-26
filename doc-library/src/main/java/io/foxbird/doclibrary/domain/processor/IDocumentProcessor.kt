package io.foxbird.doclibrary.domain.processor

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface IDocumentProcessor {
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
