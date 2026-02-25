package io.foxbird.edumate

import android.app.Application
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.MemoryMonitor
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named

class EduMateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.appTag = "EduMate"

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@EduMateApplication)
            modules(appModules)
        }

        // All long-running operations share the single Koin-provided application scope.
        val appScope: CoroutineScope by inject(named("appScope"))
        val memoryMonitor: MemoryMonitor by inject()
        val orchestrator: EngineOrchestrator by inject()
        val modelManager: ModelManager by inject()

        memoryMonitor.setOnCritical { orchestrator.handleMemoryPressure() }
        memoryMonitor.startMonitoring()

        appScope.launch {
            modelManager.initialize()
            modelManager.loadModel(AppModelConfigs.GEMMA_3N_E2B_LITERT)
            modelManager.loadModel(AppModelConfigs.EMBEDDING_GEMMA_LITERT)
        }
    }
}
