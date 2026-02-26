package io.foxbird.doclibrary.domain.processor

import android.net.Uri
import io.foxbird.doclibrary.data.local.dao.PageDao
import io.foxbird.doclibrary.data.local.entity.ChunkEntity
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.doclibrary.data.local.entity.PageEntity
import io.foxbird.doclibrary.data.local.converter.Converters
import io.foxbird.doclibrary.data.repository.ChunkRepository
import io.foxbird.doclibrary.domain.engine.TextChunk
import io.foxbird.doclibrary.data.repository.DocumentRepository
import io.foxbird.doclibrary.domain.adapter.ExtractedPage
import io.foxbird.doclibrary.domain.adapter.ImageInputAdapter
import io.foxbird.doclibrary.domain.adapter.PdfInputAdapter
import io.foxbird.doclibrary.domain.adapter.TextInputAdapter
import io.foxbird.doclibrary.domain.engine.IChunkingEngine
import io.foxbird.doclibrary.domain.engine.KeywordExtractor
import io.foxbird.edgeai.embedding.IEmbeddingService
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TARGET_CHUNK_SIZE_TOKENS = 1800
private const val MAX_CHUNK_SIZE_TOKENS = 1950
private const val CHUNK_OVERLAP_CHARS = 200
private const val EMBEDDING_BATCH_SIZE = 20
private const val STORAGE_BATCH_SIZE = 50

class DocumentProcessor(
    private val pdfAdapter: PdfInputAdapter,
    private val textAdapter: TextInputAdapter,
    private val imageAdapter: ImageInputAdapter,
    private val chunkingEngine: IChunkingEngine,
    private val embeddingService: IEmbeddingService,
    private val keywordExtractor: KeywordExtractor,
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
    private val pageDao: PageDao
) : IDocumentProcessor {

    companion object {
        private const val TAG = "DocumentProcessor"
    }

    private val converters = Converters()
    private val json = Json { ignoreUnknownKeys = true }

    override fun processPdf(
        uri: Uri,
        title: String,
        subject: String?,
        gradeLevel: Int?,
        mode: ProcessingMode
    ): Flow<ProcessingEvent> = flow {
        val now = System.currentTimeMillis()
        val document = DocumentEntity(
            title = title, sourceType = "pdf", subject = subject,
            gradeLevel = gradeLevel, processingMode = mode.name.lowercase(),
            status = "processing", createdAt = now
        )
        val documentId = documentRepository.insert(document)

        try {
            emit(ProcessingEvent.Progress("Extracting text", 0, 1))

            val pages = mutableListOf<ExtractedPage>()
            pdfAdapter.extractText(uri).collect { page ->
                pages.add(page)
                pageDao.insert(
                    PageEntity(
                        documentId = documentId,
                        pageNumber = page.pageNumber,
                        extractionMethod = page.extractionMethod,
                        createdAt = now
                    )
                )
            }

            val allText = pages.joinToString("\n\n") { it.text }
            val chunkCount = processContent(documentId, pages, allText) { stage, current, total ->
                emit(ProcessingEvent.Progress(stage, current, total))
            }

            documentRepository.update(
                document.copy(
                    id = documentId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = chunkCount,
                    pageCount = pages.size,
                    totalWords = allText.split("\\s+".toRegex()).size,
                    keywordsJson = json.encodeToString(keywordExtractor.extractKeywords(allText))
                )
            )
            emit(ProcessingEvent.Complete(documentId, chunkCount))
        } catch (e: Exception) {
            Logger.e(TAG, "PDF processing failed", e)
            documentRepository.updateStatus(documentId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun processImages(
        uris: List<Uri>,
        title: String,
        subject: String?,
        gradeLevel: Int?,
        mode: ProcessingMode
    ): Flow<ProcessingEvent> = flow {
        val now = System.currentTimeMillis()
        val document = DocumentEntity(
            title = title, sourceType = "image", subject = subject,
            gradeLevel = gradeLevel, processingMode = mode.name.lowercase(),
            status = "processing", createdAt = now
        )
        val documentId = documentRepository.insert(document)

        try {
            emit(ProcessingEvent.Progress("Loading images", 0, uris.size))

            val pages = mutableListOf<ExtractedPage>()
            uris.forEachIndexed { index, uri ->
                emit(ProcessingEvent.Progress("Loading images", index + 1, uris.size))
                // For MVP: each image becomes a placeholder page.
                // THOROUGH mode will add OCR here in a future iteration.
                pages.add(ExtractedPage(pageNumber = index + 1, text = "", extractionMethod = "image"))
                pageDao.insert(
                    PageEntity(
                        documentId = documentId,
                        pageNumber = index + 1,
                        extractionMethod = "image",
                        createdAt = now
                    )
                )
            }

            val allText = pages.joinToString("\n\n") { it.text }.trim()

            val chunkCount = if (allText.isNotBlank()) {
                processContent(documentId, pages, allText) { stage, current, total ->
                    emit(ProcessingEvent.Progress(stage, current, total))
                }
            } else 0

            documentRepository.update(
                document.copy(
                    id = documentId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = chunkCount,
                    pageCount = uris.size
                )
            )
            emit(ProcessingEvent.Complete(documentId, chunkCount))
        } catch (e: Exception) {
            Logger.e(TAG, "Image processing failed", e)
            documentRepository.updateStatus(documentId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Image processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun processText(
        text: String,
        title: String,
        subject: String?
    ): Flow<ProcessingEvent> = flow {
        val now = System.currentTimeMillis()
        val document = DocumentEntity(
            title = title, sourceType = "text", subject = subject,
            status = "processing", createdAt = now
        )
        val documentId = documentRepository.insert(document)

        try {
            val page = textAdapter.extractText(text)
            val chunkCount = processContent(documentId, listOf(page), text) { stage, current, total ->
                emit(ProcessingEvent.Progress(stage, current, total))
            }

            documentRepository.update(
                document.copy(
                    id = documentId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = chunkCount,
                    pageCount = 1,
                    totalWords = text.split("\\s+".toRegex()).size,
                    keywordsJson = json.encodeToString(keywordExtractor.extractKeywords(text))
                )
            )
            emit(ProcessingEvent.Complete(documentId, chunkCount))
        } catch (e: Exception) {
            Logger.e(TAG, "Text processing failed", e)
            documentRepository.updateStatus(documentId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun processContent(
        documentId: Long,
        pages: List<ExtractedPage>,
        allText: String,
        onProgress: suspend (String, Int, Int) -> Unit
    ): Int {
        onProgress("Chunking text", 0, pages.size)
        val chunks = mutableListOf<TextChunk>()
        for (page in pages) {
            if (page.text.isNotBlank()) {
                chunks.addAll(
                    chunkingEngine.chunkText(
                        text = page.text,
                        pageNumber = page.pageNumber,
                        targetTokens = TARGET_CHUNK_SIZE_TOKENS,
                        maxTokens = MAX_CHUNK_SIZE_TOKENS,
                        overlapChars = CHUNK_OVERLAP_CHARS
                    )
                )
            }
        }

        if (chunks.isEmpty()) return 0

        val chunkKeywords = chunks.map { keywordExtractor.extractKeywords(it.content, 5) }

        onProgress("Generating embeddings", 0, chunks.size)
        var embeddingFailures = 0
        val embeddingResults = embeddingService.embedBatch(
            texts = chunks.map { it.content },
            batchSize = EMBEDDING_BATCH_SIZE
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
                documentId = documentId,
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

        for (batch in chunkEntities.chunked(STORAGE_BATCH_SIZE)) {
            chunkRepository.insertAll(batch)
        }

        if (embeddingFailures > 0) {
            Logger.w(TAG, "$embeddingFailures/${chunks.size} chunks stored without embedding")
        }

        documentRepository.updateChunkCount(documentId, chunkEntities.size)
        return chunkEntities.size
    }
}
