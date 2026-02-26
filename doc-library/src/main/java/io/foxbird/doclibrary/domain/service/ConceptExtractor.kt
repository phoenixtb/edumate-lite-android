package io.foxbird.doclibrary.domain.service

import io.foxbird.doclibrary.data.local.dao.ConceptDao
import io.foxbird.doclibrary.data.local.entity.ConceptEntity
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
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

private const val CONCEPT_EXTRACTION_PROMPT = """System: Extract key concepts from the following text. Return a JSON array of objects with fields: name, type (one of: term, person, formula, theorem, concept, event, place, definition), and definition (brief). Return ONLY valid JSON, no explanation."""

class ConceptExtractor(
    private val orchestrator: EngineOrchestrator,
    private val conceptDao: ConceptDao
) {
    companion object {
        private const val TAG = "ConceptExtractor"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun extractAndStore(
        text: String,
        documentId: Long,
        chunkIds: List<Long>
    ): List<ConceptEntity> {
        val prompt = "$CONCEPT_EXTRACTION_PROMPT\n\nText:\n$text\n\nJSON:"
        val result = orchestrator.generateComplete(
            prompt = prompt,
            params = GenerationParams(maxTokens = 512, temperature = 0.2f)
        )

        return result.fold(
            ifLeft = { error ->
                Logger.e(TAG, "Extraction failed: ${error.message}")
                emptyList()
            },
            ifRight = { response ->
                parseAndMergeConcepts(response, documentId, chunkIds)
            }
        )
    }

    private suspend fun parseAndMergeConcepts(
        response: String,
        documentId: Long,
        chunkIds: List<Long>
    ): List<ConceptEntity> {
        val extracted = try {
            val jsonStr = extractJsonArray(response)
            json.decodeFromString<List<ExtractedConcept>>(jsonStr)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse concepts JSON", e)
            emptyList()
        }

        val stored = mutableListOf<ConceptEntity>()
        val now = System.currentTimeMillis()
        val docIdJson = "[$documentId]"
        val chunkIdJson = json.encodeToString(ListSerializer(Long.serializer()), chunkIds)

        for (concept in extracted) {
            val normalized = concept.name.lowercase().trim()
            val existing = conceptDao.getByNormalizedName(normalized)

            if (existing != null) {
                val updatedDocIds = mergeJsonArrays(existing.documentIdsJson, docIdJson)
                val updatedChunkIds = mergeJsonArrays(existing.chunkIdsJson, chunkIdJson)
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
                    name = concept.name,
                    normalizedName = normalized,
                    type = concept.type,
                    definition = concept.definition,
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

    private fun extractJsonArray(text: String): String {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else "[]"
    }

    private fun mergeJsonArrays(existing: String, new: String): String {
        return try {
            val list1 = json.decodeFromString<List<Long>>(existing)
            val list2 = json.decodeFromString<List<Long>>(new)
            json.encodeToString(ListSerializer(Long.serializer()), (list1 + list2).distinct())
        } catch (e: Exception) {
            existing
        }
    }
}
