package io.foxbird.edumate.domain.service

import android.net.Uri
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.data.local.converter.Converters
import io.foxbird.edumate.data.local.entity.ChunkEntity
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.data.local.entity.PageEntity
import io.foxbird.edumate.data.repository.ChunkRepository
import io.foxbird.edumate.data.repository.MaterialRepository
import io.foxbird.edumate.data.local.dao.PageDao
import io.foxbird.edumate.domain.engine.IChunkingEngine
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
    private val chunkingEngine: IChunkingEngine,
    private val embeddingService: IEmbeddingService,
    private val keywordExtractor: KeywordExtractor,
    private val materialRepository: MaterialRepository,
    private val chunkRepository: ChunkRepository,
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
            title = title, sourceType = "pdf", subject = subject,
            status = "processing", createdAt = now
        )
        val materialId = materialRepository.insert(material)

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

            materialRepository.update(
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
            materialRepository.updateStatus(materialId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun processText(text: String, title: String, subject: String? = null): Flow<ProcessingEvent> = flow {
        val now = System.currentTimeMillis()
        val material = MaterialEntity(
            title = title, sourceType = "text", subject = subject,
            status = "processing", createdAt = now
        )
        val materialId = materialRepository.insert(material)

        try {
            val page = textAdapter.extractText(text)
            val chunkCount = processContent(materialId, listOf(page), text) { stage, current, total ->
                emit(ProcessingEvent.Progress(stage, current, total))
            }

            materialRepository.update(
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
            materialRepository.updateStatus(materialId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun processContent(
        materialId: Long,
        pages: List<ExtractedPage>,
        allText: String,
        onProgress: suspend (String, Int, Int) -> Unit
    ): Int {
        onProgress("Chunking text", 0, pages.size)
        val chunks = mutableListOf<io.foxbird.edumate.domain.engine.TextChunk>()
        for (page in pages) {
            if (page.text.isNotBlank()) {
                chunks.addAll(
                    chunkingEngine.chunkText(
                        text = page.text,
                        pageNumber = page.pageNumber,
                        targetTokens = AppConstants.TARGET_CHUNK_SIZE_TOKENS,
                        maxTokens = AppConstants.MAX_CHUNK_SIZE_TOKENS,
                        overlapChars = AppConstants.CHUNK_OVERLAP_CHARS
                    )
                )
            }
        }

        if (chunks.isEmpty()) return 0

        val chunkKeywords = chunks.map { keywordExtractor.extractKeywords(it.content, 5) }

        // Embedding — each failure is captured as an AppResult.Left; stored as null embedding.
        // Callers receive partial progress events so the user knows if embedding fails.
        onProgress("Generating embeddings", 0, chunks.size)
        var embeddingFailures = 0
        val embeddingResults = embeddingService.embedBatch(
            texts = chunks.map { it.content },
            batchSize = AppConstants.EMBEDDING_BATCH_SIZE
        ) { done, total ->
            onProgress("Generating embeddings", done, total)
        }

        onProgress("Saving chunks", 0, chunks.size)
        val chunkEntities = chunks.mapIndexed { index, chunk ->
            val embeddingResult = embeddingResults.getOrNull(index)
            val embeddingBytes = embeddingResult?.fold(
                ifLeft = { error ->
                    Logger.w(TAG, "Embedding failed for chunk $index: ${error.message}")
                    embeddingFailures++
                    null
                },
                ifRight = { converters.fromFloatArray(it) }
            )

            ChunkEntity(
                materialId = materialId,
                content = chunk.content,
                embedding = embeddingBytes,
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
            chunkRepository.insertAll(batch)
        }

        if (embeddingFailures > 0) {
            Logger.w(TAG, "$embeddingFailures/${chunks.size} chunks stored without embedding — RAG retrieval will be partial")
        }

        materialRepository.updateChunkCount(materialId, chunkEntities.size)
        return chunkEntities.size
    }
}
