package io.foxbird.agentcore.tools

import ai.koog.agents.core.tools.SimpleTool
import android.net.Uri
import io.foxbird.doclibrary.domain.processor.IDocumentProcessor
import io.foxbird.doclibrary.domain.processor.ProcessingEvent
import io.foxbird.doclibrary.domain.processor.ProcessingMode
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable

@Serializable
data class IngestMaterialArgs(
    /** String-encoded URI (content:// or file://) for "pdf" source type; raw text for "text" source type. */
    val sourceUri: String,
    val title: String,
    val sourceType: String = "pdf",   // "pdf" or "text"
    val mode: String = "fast"          // "fast" or "thorough"
)

/**
 * Agent-callable shim to trigger document ingestion via [IDocumentProcessor].
 *
 * This tool contains zero business logic — it converts Koog's serialized args into the
 * correct processor call, collects the resulting [ProcessingEvent] flow, and returns a
 * plain-text summary string back to the agent loop.
 *
 * Non-agent callers (LibraryViewModel, TaskQueue) should call [IDocumentProcessor] directly.
 */
class IngestMaterialTool(
    private val documentProcessor: IDocumentProcessor
) : SimpleTool<IngestMaterialArgs>(
    argsSerializer = IngestMaterialArgs.serializer(),
    name = "ingest_material",
    description = "Process and index a document into the RAG store. " +
        "For PDFs: set sourceType='pdf' and sourceUri to the content URI. " +
        "For plain text: set sourceType='text' and sourceUri to the text content. " +
        "mode='fast' (text+OCR, default) or 'thorough' (OCR or VLM for higher quality)."
) {
    override suspend fun execute(args: IngestMaterialArgs): String {
        val mode = if (args.mode == "thorough") ProcessingMode.THOROUGH else ProcessingMode.FAST

        val events = when (args.sourceType) {
            "text" -> documentProcessor.processText(
                text = args.sourceUri,
                title = args.title
            ).toList()
            "pdf" -> documentProcessor.processPdf(
                uri = Uri.parse(args.sourceUri),
                title = args.title,
                mode = mode
            ).toList()
            else -> return "Unsupported sourceType '${args.sourceType}'. Use 'pdf' or 'text'."
        }

        return when (val last = events.lastOrNull()) {
            is ProcessingEvent.Complete ->
                "Ingested '${args.title}': ${last.chunkCount} chunks indexed (documentId=${last.documentId})."
            is ProcessingEvent.Error ->
                "Ingestion failed: ${last.message}"
            is ProcessingEvent.Duplicate ->
                "Document already exists in library (existingId=${last.existingDocumentId}, title='${last.existingTitle}')."
            else -> "Ingestion completed."
        }
    }
}
