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
import io.foxbird.doclibrary.domain.extractor.ExtractionStrategySelector
import io.foxbird.doclibrary.domain.engine.KeywordExtractor
import io.foxbird.doclibrary.domain.service.ConceptExtractor
import io.foxbird.doclibrary.domain.task.AiTask
import io.foxbird.doclibrary.domain.task.TaskQueue
import io.foxbird.doclibrary.domain.task.TaskType
import io.foxbird.edgeai.embedding.IEmbeddingService
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Chunk/embedding constants are now in DocumentProcessorConfig (injected at construction time).

/**
 * @param chunks Each entry is (inserted DB id, chunk content). Passed to concept extraction so
 *   the extractor can process every chunk and link concepts back to exact chunk IDs.
 */
private data class ProcessContentResult(
    val chunkCount: Int,
    val embeddedCount: Int,
    val chunks: List<Pair<Long, String>>
)

class DocumentProcessor(
    private val pdfAdapter: PdfInputAdapter,
    private val textAdapter: TextInputAdapter,
    private val imageAdapter: ImageInputAdapter,
    private val chunkingEngine: IChunkingEngine,
    private val embeddingService: IEmbeddingService,
    private val keywordExtractor: KeywordExtractor,
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
    private val pageDao: PageDao,
    private val conceptExtractor: ConceptExtractor,
    private val taskQueue: TaskQueue,
    private val orchestrator: EngineOrchestrator,
    private val strategySelector: ExtractionStrategySelector,
    private val config: DocumentProcessorConfig = DocumentProcessorConfig()
) : IDocumentProcessor {

    companion object {
        private const val TAG = "DocumentProcessor"

        private val EQUATION_PATTERNS = listOf(
            "\\\\[a-zA-Z]+\\{".toRegex(),
            "\\$[^$\n]{2,}\\$".toRegex(),
            "\\\\(?:frac|sum|int|sqrt|alpha|beta|gamma|theta|pi|sigma|delta|lambda)".toRegex()
        )
        private val TABLE_PATTERNS = listOf(
            "(?m)^(\\|[^|\\n]+){2,}\\|$".toRegex(),
            "(?m)^[-|+]{5,}$".toRegex()
        )
        private val CODE_PATTERNS = listOf(
            "```".toRegex(),
            "(?m)^(    |\\t)\\S".toRegex(),
            "(?m)^(?:def |class |import |public |private |protected |function |const |var |let )".toRegex()
        )
        private val DIAGRAM_PATTERNS = listOf(
            "(?i)(?:figure|fig\\.|diagram|chart|graph|illustration)\\s+\\d+".toRegex()
        )
    }

    private val _processingState = MutableStateFlow<ProcessingState?>(null)
    override val processingState: StateFlow<ProcessingState?> = _processingState.asStateFlow()

    private val converters = Converters()
    private val json = Json { ignoreUnknownKeys = true }

    private fun pushProgress(title: String, stage: String, current: Int, total: Int) {
        _processingState.value = ProcessingState(title, stage, current, total)
    }

    private fun clearProcessingState() { _processingState.value = null }

    /**
     * Returns effective chunk size limits: the smaller of the configured constants and the embedding
     * model's context window, so chunked text is never silently truncated during embedding.
     */
    private fun effectiveChunkLimits(): Pair<Int, Int> {
        val embCtx = orchestrator.getEmbeddingContextSize()
        val target = if (embCtx > 0) minOf(config.targetChunkSizeTokens, embCtx) else config.targetChunkSizeTokens
        val max = if (embCtx > 0) minOf(config.maxChunkSizeTokens, embCtx) else config.maxChunkSizeTokens
        return target to max
    }

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
            pushProgress(title, "Extracting text", 0, 1)
            emit(ProcessingEvent.Progress("Extracting text", 0, 1))

            val rawPages = mutableListOf<ExtractedPage>()

            val textPages = mutableListOf<ExtractedPage>()
            pdfAdapter.extractText(uri).collect { textPages.add(it) }

            val total = textPages.size
            val needsExtractorCount = textPages.count { strategySelector.select(mode, it) != null }
            if (needsExtractorCount > 0 && mode == ProcessingMode.FAST) {
                emit(ProcessingEvent.Progress(
                    "Running OCR on $needsExtractorCount page(s)",
                    0, needsExtractorCount
                ))
            }

            var extractorUsed = 0
            textPages.forEachIndexed { idx, textPage ->
                if (mode == ProcessingMode.THOROUGH) {
                    val label = "Processing page ${idx + 1}/$total"
                    pushProgress(title, label, idx + 1, total)
                    emit(ProcessingEvent.Progress(label, idx + 1, total))
                }

                val extractor = strategySelector.select(mode, textPage)
                val finalPage = if (extractor != null) {
                    val bitmap = pdfAdapter.renderPageToImage(uri, idx)
                    if (bitmap != null) {
                        val text = extractor.extract(bitmap)
                        bitmap.recycle()
                        extractorUsed++
                        if (mode == ProcessingMode.FAST) {
                            emit(ProcessingEvent.Progress(
                                "${extractor.name.uppercase()} page ${idx + 1}",
                                extractorUsed, needsExtractorCount
                            ))
                        }
                        textPage.copy(
                            text = text,
                            extractionMethod = if (text.isBlank()) "${extractor.name}_empty" else extractor.name,
                            isScanned = text.isBlank()
                        )
                    } else {
                        textPage.copy(extractionMethod = "render_failed")
                    }
                } else {
                    textPage
                }
                rawPages.add(finalPage)
            }

            // Hash was computed as a side-channel during extractText(); use it for deduplication.
            val fileHash = pdfAdapter.lastComputedHash
            if (fileHash != null) {
                val existing = documentRepository.findByFileHash(fileHash)
                if (existing != null && existing.id != documentId) {
                    documentRepository.updateStatus(documentId, "duplicate")
                    emit(ProcessingEvent.Duplicate(existing.id, existing.title))
                    return@flow
                }
            }

            // Insert page entities; capture DB-assigned IDs
            val insertedPageEntities = mutableListOf<PageEntity>()
            rawPages.forEach { page ->
                val entity = PageEntity(
                    documentId = documentId,
                    pageNumber = page.pageNumber,
                    extractionMethod = page.extractionMethod,
                    createdAt = now
                )
                val id = pageDao.insert(entity)
                insertedPageEntities.add(entity.copy(id = id))
            }

            val stitchedPages = stitchPageBoundaries(rawPages)
            val allText = stitchedPages.joinToString("\n\n") { it.text }

            val result = processContent(
                documentId, stitchedPages, rawPages, insertedPageEntities
            ) { stage, current, total ->
                pushProgress(title, stage, current, total)
                emit(ProcessingEvent.Progress(stage, current, total))
            }

            val totalWords = allText.split("\\s+".toRegex()).count { it.isNotBlank() }
            val totalTokens = chunkingEngine.countTokens(allText)
            val nonEmptyPages = rawPages.count { it.text.isNotBlank() }
            val extractionQuality = if (rawPages.isEmpty()) 0.0 else nonEmptyPages.toDouble() / rawPages.size

            documentRepository.update(
                document.copy(
                    id = documentId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = result.chunkCount,
                    embeddedChunkCount = result.embeddedCount,
                    pageCount = rawPages.size,
                    totalWords = totalWords,
                    totalTokens = totalTokens,
                    extractionQuality = extractionQuality,
                    fileHash = fileHash,
                    keywordsJson = json.encodeToString(keywordExtractor.extractKeywords(allText)),
                    conceptExtractionPending = result.chunks.isNotEmpty()
                )
            )
            clearProcessingState()
            emit(ProcessingEvent.Complete(documentId, result.chunkCount))

            if (result.chunks.isNotEmpty()) enqueueConceptExtraction(documentId, result.chunks)
        } catch (e: Exception) {
            Logger.e(TAG, "PDF processing failed", e)
            clearProcessingState()
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
            val rawPages = mutableListOf<ExtractedPage>()
            val insertedPageEntities = mutableListOf<PageEntity>()

            val emptyPage = ExtractedPage(pageNumber = 0, text = "", isScanned = true)
            uris.forEachIndexed { idx, uri ->
                val label = "Extracting image ${idx + 1}/${uris.size}"
                pushProgress(title, label, idx + 1, uris.size)
                emit(ProcessingEvent.Progress(label, idx + 1, uris.size))

                // Images never have pre-existing text; selector always returns an extractor.
                val extractor = strategySelector.select(mode, emptyPage)!!
                val bitmap = imageAdapter.loadImage(uri)
                val page = if (bitmap != null) {
                    val text = extractor.extract(bitmap)
                    bitmap.recycle()
                    ExtractedPage(
                        pageNumber = idx + 1,
                        text = text.trim(),
                        extractionMethod = if (text.isBlank()) "${extractor.name}_empty" else extractor.name,
                        isScanned = text.isBlank()
                    )
                } else {
                    ExtractedPage(
                        pageNumber = idx + 1,
                        text = "",
                        extractionMethod = "load_failed",
                        isScanned = true
                    )
                }
                rawPages.add(page)

                val entity = PageEntity(
                    documentId = documentId,
                    pageNumber = idx + 1,
                    extractionMethod = page.extractionMethod,
                    createdAt = now
                )
                val id = pageDao.insert(entity)
                insertedPageEntities.add(entity.copy(id = id))
            }

            val allText = rawPages.joinToString("\n\n") { it.text }.trim()
            val result = if (allText.isNotBlank()) {
                processContent(documentId, rawPages, rawPages, insertedPageEntities) { stage, current, total ->
                    pushProgress(title, stage, current, total)
                    emit(ProcessingEvent.Progress(stage, current, total))
                }
            } else ProcessContentResult(0, 0, emptyList())

            val totalWords = allText.split("\\s+".toRegex()).count { it.isNotBlank() }
            val nonEmptyPages = rawPages.count { it.text.isNotBlank() }
            val extractionQuality = if (rawPages.isEmpty()) 0.0 else nonEmptyPages.toDouble() / rawPages.size

            documentRepository.update(
                document.copy(
                    id = documentId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = result.chunkCount,
                    embeddedChunkCount = result.embeddedCount,
                    pageCount = uris.size,
                    totalWords = totalWords,
                    totalTokens = chunkingEngine.countTokens(allText),
                    extractionQuality = extractionQuality,
                    keywordsJson = if (allText.isNotBlank())
                        json.encodeToString(keywordExtractor.extractKeywords(allText)) else null,
                    conceptExtractionPending = result.chunks.isNotEmpty()
                )
            )
            clearProcessingState()
            emit(ProcessingEvent.Complete(documentId, result.chunkCount))

            if (result.chunks.isNotEmpty()) enqueueConceptExtraction(documentId, result.chunks)
        } catch (e: Exception) {
            Logger.e(TAG, "Image processing failed", e)
            clearProcessingState()
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
            val pageEntity = PageEntity(
                documentId = documentId, pageNumber = 1,
                extractionMethod = "text", createdAt = now
            )
            val pageId = pageDao.insert(pageEntity)
            val insertedPage = pageEntity.copy(id = pageId)

            val result = processContent(documentId, listOf(page), listOf(page), listOf(insertedPage)) { stage, current, total ->
                pushProgress(title, stage, current, total)
                emit(ProcessingEvent.Progress(stage, current, total))
            }

            val totalWords = text.split("\\s+".toRegex()).count { it.isNotBlank() }
            documentRepository.update(
                document.copy(
                    id = documentId,
                    status = "completed",
                    processedAt = System.currentTimeMillis(),
                    chunkCount = result.chunkCount,
                    embeddedChunkCount = result.embeddedCount,
                    pageCount = 1,
                    totalWords = totalWords,
                    totalTokens = chunkingEngine.countTokens(text),
                    extractionQuality = 1.0,
                    keywordsJson = json.encodeToString(keywordExtractor.extractKeywords(text)),
                    conceptExtractionPending = true
                )
            )
            clearProcessingState()
            emit(ProcessingEvent.Complete(documentId, result.chunkCount))
            if (result.chunks.isNotEmpty()) enqueueConceptExtraction(documentId, result.chunks)
        } catch (e: Exception) {
            Logger.e(TAG, "Text processing failed", e)
            clearProcessingState()
            documentRepository.updateStatus(documentId, "failed", e.message)
            emit(ProcessingEvent.Error(e.message ?: "Processing failed"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stitches the last line of page N into page N+1 when page N ends without sentence-terminating
     * punctuation, preventing mid-sentence splits at page boundaries.
     * Returns new pages for chunking; the original list is unchanged.
     */
    private fun stitchPageBoundaries(pages: List<ExtractedPage>): List<ExtractedPage> {
        if (pages.size <= 1) return pages.filter { it.text.isNotBlank() }
        val result = pages.map { it }.toMutableList()
        for (i in 0 until result.size - 1) {
            val current = result[i]
            val next = result[i + 1]
            if (current.text.isBlank() || next.text.isBlank()) continue
            val lastChar = current.text.trimEnd().lastOrNull() ?: continue
            if (lastChar !in setOf('.', '!', '?', ':', ';')) {
                result[i] = current.copy(text = current.text.trimEnd() + " " + next.text.trimStart())
                result[i + 1] = next.copy(text = "")
            }
        }
        return result.filter { it.text.isNotBlank() }
    }

    private suspend fun processContent(
        documentId: Long,
        chunkingPages: List<ExtractedPage>,
        originalPages: List<ExtractedPage>,
        insertedPageEntities: List<PageEntity>,
        onProgress: suspend (String, Int, Int) -> Unit
    ): ProcessContentResult {
        onProgress("Chunking text", 0, chunkingPages.size)

        val (targetTokens, maxTokens) = effectiveChunkLimits()

        val chunks = mutableListOf<TextChunk>()
        for (page in chunkingPages) {
            if (page.text.isNotBlank()) {
                chunks.addAll(
                    chunkingEngine.chunkText(
                        text = page.text,
                        pageNumber = page.pageNumber,
                        targetTokens = targetTokens,
                        maxTokens = maxTokens,
                        overlapTokens = config.chunkOverlapTokens
                    )
                )
            }
        }

        if (chunks.isEmpty()) return ProcessContentResult(0, 0, emptyList())

        updatePageMetadata(originalPages, insertedPageEntities, chunks)

        val chunkKeywords = chunks.map { keywordExtractor.extractKeywords(it.content, 5) }

        onProgress("Generating embeddings", 0, chunks.size)
        val embeddingResults = embeddingService.embedBatch(
            texts = chunks.map { it.content },
            batchSize = config.embeddingBatchSize
        ) { done, total ->
            onProgress("Generating embeddings", done, total)
        }

        onProgress("Saving chunks", 0, chunks.size)
        var embeddedCount = 0
        val chunkEntities = chunks.mapIndexed { index, chunk ->
            val embeddingBytes = embeddingResults.getOrNull(index)?.fold(
                ifLeft = { error ->
                    Logger.w(TAG, "Embedding failed for chunk $index: ${error.message}")
                    null
                },
                ifRight = {
                    embeddedCount++
                    converters.fromFloatArray(it)
                }
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
                // keywordsJson stores TF-IDF keywords; conceptTagsJson is for linked concept names
                keywordsJson = json.encodeToString(chunkKeywords.getOrNull(index) ?: emptyList<String>())
            )
        }

        val savedChunks = mutableListOf<Pair<Long, String>>()
        for (batch in chunkEntities.chunked(config.storageBatchSize)) {
            val ids = chunkRepository.insertAll(batch)
            batch.zip(ids).mapTo(savedChunks) { (entity, id) -> id to entity.content }
        }

        val embeddingFailures = chunks.size - embeddedCount
        if (embeddingFailures > 0) {
            Logger.w(TAG, "$embeddingFailures/${chunks.size} chunks stored without embedding")
        }

        documentRepository.updateChunkCount(documentId, chunkEntities.size)
        return ProcessContentResult(chunkEntities.size, embeddedCount, savedChunks)
    }

    private suspend fun updatePageMetadata(
        originalPages: List<ExtractedPage>,
        insertedPageEntities: List<PageEntity>,
        chunks: List<TextChunk>
    ) {
        val chunksByPage = chunks.groupBy { it.pageNumber }
        val entityByPageNumber = insertedPageEntities.associateBy { it.pageNumber }

        for (page in originalPages) {
            val entity = entityByPageNumber[page.pageNumber] ?: continue
            val text = page.text
            if (text.isBlank()) continue

            val pageChunks = chunksByPage[page.pageNumber] ?: emptyList()
            val wordCount = text.split("\\s+".toRegex()).count { it.isNotBlank() }
            val textDensity = if (text.isNotEmpty()) wordCount.toDouble() / text.length else 0.0

            pageDao.update(
                entity.copy(
                    extractionMethod = page.extractionMethod,
                    hasEquations = EQUATION_PATTERNS.any { it.containsMatchIn(text) },
                    hasTables = TABLE_PATTERNS.any { it.containsMatchIn(text) },
                    hasCode = CODE_PATTERNS.any { it.containsMatchIn(text) },
                    hasDiagrams = DIAGRAM_PATTERNS.any { it.containsMatchIn(text) },
                    textDensity = textDensity,
                    chunkCount = pageChunks.size
                )
            )
        }
    }

    private fun enqueueConceptExtraction(documentId: Long, chunks: List<Pair<Long, String>>) {
        taskQueue.enqueue(
            AiTask(
                id = "concept_$documentId",
                type = TaskType.CONCEPT_EXTRACTION,
                title = "Extracting concepts for document $documentId"
            ) { _ ->
                conceptExtractor.extractAndStoreByChunks(documentId, chunks)
                documentRepository.updateConceptExtractionPending(documentId, false)
            }
        )
    }
}
