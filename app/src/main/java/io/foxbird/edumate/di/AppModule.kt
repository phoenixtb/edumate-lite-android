package io.foxbird.edumate.di

import androidx.room.Room
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.LiteRtEngine
import io.foxbird.edgeai.engine.LlamaCppEngine
import io.foxbird.edgeai.engine.MemoryMonitor
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.ModelDownloader
import io.foxbird.edgeai.util.DeviceInfo
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.data.local.EduMateDatabase
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import io.foxbird.edumate.domain.engine.BM25Scorer
import io.foxbird.edumate.domain.engine.ChunkingEngine
import io.foxbird.edumate.domain.engine.RagEngine
import io.foxbird.edumate.domain.engine.VectorSearchEngine
import io.foxbird.edumate.domain.service.ConceptExtractor
import io.foxbird.edumate.domain.service.ConversationManager
import io.foxbird.edumate.domain.service.EmbeddingService
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            EduMateDatabase::class.java,
            "edumate.db"
        ).build()
    }
    single { get<EduMateDatabase>().materialDao() }
    single { get<EduMateDatabase>().chunkDao() }
    single { get<EduMateDatabase>().pageDao() }
    single { get<EduMateDatabase>().conceptDao() }
    single { get<EduMateDatabase>().conversationDao() }
    single { get<EduMateDatabase>().messageDao() }
}

val preferencesModule = module {
    single { UserPreferencesManager(androidContext()) }
}

val engineModule = module {
    single { DeviceInfo(androidContext()) }
    single { LlamaCppEngine() }
    single { LiteRtEngine() }
    single { MemoryMonitor(androidContext()) }
    single { EngineOrchestrator(get(), get(), get()) }
    single { ModelDownloader(androidContext()) }
    single { ModelManager(androidContext(), get(), get(), get(), AppModelConfigs.ALL) }
}

val domainModule = module {
    single { PdfInputAdapter(androidContext()) }
    single { TextInputAdapter() }
    single { ImageInputAdapter(androidContext()) }
    single { ChunkingEngine(get<EngineOrchestrator>()) }
    single { EmbeddingService(get<EngineOrchestrator>()) }
    single { KeywordExtractor() }
    single { MaterialProcessor(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { VectorSearchEngine(get()) }
    single { BM25Scorer() }
    single { RagEngine(get(), get(), get(), get<EngineOrchestrator>(), get()) }
    single { ConversationManager(get(), get(), get<EngineOrchestrator>()) }
    single { ConceptExtractor(get<EngineOrchestrator>(), get()) }
    single { WorksheetService(androidContext(), get(), get<EngineOrchestrator>()) }
    single { TaskQueue() }
}

val viewModelModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { MaterialsViewModel(get(), get()) }
    viewModel { (id: Long) -> MaterialDetailViewModel(id, get(), get(), get()) }
    viewModel { ChatViewModel(get(), get()) }
    viewModel { KnowledgeGraphViewModel(get()) }
    viewModel { WorksheetViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
}

val appModules = listOf(
    databaseModule,
    preferencesModule,
    engineModule,
    domainModule,
    viewModelModule
)
