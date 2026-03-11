package io.foxbird.doclibrary.domain.service

import io.foxbird.doclibrary.data.local.dao.ConceptDao
import io.foxbird.doclibrary.data.local.entity.ConceptEntity
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.engine.ITextGenerator
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
data class ExtractedConcept(
    val name: String,
    val type: String = "concept",
    val definition: String? = null
)

class ConceptExtractor(
    private val textGenerator: ITextGenerator,
    private val conceptDao: ConceptDao
) {
    companion object {
        private const val TAG = "ConceptExtractor"

        private const val SYSTEM_INSTRUCTION =
            "Extract key concepts from the text below. " +
            "Return ONLY a valid JSON array of objects with fields: " +
            "\"name\" (string), \"type\" (one of: term, person, formula, theorem, concept, event, place, definition), " +
            "\"definition\" (brief string or null). No explanation, no markdown, only the JSON array."

        private const val NO_THINK_SUFFIX = " /no_think"

        /**
         * Number of chunks batched per LLM call. 4 chunks ≈ 8 000 chars — enough context for
         * cross-chunk concept detection without exploding the prompt. Adjust if context budget is tight.
         */
        private const val CHUNK_BATCH_SIZE = 4
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Processes every chunk in [chunks] (id → content pairs) in sliding batches, running one LLM
     * call per batch. This gives 100% document coverage and exact chunk→concept links with no
     * sampling gaps. For a 200-chunk document, that is ceil(200/4) = 50 LLM calls — proportional
     * to document size, same as the old windowing approach but without coverage holes.
     */
    suspend fun extractAndStoreByChunks(
        documentId: Long,
        chunks: List<Pair<Long, String>>
    ): List<ConceptEntity> {
        if (chunks.isEmpty()) return emptyList()

        val modelConfig = textGenerator.findActiveModelConfig()
        val systemLine = buildSystemLine(modelConfig)

        val allConcepts = mutableListOf<ConceptEntity>()
        val batches = chunks.chunked(CHUNK_BATCH_SIZE)
        Logger.i(TAG, "Concept extraction: ${chunks.size} chunks in ${batches.size} batches (doc $documentId)")

        for (batch in batches) {
            val batchIds = batch.map { it.first }
            val batchText = batch.joinToString("\n\n---\n\n") { it.second }
            val concepts = runExtractionWindow(batchText, documentId, batchIds, systemLine, modelConfig)
            allConcepts.addAll(concepts)
        }

        return allConcepts
    }

    private fun buildSystemLine(modelConfig: ModelConfig?): String =
        if (modelConfig?.thinkOpenTag != null) "$SYSTEM_INSTRUCTION$NO_THINK_SUFFIX"
        else SYSTEM_INSTRUCTION

    private suspend fun runExtractionWindow(
        text: String,
        documentId: Long,
        chunkIds: List<Long>,
        systemLine: String,
        modelConfig: ModelConfig?
    ): List<ConceptEntity> {
        val prompt = "System: $systemLine\n\nText:\n$text\n\nJSON:"
        val result = textGenerator.generateComplete(
            prompt = prompt,
            params = GenerationParams(maxTokens = 1024, temperature = 0.1f)
        )
        return result.fold(
            ifLeft = { error ->
                Logger.w(TAG, "Concept extraction failed for batch: ${error.message}")
                emptyList()
            },
            ifRight = { response ->
                val clean = stripThinkBlocks(response, modelConfig?.thinkOpenTag, modelConfig?.thinkCloseTag)
                parseAndMergeConcepts(clean, documentId, chunkIds)
            }
        )
    }

    /**
     * Removes all content between think open/close tags from the model response.
     * Prevents thinking-mode models (e.g. Qwen3.5) from polluting JSON output with CoT text.
     */
    private fun stripThinkBlocks(text: String, openTag: String?, closeTag: String?): String {
        if (openTag == null || closeTag == null) return text
        var result = text
        var startIdx = result.indexOf(openTag)
        while (startIdx >= 0) {
            val endIdx = result.indexOf(closeTag, startIdx)
            result = if (endIdx >= 0) {
                result.removeRange(startIdx, endIdx + closeTag.length)
            } else {
                result.substring(0, startIdx)
            }
            startIdx = result.indexOf(openTag)
        }
        return result.trim()
    }

    private suspend fun parseAndMergeConcepts(
        response: String,
        documentId: Long,
        chunkIds: List<Long>
    ): List<ConceptEntity> {
        val extracted = try {
            val jsonStr = extractJsonArray(response)
            if (jsonStr == "[]") return emptyList()
            json.decodeFromString<List<ExtractedConcept>>(jsonStr)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse concepts JSON: ${e.message}")
            return emptyList()
        }

        val stored = mutableListOf<ConceptEntity>()
        val now = System.currentTimeMillis()
        val docIdJson = "[$documentId]"
        val chunkIdJson = json.encodeToString(ListSerializer(Long.serializer()), chunkIds)

        for (concept in extracted) {
            if (concept.name.isBlank()) continue
            val normalized = concept.name.lowercase().trim()
            val existing = conceptDao.getByNormalizedName(normalized)

            if (existing != null) {
                val updatedDocIds = mergeJsonLongArrays(existing.documentIdsJson, docIdJson)
                val updatedChunkIds = mergeJsonLongArrays(existing.chunkIdsJson, chunkIdJson)
                val updated = existing.copy(
                    documentIdsJson = updatedDocIds,
                    chunkIdsJson = updatedChunkIds,
                    frequency = existing.frequency + 1,
                    updatedAt = now
                )
                conceptDao.update(updated)
                stored.add(updated)
            } else {
                val entity = ConceptEntity(
                    name = concept.name.take(200),
                    normalizedName = normalized.take(200),
                    type = concept.type,
                    definition = concept.definition?.take(500),
                    documentIdsJson = docIdJson,
                    chunkIdsJson = chunkIdJson,
                    createdAt = now
                )
                val id = conceptDao.insert(entity)
                stored.add(entity.copy(id = id))
            }
        }

        return stored
    }

    /**
     * Extracts the outermost JSON array from a model response that may contain preamble text,
     * markdown fences, or explanatory prose.
     */
    private fun extractJsonArray(text: String): String {
        val stripped = text.replace("```json", "").replace("```", "").trim()
        val start = stripped.indexOf('[')
        val end = stripped.lastIndexOf(']')
        return if (start >= 0 && end > start) stripped.substring(start, end + 1) else "[]"
    }

    private fun mergeJsonLongArrays(existing: String, new: String): String {
        return try {
            val list1 = json.decodeFromString<List<Long>>(existing.ifBlank { "[]" })
            val list2 = json.decodeFromString<List<Long>>(new.ifBlank { "[]" })
            json.encodeToString(ListSerializer(Long.serializer()), (list1 + list2).distinct())
        } catch (e: Exception) {
            existing
        }
    }
}
