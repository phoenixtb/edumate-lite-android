package io.foxbird.edumate.domain.service

import android.net.Uri
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.data.local.converter.Converters
import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.local.dao.PageDao
import io.foxbird.edumate.data.local.entity.ChunkEntity
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.data.local.entity.PageEntity
import io.foxbird.edumate.domain.engine.ChunkingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class ProcessingEvent {
    data class Progress(val stage: String, val current: Int, val total: Int) : ProcessingEvent()
    data class Complete(val materialId: Long, val chunkCount: Int) : ProcessingEvent()
    data class Error(val message: String) : ProcessingEvent()
}

class MaterialProcessor(
    private val pdfAdapter: PdfInputAdapter,
    private val textAdapter: TextInputAdapter,
    private val chunkingEngine: ChunkingEngine,
    private val embeddingService: EmbeddingService,
    private val keywordExtractor: KeywordExtractor,
    private val materialDao: MaterialDao,
    private val chunkDao: ChunkDao,
    private val pageDao: PageDao
) {
    companion object {
        private const val TAG = "MaterialProcessor"
    }

    private val converters = Converters()
    private val json = Json { ignoreUnknownKeys = true }

    fun processPdf(uri: Uri, title: String, subject: String? = null): Flow<ProcessingEvent> = flow {
        val now = System.currentTimeMillis()
        val material = MaterialEntity(
            title = title,
            sourceType = "pdf",
            subject = subject,
            status = "processing",
            createdAt = now
        )
        val materialId = materialDao.insert(material)

        try {
            emit(ProcessingEvent.Progress("Extracting text", 0, 1))

            val pages = mutableListOf<ExtractedPage>()
            pdfAdapter.extractText(uri).collect { page ->
                pages.add(page)
                pageDao.insert(
                    PageEntity(
                        materialId = materialId,
                        pageNumber = page.pageNumber,
                        extractionMethod = page.extractionMethod,
                        createdAt = now
                    )
                )
            }

            val allText = pages.joinToString("\n\n") { it.text }
            val chunkCount = processContent(materialId, pages, allText) { stage, current, total ->
                emit(ProcessingEvent.Progress(stage, current, total))
            }

            materialDao.update(
                material.copy(
                    id = materialId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = chunkCount,
                    pageCount = pages.size,
                    totalWords = allText.split("\\s+".toRegex()).size,
                    keywordsJson = json.encodeToString(keywordExtractor.extractKeywords(allText))
                )
            )

            emit(ProcessingEvent.Complete(materialId, chunkCount))
        } catch (e: Exception) {
            Logger.e(TAG, "Processing failed", e)
            materialDao.updateStatus(materialId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun processText(text: String, title: String, subject: String? = null): Flow<ProcessingEvent> = flow {
        val now = System.currentTimeMillis()
        val material = MaterialEntity(
            title = title,
            sourceType = "text",
            subject = subject,
            status = "processing",
            createdAt = now
        )
        val materialId = materialDao.insert(material)

        try {
            val page = textAdapter.extractText(text)
            val chunkCount = processContent(materialId, listOf(page), text) { stage, current, total ->
                emit(ProcessingEvent.Progress(stage, current, total))
            }

            materialDao.update(
                material.copy(
                    id = materialId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = chunkCount,
                    pageCount = 1,
                    totalWords = text.split("\\s+".toRegex()).size,
                    keywordsJson = json.encodeToString(keywordExtractor.extractKeywords(text))
                )
            )

            emit(ProcessingEvent.Complete(materialId, chunkCount))
        } catch (e: Exception) {
            Logger.e(TAG, "Text processing failed", e)
            materialDao.updateStatus(materialId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun processContent(
        materialId: Long,
        pages: List<ExtractedPage>,
        allText: String,
        onProgress: suspend (String, Int, Int) -> Unit
    ): Int {
        // Chunking
        onProgress("Chunking text", 0, pages.size)
        val chunks = mutableListOf<io.foxbird.edumate.domain.engine.TextChunk>()
        for (page in pages) {
            if (page.text.isNotBlank()) {
                chunks.addAll(chunkingEngine.chunkText(page.text, page.pageNumber))
            }
        }

        if (chunks.isEmpty()) return 0

        // Extract per-chunk keywords for hybrid search
        val chunkKeywords = chunks.map { chunk ->
            keywordExtractor.extractKeywords(chunk.content, 5)
        }

        // Embedding
        onProgress("Generating embeddings", 0, chunks.size)
        val embeddings = embeddingService.embedBatch(
            chunks.map { it.content },
            batchSize = AppConstants.EMBEDDING_BATCH_SIZE
        ) { done, total ->
            onProgress("Generating embeddings", done, total)
        }

        // Store chunks
        onProgress("Saving chunks", 0, chunks.size)
        val chunkEntities = chunks.mapIndexed { index, chunk ->
            ChunkEntity(
                materialId = materialId,
                content = chunk.content,
                embedding = embeddings.getOrNull(index)?.let { converters.fromFloatArray(it) },
                pageNumber = chunk.pageNumber,
                sequenceIndex = chunk.sequenceIndex,
                chunkType = chunk.chunkType,
                wordCount = chunk.wordCount,
                sentenceCount = chunk.sentenceCount,
                tokenCount = chunkingEngine.countTokens(chunk.content),
                conceptTagsJson = json.encodeToString(chunkKeywords.getOrNull(index) ?: emptyList<String>())
            )
        }

        for (batch in chunkEntities.chunked(AppConstants.STORAGE_BATCH_SIZE)) {
            chunkDao.insertAll(batch)
        }

        materialDao.updateChunkCount(materialId, chunkEntities.size)
        return chunkEntities.size
    }
}
