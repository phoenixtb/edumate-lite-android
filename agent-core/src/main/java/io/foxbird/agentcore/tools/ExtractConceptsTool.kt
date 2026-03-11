package io.foxbird.agentcore.tools

import ai.koog.agents.core.tools.SimpleTool
import io.foxbird.doclibrary.data.repository.ChunkRepository
import io.foxbird.doclibrary.domain.service.ConceptExtractor
import kotlinx.serialization.Serializable

@Serializable
data class ExtractConceptsArgs(
    val documentId: Long
)

/**
 * Agent-callable shim to trigger concept extraction for an already-ingested document.
 *
 * Thin delegation to [ConceptExtractor.extractAndStoreByChunks] — no business logic here.
 * Non-agent callers (TaskQueue background tasks) should call [ConceptExtractor] directly.
 */
class ExtractConceptsTool(
    private val conceptExtractor: ConceptExtractor,
    private val chunkRepository: ChunkRepository
) : SimpleTool<ExtractConceptsArgs>(
    argsSerializer = ExtractConceptsArgs.serializer(),
    name = "extract_concepts",
    description = "Extract and index key concepts from an already-ingested document. " +
        "Requires the document to have been ingested first via ingest_material. " +
        "Returns a summary of how many concepts were extracted."
) {
    override suspend fun execute(args: ExtractConceptsArgs): String {
        val chunks = chunkRepository.getByDocumentId(args.documentId)
        if (chunks.isEmpty()) {
            return "No chunks found for documentId=${args.documentId}. Ingest the document first."
        }
        val concepts = conceptExtractor.extractAndStoreByChunks(
            documentId = args.documentId,
            chunks = chunks.map { it.id to it.content }
        )
        return "Extracted ${concepts.size} concepts from ${chunks.size} chunks " +
            "(documentId=${args.documentId})."
    }
}
