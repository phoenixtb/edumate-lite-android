package io.foxbird.edumate.di

import androidx.room.Room
import io.foxbird.doclibrary.data.local.DocumentDatabase
import io.foxbird.doclibrary.di.documentLibraryKoinModules
import io.foxbird.doclibrary.domain.rag.IRagEngine
import io.foxbird.doclibrary.domain.rag.RagConfig
import io.foxbird.agentcore.EduMateAgentOrchestrator
import io.foxbird.edgeai.agent.IAgentOrchestrator
import io.foxbird.edgeai.embedding.EmbeddingService
import io.foxbird.edgeai.embedding.IEmbeddingService
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
import io.foxbird.edumate.data.repository.ConversationRepository
import io.foxbird.edumate.data.repository.MessageRepository
import io.foxbird.edumate.data.repository.RoomConversationRepository
import io.foxbird.edumate.data.repository.RoomMessageRepository
import io.foxbird.edumate.domain.service.ConversationManager
import io.foxbird.edumate.domain.service.WorksheetService
import io.foxbird.edumate.feature.chat.ChatViewModel
import io.foxbird.edumate.feature.home.HomeViewModel
import io.foxbird.edumate.feature.knowledge.KnowledgeGraphViewModel
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

val appScopeModule = module {
    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}

val chatDatabaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), EduMateDatabase::class.java, "edumate_chat.db").build()
    }
    single { get<EduMateDatabase>().conversationDao() }
    single { get<EduMateDatabase>().messageDao() }
}

val repositoryModule = module {
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
    // IEmbeddingService implementation — backed by EngineOrchestrator
    single<IEmbeddingService> { EmbeddingService(get<EngineOrchestrator>()) }
}

/** EduMate-specific RAG config — "You are EduMate" persona. */
val ragConfigModule = module {
    single {
        RagConfig(
            systemPrompt = "You are EduMate, a friendly and knowledgeable educational tutor " +
                "for students in grades 5 through 10. Answer questions using ONLY the " +
                "provided study material. If the answer isn't in the material, say so " +
                "honestly. Keep explanations clear and age-appropriate. Use examples when helpful."
        )
    }
}

val domainModule = module {
    single {
        ConversationManager(
            get<ConversationRepository>(),
            get<MessageRepository>(),
            get<EngineOrchestrator>()
        )
    }
    single { WorksheetService(androidContext(), get<IRagEngine>(), get<EngineOrchestrator>()) }
    single<IAgentOrchestrator> {
        EduMateAgentOrchestrator(
            orchestrator = get<EngineOrchestrator>(),
            ragEngine = get<IRagEngine>(),
            memoryMonitor = get<MemoryMonitor>()
        )
    }
}

val viewModelModule = module {
    viewModel {
        SettingsViewModel(
            prefsManager = get(),
            modelManager = get(),
            memoryMonitor = get(),
            documentRepository = get<io.foxbird.doclibrary.data.repository.DocumentRepository>(),
            chunkRepository = get<io.foxbird.doclibrary.data.repository.ChunkRepository>(),
            chatDatabase = get<EduMateDatabase>(),
            documentDatabase = get<DocumentDatabase>()
        )
    }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { ChatViewModel(get(), get<IRagEngine>(), get<IAgentOrchestrator>()) }
    viewModel { KnowledgeGraphViewModel(get()) }
    viewModel { WorksheetViewModel(get(), get<io.foxbird.doclibrary.data.repository.DocumentRepository>()) }
    viewModel {
        HomeViewModel(
            get<io.foxbird.doclibrary.data.repository.DocumentRepository>(),
            get<io.foxbird.doclibrary.data.repository.ChunkRepository>(),
            get<ConversationRepository>(),
            get(),
            get(),
            get<io.foxbird.doclibrary.domain.processor.IDocumentProcessor>()
        )
    }
}

val appModules = listOf(
    appScopeModule,
    *documentLibraryKoinModules().toTypedArray(),
    chatDatabaseModule,
    repositoryModule,
    preferencesModule,
    engineModule,
    ragConfigModule,
    domainModule,
    viewModelModule
)
