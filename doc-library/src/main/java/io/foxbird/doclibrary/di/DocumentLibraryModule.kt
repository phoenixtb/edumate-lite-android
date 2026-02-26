package io.foxbird.doclibrary.di

import androidx.room.Room
import io.foxbird.doclibrary.data.local.DocumentDatabase
import io.foxbird.doclibrary.data.repository.ChunkRepository
import io.foxbird.doclibrary.data.repository.DocumentRepository
import io.foxbird.doclibrary.data.repository.RoomChunkRepository
import io.foxbird.doclibrary.data.repository.RoomDocumentRepository
import io.foxbird.doclibrary.domain.adapter.ImageInputAdapter
import io.foxbird.doclibrary.domain.adapter.PdfInputAdapter
import io.foxbird.doclibrary.domain.adapter.TextInputAdapter
import io.foxbird.doclibrary.domain.engine.BM25Scorer
import io.foxbird.doclibrary.domain.engine.ChunkingEngine
import io.foxbird.doclibrary.domain.engine.IChunkingEngine
import io.foxbird.doclibrary.domain.engine.KeywordExtractor
import io.foxbird.doclibrary.domain.engine.OrchestratorTokenCounter
import io.foxbird.doclibrary.domain.processor.DocumentProcessor
import io.foxbird.doclibrary.domain.processor.IDocumentProcessor
import io.foxbird.doclibrary.domain.rag.IRagEngine
import io.foxbird.doclibrary.domain.rag.RagConfig
import io.foxbird.doclibrary.domain.rag.RagEngine
import io.foxbird.doclibrary.domain.rag.VectorSearchEngine
import io.foxbird.doclibrary.domain.service.ConceptExtractor
import io.foxbird.doclibrary.domain.task.TaskQueue
import io.foxbird.doclibrary.viewmodel.DocumentDetailViewModel
import io.foxbird.doclibrary.viewmodel.LibraryViewModel
import io.foxbird.edgeai.embedding.IEmbeddingService
import io.foxbird.edgeai.engine.EngineOrchestrator
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val documentDatabaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), DocumentDatabase::class.java, "doc_library.db").build()
    }
    single { get<DocumentDatabase>().documentDao() }
    single { get<DocumentDatabase>().chunkDao() }
    single { get<DocumentDatabase>().pageDao() }
    single { get<DocumentDatabase>().conceptDao() }
}

private val documentRepositoryModule = module {
    single<DocumentRepository> { RoomDocumentRepository(get()) }
    single<ChunkRepository> { RoomChunkRepository(get()) }
}

private val documentDomainModule = module {
    single { PdfInputAdapter(androidContext()) }
    single { TextInputAdapter() }
    single { ImageInputAdapter(androidContext()) }
    single { KeywordExtractor() }
    single { BM25Scorer() }
    single { OrchestratorTokenCounter(get<EngineOrchestrator>()) }
    single<IChunkingEngine> { ChunkingEngine(get<OrchestratorTokenCounter>()) }
    single<IDocumentProcessor> {
        DocumentProcessor(
            pdfAdapter = get(),
            textAdapter = get(),
            imageAdapter = get(),
            chunkingEngine = get<IChunkingEngine>(),
            embeddingService = get<IEmbeddingService>(),
            keywordExtractor = get(),
            documentRepository = get<DocumentRepository>(),
            chunkRepository = get<ChunkRepository>(),
            pageDao = get()
        )
    }
    single { ConceptExtractor(get<EngineOrchestrator>(), get()) }
    single { TaskQueue(get<CoroutineScope>(named("appScope"))) }
}

private val documentRagModule = module {
    single { VectorSearchEngine(get<ChunkRepository>()) }
    single<IRagEngine> {
        RagEngine(
            vectorSearch = get(),
            bm25Scorer = get(),
            embeddingService = get<IEmbeddingService>(),
            orchestrator = get<EngineOrchestrator>(),
            chunkingEngine = get<IChunkingEngine>(),
            config = getOrNull<RagConfig>() ?: RagConfig()
        )
    }
}

private val documentViewModelModule = module {
    viewModel {
        LibraryViewModel(
            documentRepository = get<DocumentRepository>(),
            documentProcessor = get<IDocumentProcessor>(),
            appScope = get<CoroutineScope>(named("appScope"))
        )
    }
    viewModel { (id: Long) ->
        DocumentDetailViewModel(
            documentId = id,
            documentRepository = get<DocumentRepository>(),
            chunkRepository = get<ChunkRepository>(),
            conceptDao = get()
        )
    }
}

/**
 * Returns all Koin modules for the doc-library.
 * The host app must also provide:
 *   - [EngineOrchestrator] (from :edge-ai-core's engine module)
 *   - [IEmbeddingService] (from :edge-ai-core)
 *   - [CoroutineScope] qualified with named("appScope")
 *   - Optionally: [RagConfig] (defaults to generic prompt if not provided)
 */
fun documentLibraryKoinModules(): List<Module> = listOf(
    documentDatabaseModule,
    documentRepositoryModule,
    documentDomainModule,
    documentRagModule,
    documentViewModelModule
)

