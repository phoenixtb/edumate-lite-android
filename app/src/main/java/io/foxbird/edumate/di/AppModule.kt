package io.foxbird.edumate.di

import androidx.room.Room
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.LiteRtEmbeddingEngine
import io.foxbird.edgeai.engine.LiteRtGenerationEngine
import io.foxbird.edgeai.engine.LlamaCppEngine
import io.foxbird.edgeai.engine.MemoryMonitor
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.ModelDownloader
import io.foxbird.edgeai.util.DeviceInfo
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.data.local.EduMateDatabase
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import io.foxbird.edumate.data.repository.ChunkRepository
import io.foxbird.edumate.data.repository.ConversationRepository
import io.foxbird.edumate.data.repository.MaterialRepository
import io.foxbird.edumate.data.repository.MessageRepository
import io.foxbird.edumate.data.repository.RoomChunkRepository
import io.foxbird.edumate.data.repository.RoomConversationRepository
import io.foxbird.edumate.data.repository.RoomMaterialRepository
import io.foxbird.edumate.data.repository.RoomMessageRepository
import io.foxbird.edumate.domain.engine.BM25Scorer
import io.foxbird.edumate.domain.engine.ChunkingEngine
import io.foxbird.edumate.domain.engine.IChunkingEngine
import io.foxbird.edumate.domain.engine.IRagEngine
import io.foxbird.edumate.domain.engine.OrchestratorTokenCounter
import io.foxbird.edumate.domain.engine.RagEngine
import io.foxbird.edumate.domain.engine.VectorSearchEngine
import io.foxbird.edumate.domain.service.ConceptExtractor
import io.foxbird.edumate.domain.service.ConversationManager
import io.foxbird.edumate.domain.service.EmbeddingService
import io.foxbird.edumate.domain.service.IEmbeddingService
import io.foxbird.edumate.domain.service.ImageInputAdapter
import io.foxbird.edumate.domain.service.KeywordExtractor
import io.foxbird.edumate.domain.service.MaterialProcessor
import io.foxbird.edumate.domain.service.PdfInputAdapter
import io.foxbird.edumate.domain.service.TaskQueue
import io.foxbird.edumate.domain.service.TextInputAdapter
import io.foxbird.edumate.domain.service.WorksheetService
import io.foxbird.edumate.feature.chat.ChatViewModel
import io.foxbird.edumate.feature.home.HomeViewModel
import io.foxbird.edumate.feature.knowledge.KnowledgeGraphViewModel
import io.foxbird.edumate.feature.materials.MaterialDetailViewModel
import io.foxbird.edumate.feature.materials.MaterialsViewModel
import io.foxbird.edumate.feature.onboarding.OnboardingViewModel
import io.foxbird.edumate.feature.settings.SettingsViewModel
import io.foxbird.edumate.feature.worksheet.WorksheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Single application-lifetime coroutine scope shared across all long-running operations.
// Named so it can be injected by qualifier without collision.
val appScopeModule = module {
    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), EduMateDatabase::class.java, "edumate.db").build()
    }
    single { get<EduMateDatabase>().materialDao() }
    single { get<EduMateDatabase>().chunkDao() }
    single { get<EduMateDatabase>().pageDao() }
    single { get<EduMateDatabase>().conceptDao() }
    single { get<EduMateDatabase>().conversationDao() }
    single { get<EduMateDatabase>().messageDao() }
}

val repositoryModule = module {
    single<MaterialRepository> { RoomMaterialRepository(get()) }
    single<ChunkRepository> { RoomChunkRepository(get()) }
    single<ConversationRepository> { RoomConversationRepository(get()) }
    single<MessageRepository> { RoomMessageRepository(get()) }
}

val preferencesModule = module {
    single { UserPreferencesManager(androidContext()) }
}

val engineModule = module {
    single { DeviceInfo(androidContext()) }
    single { LlamaCppEngine() }
    single { LiteRtGenerationEngine() }
    single { LiteRtEmbeddingEngine() }
    single { MemoryMonitor(androidContext()) }
    single { EngineOrchestrator(get(), get(), get(), get()) }
    single { ModelDownloader(androidContext()) }
    single { ModelManager(androidContext(), get(), get(), get(), AppModelConfigs.ALL) }
}

val domainModule = module {
    single { PdfInputAdapter(androidContext()) }
    single { TextInputAdapter() }
    single { ImageInputAdapter(androidContext()) }
    single { KeywordExtractor() }
    single { BM25Scorer() }

    // TokenCounter: use orchestrator-backed counter; falls back automatically when no model loaded
    single { OrchestratorTokenCounter(get<EngineOrchestrator>()) }
    single<IChunkingEngine> { ChunkingEngine(get<OrchestratorTokenCounter>()) }

    single<IEmbeddingService> { EmbeddingService(get<EngineOrchestrator>()) }
    single<IRagEngine> { RagEngine(get(), get(), get<IEmbeddingService>(), get(), get<IChunkingEngine>()) }

    single { VectorSearchEngine(get<ChunkRepository>()) }
    single {
        MaterialProcessor(
            get(), get(),
            get<IChunkingEngine>(),
            get<IEmbeddingService>(),
            get(),
            get<MaterialRepository>(),
            get<ChunkRepository>(),
            get()   // PageDao still direct — no repository wrapping planned for MVP
        )
    }
    single {
        ConversationManager(
            get<ConversationRepository>(),
            get<MessageRepository>(),
            get<EngineOrchestrator>()
        )
    }
    single { ConceptExtractor(get<EngineOrchestrator>(), get()) }
    single { WorksheetService(androidContext(), get<IRagEngine>(), get<EngineOrchestrator>()) }
    single { TaskQueue(get(named("appScope"))) }
}

val viewModelModule = module {
    viewModel {
        SettingsViewModel(
            get(), get(), get(),
            get<MaterialRepository>(),
            get<ChunkRepository>(),
            get()
        )
    }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { MaterialsViewModel(get<MaterialRepository>(), get(), get(named("appScope"))) }
    viewModel { (id: Long) ->
        MaterialDetailViewModel(id, get<MaterialRepository>(), get<ChunkRepository>(), get())
    }
    viewModel { ChatViewModel(get(), get<IRagEngine>()) }
    viewModel { KnowledgeGraphViewModel(get()) }
    viewModel { WorksheetViewModel(get(), get<MaterialRepository>()) }
    viewModel {
        HomeViewModel(
            get<MaterialRepository>(),
            get<ChunkRepository>(),
            get<ConversationRepository>(),
            get(),
            get()
        )
    }
}

val appModules = listOf(
    appScopeModule,
    databaseModule,
    repositoryModule,
    preferencesModule,
    engineModule,
    domainModule,
    viewModelModule
)
